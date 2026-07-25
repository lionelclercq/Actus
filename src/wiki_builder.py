"""Génération du wiki type portail d'actualités."""

from __future__ import annotations

import re
from datetime import datetime, timezone

from .fetcher import Article
from .gemini_briefing import BriefingResult, ProcessedArticle, rubrique_slug


def _format_date(dt) -> str:
    if not dt:
        return ""
    return dt.strftime("%d/%m %H:%M")


def _article_block(p: ProcessedArticle) -> list[str]:
    a = p.article
    lines = [
        f"### {a.title}",
        "",
        f"**{a.feed_name}** · {_format_date(a.published)} · [Lire]({a.link})",
        "",
    ]
    if a.image_url:
        lines.append(f"![{a.title}]({a.image_url})")
        lines.append("")
    lines.append(p.summary)
    lines.append("")
    lines.append("---")
    lines.append("")
    return lines


def render_home_portal(briefing: BriefingResult) -> str:
    """Page d'accueil wiki — portail d'actualités."""
    now = datetime.now(timezone.utc)
    total = len(briefing.all_articles)
    rubriques = sorted(briefing.by_rubrique.keys(), key=lambda r: -len(briefing.by_rubrique[r]))

    lines = [
        f"# 📰 Actus — {briefing.date_str}",
        "",
        f"_Briefing personnel · {total} articles · mis à jour le "
        f"{now.strftime('%d/%m/%Y à %H:%M')} (UTC)_",
        "",
        "> Page d'accueil : choisissez une rubrique ou parcourez les titres ci-dessous.",
        "",
        "## 🔥 À la une",
        "",
    ]

    for p in briefing.all_articles[:6]:
        a = p.article
        lines.append(f"- **[[{rubrique_slug(p.rubrique)}|{p.rubrique}]]** — "
                     f"{a.title} — [lire]({a.link})")

    lines.extend(["", "## 📂 Toutes les rubriques", "", "| Rubrique | Articles |", "|----------|----------|"])
    for rub in rubriques:
        slug = rubrique_slug(rub)
        count = len(briefing.by_rubrique[rub])
        lines.append(f"| [[{slug}|{rub}]] | {count} |")

    lines.extend(["", "## 📋 Derniers articles", ""])
    for p in briefing.all_articles[:15]:
        a = p.article
        lines.append(f"- **{a.title}** _({p.rubrique})_ — [article]({a.link})")

    lines.extend([
        "",
        "---",
        "",
        f"Archive du jour : [[Briefing-{briefing.date_str}]]",
        "",
        "_Généré par Actus Sync (Android) · Le Monde · Charente Libre · Gemini_",
    ])
    return "\n".join(lines) + "\n"


def render_rubrique_page(rubrique: str, items: list[ProcessedArticle]) -> str:
    lines = [
        f"# {rubrique}",
        "",
        f"_{len(items)} article(s) — retour [[Home]]_",
        "",
    ]
    for p in items:
        lines.extend(_article_block(p))
    return "\n".join(lines).rstrip() + "\n"


def render_full_briefing(briefing: BriefingResult) -> str:
  lines = [
      f"# Briefing complet — {briefing.date_str}",
      "",
      f"_{len(briefing.all_articles)} articles classés automatiquement_",
      "",
  ]
  for rubrique in sorted(briefing.by_rubrique.keys()):
      lines.append(f"## {rubrique}")
      lines.append("")
      for p in briefing.by_rubrique[rubrique]:
          lines.extend(_article_block(p))
  return "\n".join(lines).rstrip() + "\n"


def build_wiki_files(briefing: BriefingResult) -> dict[str, str]:
    """Retourne {nom_fichier.md: contenu}."""
    files: dict[str, str] = {"Home.md": render_home_portal(briefing)}
    for rubrique, items in briefing.by_rubrique.items():
        if not items:
            continue
        slug = rubrique_slug(rubrique)
        # Nom de page wiki lisible
        page_name = slug.replace("-", " ").title().replace(" ", "-")
        if rubrique == "Idées / Débats":
            page_name = "Idees-Debats"
        elif rubrique == "Faits divers":
            page_name = "Faits-divers"
        files[f"{page_name}.md"] = render_rubrique_page(rubrique, items)

    files[f"Briefing-{briefing.date_str}.md"] = render_full_briefing(briefing)
    return files
