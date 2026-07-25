package fr.actus.sync.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build(),
) {
    fun processArticle(article: Article, rubriques: List<String>): ProcessedArticle {
        val source = article.fullText.ifBlank { article.excerpt }.ifBlank { article.title }
        val rubList = rubriques.joinToString(", ")
        val prompt = """
            Tu es un journaliste. Analyse cet article et réponds en JSON strict.

            Rubriques possibles (choisir UNE seule) : $rubList

            Champs JSON requis :
            - "rubrique" : la rubrique choisie (exactement comme dans la liste)
            - "resume" : résumé complet en français, 8 à 12 phrases factuelles, style presse

            Article :
            Titre : ${article.title}
            Source : ${article.feedName}
            URL : ${article.link}
            Texte :
            ${source.take(10_000)}
        """.trimIndent()

        return try {
            val json = callGemini(prompt)
            var rubrique = json.optString("rubrique", "Autres")
            if (rubrique !in rubriques) rubrique = "Autres"
            val summary = json.optString("resume", article.excerpt.ifBlank { article.title })
            ProcessedArticle(article, rubrique, summary)
        } catch (_: Exception) {
            ProcessedArticle(
                article = article,
                rubrique = "Autres",
                summary = article.excerpt.ifBlank { article.title },
            )
        }
    }

    suspend fun buildBriefing(
        articles: List<Article>,
        rubriques: List<String>,
        dateStr: String,
        onProgress: suspend (Int, Int, String) -> Unit = { _, _, _ -> },
    ): BriefingResult {
        val all = mutableListOf<ProcessedArticle>()
        val byRubrique = linkedMapOf<String, MutableList<ProcessedArticle>>()
        val total = articles.size

        for ((index, article) in articles.withIndex()) {
            onProgress(index + 1, total, article.title)
            val processed = processArticle(article, rubriques)
            all += processed
            byRubrique.getOrPut(processed.rubrique) { mutableListOf() } += processed
            Thread.sleep(1200)
        }

        return BriefingResult(dateStr, byRubrique, all)
    }

    private fun callGemini(prompt: String): JSONObject {
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
        val payload = JSONObject()
            .put(
                "contents",
                org.json.JSONArray().put(
                    JSONObject().put(
                        "parts",
                        org.json.JSONArray().put(JSONObject().put("text", prompt)),
                    ),
                ),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.2)
                    .put("maxOutputTokens", 2048)
                    .put("responseMimeType", "application/json"),
            )

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Gemini HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val root = JSONObject(body)
            val text = root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
            return JSONObject(text)
        }
    }
}
