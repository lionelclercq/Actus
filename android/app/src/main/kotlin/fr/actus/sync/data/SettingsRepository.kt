package fr.actus.sync.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "actus_settings",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_GEMINI, value.trim()).apply()

    var githubToken: String
        get() = prefs.getString(KEY_GITHUB_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_GITHUB_TOKEN, value.trim()).apply()

    var githubRepo: String
        get() = prefs.getString(KEY_GITHUB_REPO, DEFAULT_REPO).orEmpty()
        set(value) = prefs.edit().putString(KEY_GITHUB_REPO, value.trim()).apply()

    var lastSyncMessage: String
        get() = prefs.getString(KEY_LAST_SYNC, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_SYNC, value).apply()

    fun isConfigured(): Boolean =
        geminiApiKey.isNotBlank() && githubToken.isNotBlank() && githubRepo.isNotBlank()

    companion object {
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_GITHUB_REPO = "github_repo"
        private const val KEY_LAST_SYNC = "last_sync_message"
        const val DEFAULT_REPO = "lionelclercq/Actus"
    }
}
