"""Chargement config sources.yaml (tous flux, sans thème fixe)."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import yaml

SOURCES_PATH = Path(__file__).resolve().parent.parent / "config" / "sources.yaml"


@dataclass
class SourceFeed:
    id: str
    name: str
    url: str


@dataclass
class SourcesConfig:
    feeds: list[SourceFeed]
    rubriques: list[str]
    max_articles_per_feed: int = 8
    max_age_hours: int = 48
    max_total_articles: int = 40


def load_sources(path: Path | None = None) -> SourcesConfig:
    path = path or SOURCES_PATH
    with path.open(encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    feeds = [SourceFeed(**f) for f in raw.get("feeds", [])]
    return SourcesConfig(
        feeds=feeds,
        rubriques=raw.get("rubriques", []),
        max_articles_per_feed=int(raw.get("max_articles_per_feed", 8)),
        max_age_hours=int(raw.get("max_age_hours", 48)),
        max_total_articles=int(raw.get("max_total_articles", 40)),
    )
