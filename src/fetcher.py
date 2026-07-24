"""Récupération des articles depuis les flux RSS."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from html import unescape
import re

import feedparser

from .config_loader import AppConfig, Feed


@dataclass
class Article:
    feed_id: str
    feed_name: str
    themes: list[str]
    title: str
    link: str
    published: datetime | None
    excerpt: str


def _strip_html(text: str) -> str:
    text = unescape(text or "")
    text = re.sub(r"<[^>]+>", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def _parse_date(entry) -> datetime | None:
    if hasattr(entry, "published_parsed") and entry.published_parsed:
        return datetime(*entry.published_parsed[:6], tzinfo=timezone.utc)
    if hasattr(entry, "updated_parsed") and entry.updated_parsed:
        return datetime(*entry.updated_parsed[:6], tzinfo=timezone.utc)
    return None


def fetch_articles(config: AppConfig) -> list[Article]:
    cutoff = datetime.now(timezone.utc) - timedelta(hours=config.max_age_hours)
    articles: list[Article] = []

    for feed in config.feeds:
        parsed = feedparser.parse(
            feed.url,
            agent="ActuMaison/1.0 (+https://github.com/actu-maison)",
        )
        if parsed.bozo and not parsed.entries:
            print(f"⚠ Flux ignoré ({feed.name}): {parsed.bozo_exception}")
            continue

        count = 0
        for entry in parsed.entries:
            if count >= config.max_articles_per_feed:
                break

            published = _parse_date(entry)
            if published and published < cutoff:
                continue

            summary = _strip_html(
                entry.get("summary") or entry.get("description") or ""
            )
            title = _strip_html(entry.get("title", "Sans titre"))
            link = entry.get("link", "")

            if not link:
                continue

            articles.append(
                Article(
                    feed_id=feed.id,
                    feed_name=feed.name,
                    themes=list(feed.themes),
                    title=title,
                    link=link,
                    published=published,
                    excerpt=summary[:800] if summary else "",
                )
            )
            count += 1

    articles.sort(
        key=lambda a: a.published or datetime.min.replace(tzinfo=timezone.utc),
        reverse=True,
    )
    return articles
