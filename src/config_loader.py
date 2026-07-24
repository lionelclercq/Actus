"""Chargement de la configuration feeds.yaml."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

import yaml

CONFIG_PATH = Path(__file__).resolve().parent.parent / "config" / "feeds.yaml"


@dataclass
class Theme:
    id: str
    label: str
    icon: str = ""


@dataclass
class Feed:
    id: str
    name: str
    url: str
    themes: list[str]


@dataclass
class AppConfig:
    themes: list[Theme]
    feeds: list[Feed]
    max_articles_per_feed: int = 5
    max_age_hours: int = 48

    def theme_map(self) -> dict[str, Theme]:
        return {t.id: t for t in self.themes}


def load_config(path: Path | None = None) -> AppConfig:
    path = path or CONFIG_PATH
    with path.open(encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    themes = [Theme(**t) for t in raw.get("themes", [])]
    feeds = [Feed(**f) for f in raw.get("feeds", [])]
    return AppConfig(
        themes=themes,
        feeds=feeds,
        max_articles_per_feed=int(raw.get("max_articles_per_feed", 5)),
        max_age_hours=int(raw.get("max_age_hours", 48)),
    )
