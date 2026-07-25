package fr.actus.sync.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.actus.sync.data.CookieRepository
import fr.actus.sync.data.CookieRepositoryHolder
import fr.actus.sync.databinding.ActivityLoginWebviewBinding

class LoginWebViewActivity : AppCompatActivity() {
  private lateinit var binding: ActivityLoginWebviewBinding
  private lateinit var site: CookieRepository.PublisherSite

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityLoginWebviewBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val siteName = intent.getStringExtra(EXTRA_SITE).orEmpty()
    site = CookieRepository.PublisherSite.entries.firstOrNull { it.name == siteName }
      ?: run {
        finish()
        return
      }

    binding.toolbar.setNavigationOnClickListener { finish() }
    binding.toolbar.title = "Connexion ${site.label}"

    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)

    binding.webView.settings.javaScriptEnabled = true
    binding.webView.settings.domStorageEnabled = true
    binding.webView.webChromeClient = WebChromeClient()
    binding.webView.webViewClient = object : WebViewClient() {
      override fun onPageFinished(view: WebView?, url: String?) {
        binding.statusText.text = url.orEmpty()
        if (url != null && isLikelyLoggedIn(url)) {
          CookieRepositoryHolder.repository.saveFromWebView(site)
          Toast.makeText(
            this@LoginWebViewActivity,
            "Session ${site.label} enregistrée",
            Toast.LENGTH_SHORT,
          ).show()
        }
      }
    }

    binding.saveSessionButton.setOnClickListener {
      CookieRepositoryHolder.repository.saveFromWebView(site)
      Toast.makeText(this, "Session ${site.label} enregistrée", Toast.LENGTH_SHORT).show()
      setResult(RESULT_OK)
      finish()
    }

    binding.webView.loadUrl(site.loginUrl)
  }

  private fun isLikelyLoggedIn(url: String): Boolean {
    val lower = url.lowercase()
    return when (site) {
      CookieRepository.PublisherSite.LEMONDE ->
        lower.contains("lemonde.fr") && !lower.contains("connexion") && !lower.contains("login")
      CookieRepository.PublisherSite.CHARENTELIBRE ->
        lower.contains("charentelibre.fr") &&
          !lower.contains("connexion") &&
          !lower.contains("login") &&
          !lower.contains("inscription")
    }
  }

  companion object {
    const val EXTRA_SITE = "publisher_site"
  }
}
