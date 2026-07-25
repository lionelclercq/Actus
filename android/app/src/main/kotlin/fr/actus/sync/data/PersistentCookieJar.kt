package fr.actus.sync.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(
    private val cookieRepository: CookieRepository,
) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // Cookies are saved from WebView login, not from OkHttp responses.
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        cookieRepository.loadForHost(url.host)
}
