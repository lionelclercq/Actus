package fr.actus.sync.data

import android.content.Context
import android.webkit.CookieManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class CookieRepository(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "actus_cookies",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveFromWebView(site: PublisherSite) {
        val cookieHeader = CookieManager.getInstance().getCookie(site.baseUrl).orEmpty()
        if (cookieHeader.isNotBlank()) {
            prefs.edit().putString(site.prefKey, cookieHeader).apply()
        }
    }

    fun clear(site: PublisherSite) {
        prefs.edit().remove(site.prefKey).apply()
        CookieManager.getInstance().removeAllCookies(null)
    }

    fun isLoggedIn(site: PublisherSite): Boolean =
        prefs.getString(site.prefKey, "").orEmpty().isNotBlank()

    fun loadForHost(host: String): List<Cookie> {
        val site = PublisherSite.fromHost(host) ?: return emptyList()
        val raw = prefs.getString(site.prefKey, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return parseCookieHeader(raw, site.baseUrl)
    }

    private fun parseCookieHeader(header: String, baseUrl: String): List<Cookie> {
        val httpUrl = baseUrl.toHttpUrlOrNull() ?: return emptyList()
        return header.split(';')
            .mapNotNull { part ->
                val trimmed = part.trim()
                if (trimmed.isBlank() || !trimmed.contains('=')) return@mapNotNull null
                val eq = trimmed.indexOf('=')
                val name = trimmed.substring(0, eq).trim()
                val value = URLDecoder.decode(trimmed.substring(eq + 1).trim(), StandardCharsets.UTF_8)
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(httpUrl.host)
                    .path("/")
                    .build()
            }
    }

    enum class PublisherSite(
        val prefKey: String,
        val label: String,
        val loginUrl: String,
        val baseUrl: String,
        val cookieDomain: String,
    ) {
        LEMONDE(
            prefKey = "cookies_lemonde",
            label = "Le Monde",
            loginUrl = "https://secure.lemonde.fr/sfuser/connexion",
            baseUrl = "https://www.lemonde.fr",
            cookieDomain = "lemonde.fr",
        ),
        CHARENTELIBRE(
            prefKey = "cookies_charentelibre",
            label = "Charente Libre",
            loginUrl = "https://www.charentelibre.fr/mon-compte/connexion",
            baseUrl = "https://www.charentelibre.fr",
            cookieDomain = "charentelibre.fr",
        );

        companion object {
            fun fromHost(host: String): PublisherSite? {
                val h = host.lowercase().removePrefix("www.")
                return entries.firstOrNull { h.endsWith(it.cookieDomain) }
            }
        }
    }
}
