"""Fetch tous les flux sources.yaml sans filtre thématique."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone

import feedparser

from .fetcher import Article, _extract_image, _parse_date, _strip_html
from .sources_config import SourcesConfig


def fetch_all_sources(config: SourcesConfig) -> list[Article]:
    cutoff = datetime.now(timezone.utc) - timedelta(hours=config.max_age_hours)
    seen_links: set[str] = set()
    articles: list[Article] = []

    for feed in config.feeds:
        parsed = feedparser.parse(
            feed.url,
            agent="ActuMobile/1.0 (+https://github.com/lionelclercq/Actus)",
        )
        if parsed.bozo and not parsed.entries:
            print(f"⚠ Flux ignoré ({feed.name}): {parsed.bozo_exception}")
            continue

        count = 0
        for entry in parsed.entries:
            if count >= config.max_articles_per_feed:
                break
            if len(articles) >= config.max_total_articles:
                break

            published = _parse_date(entry)
            if published and published < cutoff:
                continue

            link = entry.get("link", "")
            if not link or link in seen_links:
                continue
            seen_links.add(link)

            raw_summary = entry.get("summary") or entry.get("description") or ""
            articles.append(
                Article(
                    feed_id=feed.id,
                    feed_name=feed.name,
                    themes=[],  # classé par Gemini ensuite
                    title=_strip_html(entry.get("title", "Sans titre")),
                    link=link,
                    published=published,
                    excerpt=_strip_html(raw_summary)[:2000],
                    image_url=_extract_image(entry),
                )
            )
            count += 1

    articles.sort(
        key=lambda a: a.published or datetime.min.replace(tzinfo=timezone.utc),
        reverse=True,
    )
    return articles[: config.max_total_articles]
