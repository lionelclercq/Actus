#!/usr/bin/env python3
"""
Actus Mobile — pipeline complet pour smartphone (Termux).

1. Charge identifiants locaux (~/.actus/credentials.yaml)
2. Récupère TOUS les flux (Le Monde, Charente Libre, …)
3. Enrichit avec cookies / session abonné
4. Gemini : classe + résume chaque article (toutes rubriques)
5. Génère wiki portail + pages par chapitre
6. Pousse vers GitHub Wiki

Usage Termux :
  python mobile.py
  python mobile.py --no-push    # test local sans push wiki
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
from datetime import datetime, timezone
from pathlib import Path

from src.credentials import CREDENTIALS_PATH, load_credentials
from src.enricher import enrich_articles
from src.fetch_all import fetch_all_sources
from src.gemini_briefing import build_briefing
from src.sources_config import load_sources
from src.wiki_builder import build_wiki_files

ROOT = Path(__file__).resolve().parent
WIKI_EXPORT = ROOT / "wiki"
BRIEFINGS = ROOT / "briefings"


def push_wiki(files: dict[str, str], repo: str, token: str) -> None:
    wiki_dir = ROOT / ".wiki-push"
    wiki_url = f"https://x-access-token:{token}@github.com/{repo}.wiki.git"

    if wiki_dir.exists():
        shutil.rmtree(wiki_dir)

    result = subprocess.run(["git", "clone", wiki_url, str(wiki_dir)], capture_output=True, text=True)
    if result.returncode != 0:
        raise SystemExit(
            f"Impossible de cloner le wiki.\n"
            f"Créez une page Home sur https://github.com/{repo}/wiki/_new\n"
            f"Erreur: {result.stderr}"
        )

    for name, content in files.items():
        (wiki_dir / name).write_text(content, encoding="utf-8")

    subprocess.run(["git", "-C", str(wiki_dir), "add", "-A"], check=True)
    subprocess.run(
        ["git", "-C", str(wiki_dir), "config", "user.email", "actus-mobile@local"],
        check=True,
    )
    subprocess.run(["git", "-C", str(wiki_dir), "config", "user.name", "Actus Mobile"], check=True)
    subprocess.run(
        ["git", "-C", str(wiki_dir), "commit", "-m", f"briefing mobile {datetime.now().isoformat()}"],
        check=True,
    )
    subprocess.run(["git", "-C", str(wiki_dir), "push"], check=True)
    shutil.rmtree(wiki_dir, ignore_errors=True)
    print(f"✅ Wiki en ligne : https://github.com/{repo}/wiki")


def run(push: bool = True) -> None:
    creds = load_credentials()
    if not creds.gemini_api_key:
        raise SystemExit(
            f"Clé Gemini manquante.\n"
            f"Éditez {CREDENTIALS_PATH} (voir config/credentials.example.yaml)"
        )

    if not CREDENTIALS_PATH.exists():
        print(f"⚠ Créez {CREDENTIALS_PATH} à partir de config/credentials.example.yaml")

    config = load_sources()
    print("📡 Récupération de tous les flux…")
    articles = fetch_all_sources(config)
    print(f"   {len(articles)} article(s) unique(s)")

    print("🔐 Enrichissement (cookies ~/.actus/cookies/)…")
    articles = enrich_articles(articles)
    enriched = sum(1 for a in articles if a.full_text)
    print(f"   {enriched} avec texte intégral")

    date_str = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    print("🤖 Analyse Gemini (classification + résumés)…")
    briefing = build_briefing(articles, creds.gemini_api_key, config.rubriques, date_str)

    print("📝 Génération du wiki…")
    wiki_files = build_wiki_files(briefing)

    WIKI_EXPORT.mkdir(exist_ok=True)
    BRIEFINGS.mkdir(exist_ok=True)
    for name, content in wiki_files.items():
        (WIKI_EXPORT / name).write_text(content, encoding="utf-8")
    full = wiki_files.get(f"Briefing-{date_str}.md", "")
    if full:
        (BRIEFINGS / "latest.md").write_text(full, encoding="utf-8")
        (BRIEFINGS / f"{date_str}.md").write_text(full, encoding="utf-8")

    print(f"✅ {len(wiki_files)} page(s) wiki générée(s)")
    for rub, items in sorted(briefing.by_rubrique.items()):
        print(f"   • {rub}: {len(items)} article(s)")

    if push:
        token = creds.github_token
        if not token:
            raise SystemExit("Token GitHub manquant dans credentials.yaml")
        print("🚀 Push vers GitHub Wiki…")
        push_wiki(wiki_files, creds.github_repo, token)
    else:
        print("ℹ Mode local — fichiers dans wiki/ et briefings/")


def main() -> None:
    parser = argparse.ArgumentParser(description="Actus Mobile — briefing complet")
    parser.add_argument("--no-push", action="store_true", help="Ne pas pousser sur GitHub Wiki")
    args = parser.parse_args()
    run(push=not args.no_push)


if __name__ == "__main__":
    main()
