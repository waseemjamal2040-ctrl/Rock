package com.rock.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.webkit.*
import android.view.View
import android.graphics.Color
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

        binding.webView.setBackgroundColor(Color.BLACK)
        binding.webView.webViewClient = object : WebViewClient() {}
        // ابدأ بصفحة فارغة وأخفي الويب فيو
        binding.webView.loadUrl("about:blank")
        binding.webView.visibility = View.INVISIBLE

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
    }

    private fun loadFromInput() {
        val txt = binding.searchInput.text.toString().trim()
        if (txt.isEmpty()) return

        // لو المستخدم كتب رابط مباشر أو http
        val url = if (Patterns.WEB_URL.matcher(txt).matches() || txt.startsWith("http")) {
            if (txt.startsWith("http")) txt else "https://$txt"
        } else {
            val q = URLEncoder.encode(txt, "UTF-8")
            // استخدم أي محرك بحث تفضله هنا
            "https://duckduckgo.com/?q=$q"
        }

        if (binding.webView.visibility != View.VISIBLE) {
            binding.webView.visibility = View.VISIBLE
        }
        binding.webView.loadUrl(url)
    }
}
