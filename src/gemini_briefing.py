"""Classification + résumé Gemini par article, chapitrage dynamique."""

from __future__ import annotations

import json
import re
import time
from dataclasses import dataclass, field

import requests

from .fetcher import Article


@dataclass
class ProcessedArticle:
    article: Article
    rubrique: str
    summary: str


@dataclass
class BriefingResult:
    date_str: str
    by_rubrique: dict[str, list[ProcessedArticle]] = field(default_factory=dict)
    all_articles: list[ProcessedArticle] = field(default_factory=list)


def _call_gemini_json(api_key: str, prompt: str, model: str = "gemini-2.0-flash") -> dict:
    url = (
        f"https://generativelanguage.googleapis.com/v1beta/models/"
        f"{model}:generateContent?key={api_key}"
    )
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": 0.2,
            "maxOutputTokens": 2048,
            "responseMimeType": "application/json",
        },
    }
    resp = requests.post(url, json=payload, timeout=90)
    resp.raise_for_status()
    data = resp.json()
    text = data["candidates"][0]["content"]["parts"][0]["text"]
    return json.loads(text)


def process_article(
    article: Article,
    api_key: str,
    rubriques: list[str],
) -> ProcessedArticle:
    source = article.full_text or article.excerpt or article.title
    rub_list = ", ".join(rubriques)

    prompt = f"""Tu es un journaliste. Analyse cet article et réponds en JSON strict.

Rubriques possibles (choisir UNE seule) : {rub_list}

Champs JSON requis :
- "rubrique" : la rubrique choisie (exactement comme dans la liste)
- "resume" : résumé complet en français, 8 à 12 phrases factuelles, style presse

Article :
Titre : {article.title}
Source : {article.feed_name}
URL : {article.link}
Texte :
{source[:10000]}
"""
    try:
        data = _call_gemini_json(api_key, prompt)
        rubrique = data.get("rubrique", "Autres")
        if rubrique not in rubriques:
            rubrique = "Autres"
        summary = data.get("resume", article.excerpt or article.title)
        return ProcessedArticle(article=article, rubrique=rubrique, summary=summary)
    except Exception as exc:
        print(f"    ⚠ Gemini article échoué: {exc}")
        return ProcessedArticle(
            article=article,
            rubrique="Autres",
            summary=article.excerpt or article.title,
        )


def build_briefing(
    articles: list[Article],
    api_key: str,
    rubriques: list[str],
    date_str: str,
) -> BriefingResult:
    result = BriefingResult(date_str=date_str)
    total = len(articles)

    for i, article in enumerate(articles, 1):
        print(f"  🤖 [{i}/{total}] {article.title[:55]}…")
        processed = process_article(article, api_key, rubriques)
        result.all_articles.append(processed)
        result.by_rubrique.setdefault(processed.rubrique, []).append(processed)
        time.sleep(1.2)

    return result


def rubrique_slug(name: str) -> str:
    slug = re.sub(r"[^\w\s-]", "", name.lower())
    return re.sub(r"[-\s]+", "-", slug).strip("-") or "autres"
