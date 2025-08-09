package com.rock.browser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.rock.browser.databinding.ActivityMainBinding
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goBtn.setOnClickListener { openInExternalBrowser() }
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                openInExternalBrowser()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        binding.settingsBtn.setOnClickListener { 
            startActivity(Intent(this, SettingsActivity::class.java)) 
        }
    }

    private fun openInExternalBrowser() {
        val txt = binding.searchInput.text.toString().trim()
        if (txt.isEmpty()) return

        val urlString = if (Patterns.WEB_URL.matcher(txt).matches() || txt.startsWith("http")) {
            if (txt.startsWith("http")) txt else "https://$txt"
        } else {
            val prefs = getSharedPreferences("rock_settings", MODE_PRIVATE)
            val searchEngine = prefs.getString("homepage", "https://duckduckgo.com/?q=")
            val query = URLEncoder.encode(txt, "UTF-8")
            searchEngine + query
        }
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
            startActivity(intent)
        } catch (e: Exception) {
            // Optional: Show an error toast
        }
    }
}
