package com.example.booklibrary

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.example.booklibrary.ui.library.LibraryFragment
import com.example.booklibrary.ui.profile.ProfileFragment
import com.example.booklibrary.ui.settings.SettingsFragment
import com.example.booklibrary.utils.NetworkMonitor
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var fab: FloatingActionButton
    private lateinit var toolbar: MaterialToolbar
    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        toolbar = findViewById(R.id.top_toolbar)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            replaceFragment(LibraryFragment())
            bottomNavigationView.selectedItemId = R.id.bottom_nav_library
            updateToolbarTitle(R.id.bottom_nav_library)
        } else {
            val selectedItemId = savedInstanceState.getInt("selected_nav_item", R.id.bottom_nav_library)
            bottomNavigationView.selectedItemId = selectedItemId
            updateToolbarTitle(selectedItemId)
        }
        fab = findViewById(R.id.fab_add)
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            showFab(false)
            when (menuItem.itemId) {
                R.id.bottom_nav_library -> {
                    replaceFragment(LibraryFragment())
                    toolbar.title = getString(R.string.top_library)
                    true
                }
                R.id.bottom_nav_profile -> {
                    toolbar.title = getString(R.string.top_profile)
                    replaceFragment(ProfileFragment())
                    true
                }
                R.id.bottom_nav_settings -> {
                    toolbar.title = getString(R.string.top_settings)
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
        val tvNoInternet = findViewById<TextView>(R.id.tv_no_internet)
        networkMonitor.observe(this) { isConnected ->
            tvNoInternet.visibility = if (isConnected) View.GONE else View.VISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        outState.putInt("selected_nav_item", bottomNavigationView.selectedItemId)
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(newBase)
        val language = prefs.getString("language", "english") ?: "english"
        val locale = when (language) {
            "russian" -> Locale("ru")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun applyOverrideConfiguration(overrideConfig: Configuration) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val language = prefs.getString("language", "english") ?: "english"
        val locale = when (language) {
            "russian" -> Locale("ru")
            else -> Locale("en")
        }
        overrideConfig.setLocale(locale)
        super.applyOverrideConfiguration(overrideConfig)
    }

    fun updateToolbarTitle(itemId: Int) {
        toolbar.title = when (itemId) {
            R.id.bottom_nav_library -> getString(R.string.top_library)
            R.id.bottom_nav_profile -> getString(R.string.top_profile)
            R.id.bottom_nav_settings -> getString(R.string.top_settings)
            else -> getString(R.string.top_library)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }

    fun showFab(show: Boolean, onClickAction: (() -> Unit)? = null) {
        if (show) {
            fab.visibility = View.VISIBLE
            onClickAction?.let { fab.setOnClickListener { it() } }
        } else {
            fab.visibility = View.GONE
        }
    }

}

