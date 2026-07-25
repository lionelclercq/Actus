package fr.actus.sync.data

import android.util.Base64
import org.json.JSONObject

object ConfigBackup {
    private const val FORMAT_VERSION = 1

    fun export(settings: SettingsRepository): String {
        val json = JSONObject()
            .put("v", FORMAT_VERSION)
            .put("gemini", settings.geminiApiKey)
            .put("github_token", settings.githubToken)
            .put("github_repo", settings.githubRepo)
        return Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun import(settings: SettingsRepository, encoded: String) {
        val trimmed = encoded.trim()
        val jsonText = if (trimmed.startsWith("{")) {
            trimmed
        } else {
            String(Base64.decode(trimmed, Base64.DEFAULT), Charsets.UTF_8)
        }
        val json = JSONObject(jsonText)
        settings.geminiApiKey = json.optString("gemini", "")
        settings.githubToken = json.optString("github_token", "")
        settings.githubRepo = json.optString("github_repo", SettingsRepository.DEFAULT_REPO)
            .ifBlank { SettingsRepository.DEFAULT_REPO }
    }
}
