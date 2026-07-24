"""Génération du fichier Markdown de briefing."""

from __future__ import annotations

from collections import defaultdict
from datetime import datetime, timezone

from .config_loader import AppConfig
from .fetcher import Article


def _format_date(dt: datetime | None) -> str:
    if not dt:
        return "date inconnue"
    return dt.astimezone().strftime("%d/%m/%Y %H:%M")


def render_briefing(
    config: AppConfig,
    articles: list[Article],
    summaries: dict[str, str],
) -> str:
    by_theme: dict[str, list[Article]] = defaultdict(list)

    for article in articles:
        for theme_id in article.themes:
            by_theme[theme_id].append(article)

    now = datetime.now(timezone.utc)
    theme_ids = [t.id for t in config.themes]
    theme_labels = {t.id: f"{t.icon} {t.label}".strip() for t in config.themes}

    lines: list[str] = [
        "---",
        f"date: {now.strftime('%Y-%m-%d')}",
        f"generated_at: {now.isoformat()}",
        f"themes: [{', '.join(theme_ids)}]",
        "---",
        "",
        f"# Briefing du {now.strftime('%d %B %Y')}",
        "",
        "_Résumés d'actualité par thème — généré automatiquement._",
        "",
    ]

    for theme in config.themes:
        theme_articles = by_theme.get(theme.id, [])
        if not theme_articles:
            continue

        lines.append(f"## {theme_labels[theme.id]}")
        lines.append("")

        seen_links: set[str] = set()
        for article in theme_articles:
            if article.link in seen_links:
                continue
            seen_links.add(article.link)

            summary = summaries.get(article.link, article.excerpt or "")
            lines.append(f"### {article.title}")
            lines.append("")
            lines.append(
                f"**Source :** {article.feed_name} · "
                f"**Publié :** {_format_date(article.published)} · "
                f"[Lire l'article]({article.link})"
            )
            lines.append("")
            lines.append(summary)
            lines.append("")
            lines.append("---")
            lines.append("")

    if len(lines) <= 8:
        lines.append("_Aucun article récent pour les thèmes configurés._")

    return "\n".join(lines).rstrip() + "\n"
