package fr.actus.sync.data

import java.time.Instant

data class Feed(
    val id: String,
    val name: String,
    val url: String,
)

data class Article(
    val feedId: String,
    val feedName: String,
    val title: String,
    val link: String,
    val published: Instant?,
    val excerpt: String,
    val imageUrl: String = "",
    val fullText: String = "",
)

data class ProcessedArticle(
    val article: Article,
    val rubrique: String,
    val summary: String,
)

data class BriefingResult(
    val dateStr: String,
    val byRubrique: Map<String, List<ProcessedArticle>>,
    val allArticles: List<ProcessedArticle>,
)

object FeedConfig {
    const val MAX_ARTICLES_PER_FEED = 8
    const val MAX_AGE_HOURS = 48
    const val MAX_TOTAL_ARTICLES = 40

    val feeds = listOf(
        Feed("lm-une", "Le Monde — À la une", "https://www.lemonde.fr/rss/une.xml"),
        Feed("lm-politique", "Le Monde — Politique", "https://www.lemonde.fr/politique/rss_full.xml"),
        Feed("lm-international", "Le Monde — International", "https://www.lemonde.fr/international/rss_full.xml"),
        Feed("lm-societe", "Le Monde — Société", "https://www.lemonde.fr/societe/rss_full.xml"),
        Feed("lm-economie", "Le Monde — Économie", "https://www.lemonde.fr/economie/rss_full.xml"),
        Feed("lm-sport", "Le Monde — Sport", "https://www.lemonde.fr/sport/rss_full.xml"),
        Feed("lm-culture", "Le Monde — Culture", "https://www.lemonde.fr/culture/rss_full.xml"),
        Feed("lm-sciences", "Le Monde — Sciences", "https://www.lemonde.fr/sciences/rss_full.xml"),
        Feed("lm-planete", "Le Monde — Planète", "https://www.lemonde.fr/planete/rss_full.xml"),
        Feed("lm-idees", "Le Monde — Idées", "https://www.lemonde.fr/idees/rss_full.xml"),
        Feed("cl-une", "La Charente Libre", "http://www.charentelibre.fr/rss.xml"),
        Feed("cl-charente", "Charente Libre — Charente", "https://www.charentelibre.fr/charente/rss.xml"),
    )

    val rubriques = listOf(
        "Politique",
        "Économie",
        "Société",
        "International",
        "Sport",
        "Culture",
        "Sciences",
        "Environnement",
        "Local",
        "Justice",
        "Faits divers",
        "Idées / Débats",
        "Autres",
    )
}
