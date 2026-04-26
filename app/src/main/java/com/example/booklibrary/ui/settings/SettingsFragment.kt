package com.example.booklibrary.ui.settings

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.booklibrary.R
import com.example.booklibrary.data.db.BookDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SettingsFragment : PreferenceFragmentCompat() {
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val nightModeSwitch = findPreference<SwitchPreferenceCompat>("night_mode")
        nightModeSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val isNightModeEnabled = newValue as Boolean
            applyNightMode(isNightModeEnabled)
            true
        }

        val languagePreference = findPreference<ListPreference>("language")
        updateLanguageSummary(languagePreference)

        languagePreference?.setOnPreferenceChangeListener { _, newValue ->
            val selectedLanguage = newValue as String
            applyLanguage(selectedLanguage)
            true
        }

        val clearCachePreference = findPreference<Preference>("clear_cache")
        clearCachePreference?.setOnPreferenceClickListener {
            showClearCacheDialog()
            true
        }

        val developersPreference = findPreference<Preference>("developers")
        developersPreference?.setOnPreferenceClickListener {
            //showDevelopersInfo()
            true
        }
    }

    private fun applyNightMode(isNightModeEnabled: Boolean) {
        if (isNightModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun applyLanguage(languageCode: String) {
        val locale = when (languageCode) {
            "russian" -> Locale("ru")
            else -> Locale("en")
        }
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        activity?.recreate()
    }

    private fun updateLanguageSummary(languagePreference: ListPreference?) {
        languagePreference?.let {
            val currentValue = it.value
            val index = it.entryValues.indexOf(currentValue)
            if (index >= 0) {
                it.summary = it.entries[index]
            }
        }
    }

    private fun showClearCacheDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_clear_cache)
            .setMessage(R.string.settings_clear_cache_confirm)
            .setNegativeButton(R.string.dialog_cancel) { _, _ -> }
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                clearCache()
            }
            .show()
    }

    private fun clearCache() {
        lifecycleScope.launch {
            val db = BookDatabase.getDatabase(requireContext())
            db.cachedBookDao().clearAll()
            Toast.makeText(requireContext(), R.string.settings_cache_cleared, Toast.LENGTH_SHORT).show()
        }
    }

}