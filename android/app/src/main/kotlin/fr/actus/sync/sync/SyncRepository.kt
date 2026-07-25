package fr.actus.sync.sync

import fr.actus.sync.data.ArticleEnricher
import fr.actus.sync.data.BriefingResult
import fr.actus.sync.data.CookieRepository
import fr.actus.sync.data.CookieRepositoryHolder
import fr.actus.sync.data.FeedConfig
import fr.actus.sync.data.GeminiClient
import fr.actus.sync.data.RssFetcher
import fr.actus.sync.data.SettingsRepository
import fr.actus.sync.data.WikiBuilder
import fr.actus.sync.data.WikiPusher
import java.time.LocalDate
import java.time.ZoneOffset

class SyncRepository(
    private val settings: SettingsRepository,
    private val rssFetcher: RssFetcher = RssFetcher(),
) {
    data class SyncProgress(
        val phase: Phase,
        val message: String,
        val current: Int = 0,
        val total: Int = 0,
    ) {
        enum class Phase { FETCH, ENRICH, ANALYZE, WIKI, PUSH, DONE, ERROR }
    }

    suspend fun run(onProgress: suspend (SyncProgress) -> Unit) {
        if (!settings.isConfigured()) {
            throw IllegalStateException("Configurez la clé Gemini et le token GitHub.")
        }

        onProgress(SyncProgress(SyncProgress.Phase.FETCH, "Récupération des flux RSS…"))
        val articles = rssFetcher.fetchAll()
        if (articles.isEmpty()) {
            throw IllegalStateException("Aucun article récupéré depuis les flux RSS.")
        }
        onProgress(
            SyncProgress(
                SyncProgress.Phase.FETCH,
                "${articles.size} article(s) récupéré(s)",
                articles.size,
                articles.size,
            ),
        )

        onProgress(SyncProgress(SyncProgress.Phase.ENRICH, "Connexion abonné — texte intégral…"))
        val loggedSites = CookieRepository.PublisherSite.entries
            .filter { CookieRepositoryHolder.repository.isLoggedIn(it) }
            .map { it.label }
        val enrichedArticles = if (loggedSites.isEmpty()) {
            onProgress(
                SyncProgress(
                    SyncProgress.Phase.ENRICH,
                    "Aucun abonnement connecté — extraits RSS uniquement",
                ),
            )
            articles
        } else {
            val enricher = ArticleEnricher()
            val result = enricher.enrich(articles)
            val count = result.count { it.fullText.isNotBlank() }
            onProgress(
                SyncProgress(
                    SyncProgress.Phase.ENRICH,
                    "$count article(s) enrichi(s) via ${loggedSites.joinToString(", ")}",
                    count,
                    articles.size,
                ),
            )
            result
        }

        val dateStr = LocalDate.now(ZoneOffset.UTC).toString()
        val gemini = GeminiClient(settings.geminiApiKey)
        onProgress(SyncProgress(SyncProgress.Phase.ANALYZE, "Analyse Gemini…"))
        val briefing: BriefingResult = gemini.buildBriefing(
            articles = enrichedArticles,
            rubriques = FeedConfig.rubriques,
            dateStr = dateStr,
        ) { current, total, title ->
            onProgress(
                SyncProgress(
                    SyncProgress.Phase.ANALYZE,
                    "[$current/$total] $title",
                    current,
                    total,
                ),
            )
        }

        onProgress(SyncProgress(SyncProgress.Phase.WIKI, "Génération des pages wiki…"))
        val wikiFiles = WikiBuilder.buildWikiFiles(briefing)

        onProgress(SyncProgress(SyncProgress.Phase.PUSH, "Publication sur GitHub Wiki…"))
        WikiPusher(settings.githubRepo, settings.githubToken).pushAll(wikiFiles)

        val summary = buildString {
            append("✅ ${wikiFiles.size} page(s) publiée(s)\n")
            briefing.byRubrique.entries
                .sortedByDescending { it.value.size }
                .forEach { (rub, items) -> append("• $rub: ${items.size}\n") }
        }.trim()
        settings.lastSyncMessage = summary
        onProgress(SyncProgress(SyncProgress.Phase.DONE, summary))
    }
}
