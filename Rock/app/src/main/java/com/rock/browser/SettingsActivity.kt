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
        val currentSegments = prefs.getInt("segments", 4)
        val currentLang = prefs.getString("lang", "en")!!
        val currentHome = prefs.getString("home", "") ?: ""
        binding.homeInput.setText(currentHome)

        val langAdapter = ArrayAdapter<String>(
            this,
            R.layout.spinner_item,
            listOf(getString(R.string.english) + " (en)", getString(R.string.arabic) + " (ar)")
        ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }
        binding.langSpinner.adapter = langAdapter
        binding.langSpinner.setSelection(if (currentLang == "en") 0 else 1)

        val segOptions = listOf(1,2,4,8)
        val segAdapter = ArrayAdapter<Int>(this, R.layout.spinner_item, segOptions).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        binding.segSpinner.adapter = segAdapter
        binding.segSpinner.setSelection(segOptions.indexOf(currentSegments))

        binding.saveBtn.setOnClickListener {
            val lang = if (binding.langSpinner.selectedItemPosition == 0) "en" else "ar"
            val segs = segOptions[binding.segSpinner.selectedItemPosition]
            val home = binding.homeInput.text.toString().trim()
            prefs.edit()
                .putString("lang", lang)
                .putInt("segments", segs)
                .putString("home", home)
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
