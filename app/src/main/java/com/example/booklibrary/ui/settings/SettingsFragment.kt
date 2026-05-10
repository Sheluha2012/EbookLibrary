package com.example.booklibrary.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.booklibrary.R
import com.example.booklibrary.data.db.BookDatabase
import com.example.booklibrary.ui.auth.AuthActivity
import com.example.booklibrary.utils.ReminderWorker
import com.example.booklibrary.viewmodel.BookViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SettingsFragment : PreferenceFragmentCompat() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var viewModel: BookViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            findPreference<SwitchPreferenceCompat>("notifications_enabled")?.isChecked = false
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        viewModel = ViewModelProvider(this)[BookViewModel::class.java]

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

        val notifEnabledPref = findPreference<SwitchPreferenceCompat>("notifications_enabled")
        notifEnabledPref?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            if (isEnabled) {
                checkNotificationPermission()
                scheduleNotification()
            } else {
                cancelNotification()
            }
            true
        }

        val notifTimePref = findPreference<Preference>("notification_time")
        notifTimePref?.setOnPreferenceClickListener {
            showTimePicker()
            true
        }
        updateTimeSummary()

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

        val logoutPreference = findPreference<Preference>("logout")
        logoutPreference?.setOnPreferenceClickListener {
            performLogout()
            true
        }
    }

    private fun performLogout() {
        viewModel.logout {
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showTimePicker() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val hour = prefs.getInt("notification_hour", 20)
        val minute = prefs.getInt("notification_minute", 0)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            prefs.edit()
                .putInt("notification_hour", selectedHour)
                .putInt("notification_minute", selectedMinute)
                .apply()

            updateTimeSummary()

            val isEnabled = prefs.getBoolean("notifications_enabled", false)
            if (isEnabled) {
                scheduleNotification()
            }
        }, hour, minute, true).show()
    }

    private fun updateTimeSummary() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val h = prefs.getInt("notification_hour", 20)
        val m = prefs.getInt("notification_minute", 0)
        findPreference<Preference>("notification_time")?.summary =
            getString(R.string.settings_notif_time) + String.format(" %02d:%02d", h, m)
    }

    private fun scheduleNotification() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val h = prefs.getInt("notification_hour", 20)
        val m = prefs.getInt("notification_minute", 0)

        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, h)
        calendar.set(Calendar.MINUTE, m)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = calendar.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("reading_reminder_tag")
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "reading_reminder_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cancelNotification() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("reading_reminder_work")
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