package fr.actus.sync.data

import android.util.Base64
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WikiPusher(
    private val repo: String,
    private val token: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    fun pushAll(files: Map<String, String>) {
        val wikiRepo = wikiRepoName(repo)
        verifyWikiAccess(wikiRepo)
        files.forEach { (name, content) ->
            pushFile(wikiRepo, name, content)
        }
    }

    private fun verifyWikiAccess(wikiRepo: String) {
        val url = "https://api.github.com/repos/$wikiRepo"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) return
            val body = response.body?.string().orEmpty()
            if (response.code == 404) {
                throw IllegalStateException(
                    "Accès au wiki GitHub refusé (HTTP 404). " +
                        "Créez un token **classique** (https://github.com/settings/tokens) " +
                        "avec le scope « repo » — les tokens fine-grained ne peuvent pas écrire sur Actus.wiki.",
                )
            }
            throw IllegalStateException("Wiki inaccessible: HTTP ${response.code} $body")
        }
    }

    private fun wikiRepoName(repo: String): String {
        val parts = repo.split("/", limit = 2)
        require(parts.size == 2) { "Repo invalide: $repo" }
        return "${parts[0]}/${parts[1]}.wiki"
    }

    private fun pushFile(wikiRepo: String, fileName: String, content: String) {
        val encodedPath = fileName.replace(" ", "%20")
        val getUrl = "https://api.github.com/repos/$wikiRepo/contents/$encodedPath"
        val sha = fetchExistingSha(getUrl)

        val payload = JSONObject()
            .put("message", "briefing actus-sync $fileName")
            .put(
                "content",
                Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
            )
        if (sha != null) payload.put("sha", sha)

        val request = Request.Builder()
            .url(getUrl)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .put(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                throw IllegalStateException("Wiki push $fileName failed: HTTP ${response.code} $body")
            }
            Log.i(TAG, "Pushed $fileName")
        }
    }

    private fun fetchExistingSha(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            return JSONObject(body).optString("sha").ifBlank { null }
        }
    }

    companion object {
        private const val TAG = "WikiPusher"

        fun wikiUrl(repo: String): String = "https://github.com/$repo/wiki/Home"
    }
}
