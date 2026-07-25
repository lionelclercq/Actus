package fr.actus.sync.data

import android.util.Log
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ArticleEnricher(
    cookieJar: CookieJar = PersistentCookieJar(CookieRepositoryHolder.repository),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    fun enrich(articles: List<Article>): List<Article> =
        articles.map { enrichOne(it) }

    private fun enrichOne(article: Article): Article {
        val site = PublisherSiteFromUrl(article.link) ?: return article
        if (!CookieRepositoryHolder.repository.isLoggedIn(site)) return article

        return try {
            val request = Request.Builder()
                .url(article.link)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                )
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return article
                val html = response.body?.string().orEmpty()
                if (html.length < 500 || html.contains("paywall", ignoreCase = true)) {
                    return article
                }
                val (fullText, extraImage) = extractArticleHtml(html)
                if (fullText.length < 200) return article
                article.copy(
                    fullText = fullText,
                    imageUrl = article.imageUrl.ifBlank { extraImage },
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Enrichissement échoué ${article.link}: ${e.message}")
            article
        }
    }

    private fun extractArticleHtml(html: String): Pair<String, String> {
        val images = mutableListOf<String>()
        Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html)
            .forEach { match ->
                val src = match.groupValues[1]
                if (src.startsWith("http") && !src.contains("pixel") && !src.contains("tracking")) {
                    images += src
                }
            }

        val patterns = listOf(
            Regex("""<article[^>]*>([\s\S]*?)</article>""", RegexOption.IGNORE_CASE),
            Regex("""class="article__content"[^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE),
            Regex("""class="content"[^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE),
            Regex("""itemprop="articleBody"[^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE),
            Regex("""class="article-content"[^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE),
        )

        var body = ""
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null && match.groupValues[1].length > body.length) {
                body = match.groupValues[1]
            }
        }
        if (body.isBlank()) body = html

        var text = body
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(15_000)

        return text to images.firstOrNull().orEmpty()
    }

    private fun PublisherSiteFromUrl(url: String): CookieRepository.PublisherSite? {
        val host = url.substringAfter("://").substringBefore('/').lowercase().removePrefix("www.")
        return CookieRepository.PublisherSite.fromHost(host)
    }

    companion object {
        private const val TAG = "ArticleEnricher"
    }
}

/** Holder for singleton cookie repo set from Application. */
object CookieRepositoryHolder {
    lateinit var repository: CookieRepository
        private set

    fun init(context: android.content.Context) {
        if (!::repository.isInitialized) {
            repository = CookieRepository(context.applicationContext)
        }
    }
}
