#!/usr/bin/env python3
"""Point d'entrée : fetch RSS → résumés → fichier Markdown."""

from __future__ import annotations

import argparse
import time
from datetime import datetime
from pathlib import Path

from dotenv import load_dotenv

from src.config_loader import load_config
from src.fetcher import fetch_articles
from src.generator import render_briefing
from src.summarizer import build_summarizer

ROOT = Path(__file__).resolve().parent
BRIEFINGS_DIR = ROOT / "briefings"


def generate(output: Path | None = None, skip_ai: bool = False) -> Path:
    load_dotenv(ROOT / ".env")
    config = load_config()
    articles = fetch_articles(config)

    print(f"📥 {len(articles)} article(s) récupéré(s)")

    summarizer = build_summarizer() if not skip_ai else None
    summaries: dict[str, str] = {}

    for i, article in enumerate(articles, 1):
        print(f"  [{i}/{len(articles)}] {article.title[:60]}…")
        if summarizer:
            try:
                summaries[article.link] = summarizer.summarize(article)
                time.sleep(1)  # limite de débit API Gemini
            except Exception as exc:
                print(f"    ⚠ Résumé IA échoué: {exc}")
                summaries[article.link] = article.excerpt or "_Résumé indisponible._"
        else:
            summaries[article.link] = article.excerpt or "_Pas d'extrait._"

    md = render_briefing(config, articles, summaries)

    BRIEFINGS_DIR.mkdir(exist_ok=True)
    today = datetime.now().strftime("%Y-%m-%d")
    dated_path = BRIEFINGS_DIR / f"{today}.md"
    latest_path = BRIEFINGS_DIR / "latest.md"

    dated_path.write_text(md, encoding="utf-8")
    latest_path.write_text(md, encoding="utf-8")

    if output:
        output.write_text(md, encoding="utf-8")

    print(f"✅ Briefing écrit : {dated_path}")
    print(f"✅ Copie latest   : {latest_path}")
    return latest_path


def main() -> None:
    parser = argparse.ArgumentParser(description="Génère le briefing d'actualité")
    parser.add_argument("-o", "--output", type=Path, help="Fichier de sortie additionnel")
    parser.add_argument(
        "--no-ai",
        action="store_true",
        help="Pas de résumé IA (extraits RSS uniquement)",
    )
    args = parser.parse_args()
    generate(output=args.output, skip_ai=args.no_ai)


if __name__ == "__main__":
    main()
