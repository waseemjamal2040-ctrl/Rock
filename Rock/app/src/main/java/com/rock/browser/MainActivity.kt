package com.rock.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.webkit.*
import android.view.View
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.rock.browser.databinding.ActivityMainBinding
import coil.load
import org.json.JSONArray
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
        val bgUrl = prefs.getString("bg_url", "") ?: ""

        // تحميل خلفية
        if (bgUrl.isNotBlank()) {
            binding.bgImage.load(bgUrl)
        } else {
            binding.bgImage.setImageResource(R.drawable.bg_placeholder)
        }

        with(binding.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = userAgentString + " Rock/1.0"
        }

        binding.webView.setBackgroundColor(Color.BLACK)
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { addToHistory(it) }
            }
        }

        // ابدأ مخفي
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

        // زر السجل
        binding.historyBtn.setOnClickListener { showHistoryDialog() }

        // زر الرئيسية
        binding.homeBtn.setOnClickListener {
            val home = prefs.getString("home", "") ?: ""
            if (home.isNotBlank()) {
                showWeb()
                binding.webView.loadUrl(home)
            } else {
                binding.webView.loadUrl("about:blank")
                binding.webView.visibility = View.INVISIBLE
            }
        }
    }

    private fun showWeb() {
        if (binding.webView.visibility != View.VISIBLE) binding.webView.visibility = View.VISIBLE
    }

    private fun searchEngineBase(): String {
        val engine = getSharedPreferences("rock_settings", MODE_PRIVATE)
            .getString("engine", "duck") ?: "duck"
        return when (engine) {
            "google" -> "https://www.google.com/search?q="
            "bing"   -> "https://www.bing.com/search?q="
            "brave"  -> "https://search.brave.com/search?q="
            else     -> "https://duckduckgo.com/?q="
        }
    }

    private fun loadFromInput() {
        val txt = binding.searchInput.text.toString().trim()
        if (txt.isEmpty()) return

        val url = if (Patterns.WEB_URL.matcher(txt).matches() || txt.startsWith("http")) {
            if (txt.startsWith("http")) txt else "https://$txt"
        } else {
            val q = URLEncoder.encode(txt, "UTF-8")
            searchEngineBase() + q
        }

        showWeb()
        binding.webView.loadUrl(url)
    }

    // ====== السجل البسيط (آخر 50) ======
    private fun addToHistory(u: String) {
        val sp = getSharedPreferences("rock_settings", MODE_PRIVATE)
        val arr = JSONArray(sp.getString("history", "[]"))
        // لا تكرر المتتالي
        if (arr.length() == 0 || arr.getString(arr.length()-1) != u) {
            arr.put(u)
            // اقتطع لأحدث 50
            val trimmed = JSONArray()
            val start = if (arr.length() > 50) arr.length() - 50 else 0
            for (i in start until arr.length()) trimmed.put(arr.getString(i))
            sp.edit().putString("history", trimmed.toString()).apply()
        }
    }

    private fun showHistoryDialog() {
        val sp = getSharedPreferences("rock_settings", MODE_PRIVATE)
        val arr = JSONArray(sp.getString("history", "[]"))
        val items = Array(arr.length()) { i -> arr.getString(arr.length()-1 - i) } // الأحدث أولاً
        if (items.isEmpty()) {
            AlertDialog.Builder(this).setMessage("لا يوجد عناصر في السجل.")
                .setPositiveButton("حسناً", null).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.history))
            .setItems(items) { _, which ->
                val url = items[which]
                showWeb()
                binding.webView.loadUrl(url)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
