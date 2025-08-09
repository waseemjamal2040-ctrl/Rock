package com.rock.browser

import android.content.res.Configuration
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.rock.browser.databinding.ActivitySettingsBinding
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.settings_title)

        val prefs = getSharedPreferences("rock_settings", MODE_PRIVATE)
        val currentLang = prefs.getString("lang", "en")!!
        val currentHomepage = prefs.getString("homepage", "https://duckduckgo.com/?q=")

        binding.homepageInput.setText(currentHomepage)

        val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.english) + " (en)", getString(R.string.arabic) + " (ar)"))
        binding.langSpinner.adapter = langAdapter
        binding.langSpinner.setSelection(if (currentLang == "en") 0 else 1)

        binding.saveBtn.setOnClickListener {
            val lang = if (binding.langSpinner.selectedItemPosition == 0) "en" else "ar"
            val homepage = binding.homepageInput.text.toString().trim()
            
            prefs.edit()
                .putString("lang", lang)
                .putString("homepage", homepage)
                .apply()

            applyLanguage(lang)
            finish()
        }
    }

    private fun applyLanguage(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
