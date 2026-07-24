#!/usr/bin/env python3
"""
Découpe briefings/latest.md en pages GitHub Wiki (une page par thème + Home).

Usage :
  python scripts/sync_github_wiki.py
  python scripts/sync_github_wiki.py --wiki-dir /tmp/actu.wiki
"""

from __future__ import annotations

import argparse
import re
import shutil
import subprocess
import unicodedata
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BRIEFING = ROOT / "briefings" / "latest.md"
DEFAULT_WIKI_DIR = ROOT / ".wiki"


def slugify(text: str) -> str:
    text = unicodedata.normalize("NFKD", text)
    text = text.encode("ascii", "ignore").decode("ascii")
    text = re.sub(r"[^\w\s-]", "", text.lower())
    return re.sub(r"[-\s]+", "-", text).strip("-") or "theme"


def parse_briefing(text: str) -> tuple[dict[str, str], str, str]:
    """Retourne (meta, titre principal, sections par slug)."""
    meta: dict[str, str] = {}
    body = text

    if text.startswith("---"):
        match = re.match(r"^---\n([\s\S]*?)\n---\n([\s\S]*)$", text)
        if match:
            for line in match.group(1).splitlines():
                if ":" in line:
                    k, v = line.split(":", 1)
                    meta[k.strip()] = v.strip()
            body = match.group(2)

    title_match = re.search(r"^# (.+)$", body, re.MULTILINE)
    main_title = title_match.group(1) if title_match else "Briefing"

    sections: dict[str, str] = {}
    parts = re.split(r"^## ", body, flags=re.MULTILINE)
    for part in parts[1:]:
        nl = part.find("\n")
        heading = part[:nl].strip() if nl > -1 else part.strip()
        content = part[nl + 1 :].strip() if nl > -1 else ""
        key = slugify(heading)
        sections[key] = f"# {heading}\n\n{content}\n"

    return meta, main_title, sections


def write_home(meta: dict[str, str], main_title: str, sections: dict[str, str]) -> str:
    date = meta.get("date", datetime.now(timezone.utc).strftime("%Y-%m-%d"))
    lines = [
        f"# {main_title}",
        "",
        f"_Dernière mise à jour : {date}_",
        "",
        "## Thèmes",
        "",
    ]
    for slug, content in sections.items():
        title = content.splitlines()[0].lstrip("# ").strip()
        wiki_name = title.replace(" ", "-")
        lines.append(f"- [[{wiki_name}|{title}]]")
    lines.extend(
        [
            "",
            "---",
            "",
            "Briefing généré automatiquement depuis les flux RSS configurés.",
            "",
            "[[Politique]] · [[Local]] · [[Sport]] · [[Culture]]",
        ]
    )
    return "\n".join(lines) + "\n"


def sync_wiki(wiki_dir: Path, briefing_path: Path = BRIEFING) -> list[Path]:
    if not briefing_path.exists():
        raise FileNotFoundError(f"Briefing introuvable : {briefing_path}")

    text = briefing_path.read_text(encoding="utf-8")
    meta, main_title, sections = parse_briefing(text)

    wiki_dir.mkdir(parents=True, exist_ok=True)
    for old in wiki_dir.glob("*.md"):
        old.unlink()

    written: list[Path] = []

    home = wiki_dir / "Home.md"
    home.write_text(write_home(meta, main_title, sections), encoding="utf-8")
    written.append(home)

    for slug, content in sections.items():
        title = content.splitlines()[0].lstrip("# ").strip()
        filename = title.replace(" ", "-") + ".md"
        path = wiki_dir / filename
        path.write_text(content, encoding="utf-8")
        written.append(path)

    archive = wiki_dir / f"Briefing-{meta.get('date', 'archive')}.md"
    archive.write_text(text, encoding="utf-8")
    written.append(archive)

    return written


def git_push_wiki(wiki_dir: Path, repo: str, token: str) -> None:
    wiki_url = f"https://x-access-token:{token}@github.com/{repo}.wiki.git"

    if not (wiki_dir / ".git").exists():
        if wiki_dir.exists() and any(wiki_dir.iterdir()):
            for item in wiki_dir.iterdir():
                if item.name != ".git":
                    item.unlink() if item.is_file() else shutil.rmtree(item)
        subprocess.run(["git", "clone", wiki_url, str(wiki_dir)], check=True)
    else:
        subprocess.run(["git", "-C", str(wiki_dir), "pull", "--rebase"], check=True)

    sync_wiki(wiki_dir)

    subprocess.run(["git", "-C", str(wiki_dir), "add", "-A"], check=True)
    status = subprocess.run(
        ["git", "-C", str(wiki_dir), "status", "--porcelain"],
        capture_output=True,
        text=True,
        check=True,
    )
    if not status.stdout.strip():
        print("ℹ Wiki déjà à jour")
        return

    msg = f"briefing: {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M')} UTC"
    subprocess.run(["git", "-C", str(wiki_dir), "commit", "-m", msg], check=True)
    subprocess.run(["git", "-C", str(wiki_dir), "push"], check=True)
    print(f"✅ Wiki poussé : https://github.com/{repo}/wiki")


def main() -> None:
    parser = argparse.ArgumentParser(description="Sync briefing → GitHub Wiki")
    parser.add_argument("--wiki-dir", type=Path, default=DEFAULT_WIKI_DIR)
    parser.add_argument("--push", action="store_true", help="Git push vers GitHub Wiki")
    parser.add_argument("--repo", help="owner/actu (requis avec --push)")
    parser.add_argument("--token", help="GitHub token (ou env GITHUB_TOKEN)")
    args = parser.parse_args()

    written = sync_wiki(args.wiki_dir)
    print(f"✅ {len(written)} page(s) wiki écrite(s) dans {args.wiki_dir}")
    for p in written:
        print(f"   • {p.name}")

    if args.push:
        import os

        token = args.token or os.environ.get("GITHUB_TOKEN", "")
        repo = args.repo or os.environ.get("GITHUB_REPOSITORY", "")
        if not token or not repo:
            raise SystemExit("--push requiert --repo et --token (ou GITHUB_TOKEN + GITHUB_REPOSITORY)")
        git_push_wiki(args.wiki_dir, repo, token)


if __name__ == "__main__":
    main()
