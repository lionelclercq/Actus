"""
Enrichissement local optionnel : texte intégral + images supplémentaires.

Utilise des cookies exportés depuis votre navigateur (session abonné).
NE PAS mettre de mot de passe dans GitHub — uniquement sur votre téléphone/PC.

Fichiers cookies (format Netscape, une par site) :
  ~/.actus/cookies/lemonde.fr.txt
  ~/.actus/cookies/charentelibre.fr.txt

Export : extension navigateur « Get cookies.txt LOCALLY » (Firefox/Chrome).
"""

from __future__ import annotations

import http.cookiejar
import re
from pathlib import Path
from urllib.parse import urlparse

import requests

from .fetcher import Article

COOKIES_DIR = Path.home() / ".actus" / "cookies"
USER_AGENT = (
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
)


def _domain_from_url(url: str) -> str:
    host = urlparse(url).netloc.lower()
    return host[4:] if host.startswith("www.") else host


def _load_cookies_for(url: str) -> requests.Session | None:
    domain = _domain_from_url(url)
    cookie_file = COOKIES_DIR / f"{domain}.txt"
    if not cookie_file.exists():
        return None

    session = requests.Session()
    session.headers["User-Agent"] = USER_AGENT
    jar = http.cookiejar.MozillaCookieJar(str(cookie_file))
    try:
        jar.load(ignore_discard=True, ignore_expires=True)
    except Exception as exc:
        print(f"    ⚠ Cookies invalides ({cookie_file.name}): {exc}")
        return None
    session.cookies = jar
    return session


def _extract_article_html(html: str, url: str) -> tuple[str, list[str]]:
    """Extraction simple du corps d'article et des images."""
    images: list[str] = []
    for match in re.finditer(r'<img[^>]+src=["\']([^"\']+)["\']', html, re.I):
        src = match.group(1)
        if src.startswith("http") and "pixel" not in src and "tracking" not in src:
            images.append(src)

    # Le Monde : zone article principale
    patterns = [
        r'<article[^>]*>([\s\S]*?)</article>',
        r'class="article__content"[^>]*>([\s\S]*?)</div>',
        r'class="content"[^>]*>([\s\S]*?)</div>',
        r'itemprop="articleBody"[^>]*>([\s\S]*?)</div>',
    ]
    body = ""
    for pat in patterns:
        m = re.search(pat, html, re.I)
        if m and len(m.group(1)) > len(body):
            body = m.group(1)

    if not body:
        body = html

    text = re.sub(r"<script[\s\S]*?</script>", " ", body, flags=re.I)
    text = re.sub(r"<style[\s\S]*?</style>", " ", text, flags=re.I)
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text[:15000], images[:5]


def enrich_article(article: Article) -> Article:
    """Tente de récupérer le texte intégral via cookies locaux."""
    session = _load_cookies_for(article.link)
    if not session:
        return article

    try:
        resp = session.get(article.link, timeout=30, allow_redirects=True)
        resp.raise_for_status()
        if "paywall" in resp.url.lower() or len(resp.text) < 500:
            return article

        full_text, extra_images = _extract_article_html(resp.text, article.link)
        if len(full_text) < 200:
            return article

        image_url = article.image_url
        if not image_url and extra_images:
            image_url = extra_images[0]

        return Article(
            feed_id=article.feed_id,
            feed_name=article.feed_name,
            themes=article.themes,
            title=article.title,
            link=article.link,
            published=article.published,
            excerpt=article.excerpt,
            image_url=image_url,
            full_text=full_text,
        )
    except Exception as exc:
        print(f"    ⚠ Enrichissement échoué ({article.link[:50]}…): {exc}")
        return article


def enrich_articles(articles: list[Article]) -> list[Article]:
    if not COOKIES_DIR.exists():
        return articles

    cookie_files = list(COOKIES_DIR.glob("*.txt"))
    if not cookie_files:
        return articles

    print(f"🔐 Enrichissement local ({len(cookie_files)} fichier(s) cookies)")
    return [enrich_article(a) for a in articles]
