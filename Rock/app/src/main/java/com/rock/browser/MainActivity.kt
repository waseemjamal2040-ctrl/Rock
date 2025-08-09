package com.rock.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.rock.browser.databinding.ActivityMainBinding
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("rock_settings", MODE_PRIVATE)
        val segments = prefs.getInt("segments", 4)

        with(binding.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = userAgentString + " Rock/1.0"
        }

        binding.webView.webViewClient = object : WebViewClient() {}

        binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            SegmentedDownloader(this, segments).download(url, fileName) {}
        }

        binding.goBtn.setOnClickListener { loadFromInput() }
        binding.searchInput.setOnEditorActionListener { _, _, _ ->
            loadFromInput(); true
        }

        binding.backBtn.setOnClickListener { if (binding.webView.canGoBack()) binding.webView.goBack() }
        binding.forwardBtn.setOnClickListener { if (binding.webView.canGoForward()) binding.webView.goForward() }
        binding.reloadBtn.setOnClickListener { binding.webView.reload() }
        binding.settingsBtn.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        val home = getString(R.string.default_home)
        binding.webView.loadUrl(home)
    }

    private fun loadFromInput() {
        val txt = binding.searchInput.text.toString().trim()
        if (txt.isEmpty()) return
        val url = if (Patterns.WEB_URL.matcher(txt).matches() || txt.startsWith("http")) {
            if (txt.startsWith("http")) txt else "https://$txt"
        } else {
            val q = URLEncoder.encode(txt, "UTF-8")
            "https://duckduckgo.com/?q=$q"
        }
        binding.webView.loadUrl(url)
    }
}
