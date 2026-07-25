package fr.actus.sync.data

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object WikiBuilder {
    private val dateTimeFmt = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneOffset.UTC)
    private val fullDateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm").withZone(ZoneOffset.UTC)

    fun buildWikiFiles(briefing: BriefingResult): Map<String, String> {
        val files = linkedMapOf("Home.md" to renderHomePortal(briefing))
        briefing.byRubrique.forEach { (rubrique, items) ->
            if (items.isEmpty()) return@forEach
            files[pageFileName(rubrique)] = renderRubriquePage(rubrique, items)
        }
        files["Briefing-${briefing.dateStr}.md"] = renderFullBriefing(briefing)
        return files
    }

    fun rubriqueSlug(name: String): String =
        name.lowercase()
            .replace(Regex("[^\\w\\s-]"), "")
            .replace(Regex("[-\\s]+"), "-")
            .trim('-')
            .ifBlank { "autres" }

    private fun pageFileName(rubrique: String): String = when (rubrique) {
        "Idées / Débats" -> "Idees-Debats.md"
        "Faits divers" -> "Faits-divers.md"
        else -> rubriqueSlug(rubrique)
            .split('-')
            .joinToString("-") { part -> part.replaceFirstChar { c -> c.titlecase() } } + ".md"
    }

    private fun formatDate(instant: Instant?): String =
        if (instant == null) "" else dateTimeFmt.format(instant)

    private fun articleBlock(processed: ProcessedArticle): String {
        val article = processed.article
        val lines = buildList {
            add("### ${article.title}")
            add("")
            add("**${article.feedName}** · ${formatDate(article.published)} · [Lire](${article.link})")
            add("")
            if (article.imageUrl.isNotBlank()) {
                add("![${article.title}](${article.imageUrl})")
                add("")
            }
            add(processed.summary)
            add("")
            add("---")
            add("")
        }
        return lines.joinToString("\n")
    }

    private fun renderHomePortal(briefing: BriefingResult): String {
        val now = Instant.now()
        val total = briefing.allArticles.size
        val rubriques = briefing.byRubrique.keys.sortedByDescending { briefing.byRubrique[it]?.size ?: 0 }

        val lines = buildList {
            add("# 📰 Actus — ${briefing.dateStr}")
            add("")
            add(
                "_Briefing personnel · $total articles · mis à jour le " +
                    "${fullDateFmt.format(now)} (UTC)_",
            )
            add("")
            add("> Page d'accueil : choisissez une rubrique ou parcourez les titres ci-dessous.")
            add("")
            add("## 🔥 À la une")
            add("")
            briefing.allArticles.take(6).forEach { processed ->
                val article = processed.article
                add(
                    "- **[[${rubriqueSlug(processed.rubrique)}|${processed.rubrique}]]** — " +
                        "${article.title} — [lire](${article.link})",
                )
            }
            add("")
            add("## 📂 Toutes les rubriques")
            add("")
            add("| Rubrique | Articles |")
            add("|----------|----------|")
            rubriques.forEach { rub ->
                val count = briefing.byRubrique[rub]?.size ?: 0
                add("| [[${rubriqueSlug(rub)}|$rub]] | $count |")
            }
            add("")
            add("## 📋 Derniers articles")
            add("")
            briefing.allArticles.take(15).forEach { processed ->
                val article = processed.article
                add("- **${article.title}** _(${processed.rubrique})_ — [article](${article.link})")
            }
            add("")
            add("---")
            add("")
            add("Archive du jour : [[Briefing-${briefing.dateStr}]]")
            add("")
            add("_Généré par Actus Sync (Android) · Le Monde · Charente Libre · Gemini_")
        }
        return lines.joinToString("\n") + "\n"
    }

    private fun renderRubriquePage(rubrique: String, items: List<ProcessedArticle>): String {
        val lines = buildList {
            add("# $rubrique")
            add("")
            add("_${items.size} article(s) — retour [[Home]]_")
            add("")
            items.forEach { add(articleBlock(it)) }
        }
        return lines.joinToString("\n").trimEnd() + "\n"
    }

    private fun renderFullBriefing(briefing: BriefingResult): String {
        val lines = buildList {
            add("# Briefing complet — ${briefing.dateStr}")
            add("")
            add("_${briefing.allArticles.size} articles classés automatiquement_")
            add("")
            briefing.byRubrique.keys.sorted().forEach { rubrique ->
                add("## $rubrique")
                add("")
                briefing.byRubrique[rubrique].orEmpty().forEach { add(articleBlock(it)) }
            }
        }
        return lines.joinToString("\n").trimEnd() + "\n"
    }
}
