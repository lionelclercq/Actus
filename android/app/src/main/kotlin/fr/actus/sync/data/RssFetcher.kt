package fr.actus.sync.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class RssFetcher(
    private val client: OkHttpClient = defaultClient(),
) {
    fun fetchAll(
        feeds: List<Feed> = FeedConfig.feeds,
        maxPerFeed: Int = FeedConfig.MAX_ARTICLES_PER_FEED,
        maxAgeHours: Int = FeedConfig.MAX_AGE_HOURS,
        maxTotal: Int = FeedConfig.MAX_TOTAL_ARTICLES,
    ): List<Article> {
        val cutoff = Instant.now().minusSeconds(maxAgeHours.toLong() * 3600)
        val seen = mutableSetOf<String>()
        val articles = mutableListOf<Article>()

        for (feed in feeds) {
            if (articles.size >= maxTotal) break
            try {
                val xml = fetchText(feed.url) ?: continue
                val parsed = parseFeed(xml, feed, maxPerFeed, cutoff, seen)
                for (article in parsed) {
                    if (articles.size >= maxTotal) break
                    articles += article
                }
            } catch (e: Exception) {
                Log.w(TAG, "Flux ignoré (${feed.name}): ${e.message}")
            }
        }

        return articles.sortedByDescending { it.published ?: Instant.EPOCH }
    }

    private fun fetchText(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ActusSync/1.0 (+https://github.com/lionelclercq/Actus)")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun parseFeed(
        xml: String,
        feed: Feed,
        maxPerFeed: Int,
        cutoff: Instant,
        seen: MutableSet<String>,
    ): List<Article> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xml.reader())
        val items = mutableListOf<Article>()
        var event = parser.eventType
        var inItem = false
        var title = ""
        var link = ""
        var description = ""
        var pubDate = ""
        var imageUrl = ""

        while (event != XmlPullParser.END_DOCUMENT && items.size < maxPerFeed) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> {
                        inItem = true
                        title = ""
                        link = ""
                        description = ""
                        pubDate = ""
                        imageUrl = ""
                    }
                    "title" -> if (inItem) title = parser.nextText().trim()
                    "link" -> if (inItem) link = parser.nextText().trim()
                    "description" -> if (inItem) description = parser.nextText().trim()
                    "pubDate" -> if (inItem) pubDate = parser.nextText().trim()
                    "enclosure" -> if (inItem) {
                        val type = parser.getAttributeValue(null, "type").orEmpty()
                        if (type.startsWith("image/")) {
                            imageUrl = parser.getAttributeValue(null, "url").orEmpty()
                        }
                    }
                    "content", "encoded" -> if (inItem) {
                        val content = parser.nextText()
                        if (description.isBlank()) description = content
                        if (imageUrl.isBlank()) imageUrl = extractImageFromHtml(content)
                    }
                    "thumbnail" -> if (inItem) {
                        imageUrl = parser.getAttributeValue(null, "url").orEmpty()
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "item" && inItem) {
                    inItem = false
                    val cleanLink = link.trim()
                    if (cleanLink.isNotBlank() && cleanLink !in seen) {
                        val published = parseDate(pubDate)
                        if (published == null || !published.isBefore(cutoff)) {
                            seen += cleanLink
                            items += Article(
                                feedId = feed.id,
                                feedName = feed.name,
                                title = stripHtml(title),
                                link = cleanLink,
                                published = published,
                                excerpt = stripHtml(description).take(2000),
                                imageUrl = imageUrl.ifBlank { extractImageFromHtml(description) },
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }
        return items
    }

    private fun parseDate(raw: String): Instant? {
        if (raw.isBlank()) return null
        val patterns = listOf(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        )
        for (pattern in patterns) {
            try {
                return ZonedDateTime.parse(raw, pattern).toInstant()
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun stripHtml(text: String): String =
        text.replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun extractImageFromHtml(html: String): String {
        val match = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)
        return match?.groupValues?.getOrNull(1).orEmpty()
    }

    companion object {
        private const val TAG = "RssFetcher"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
