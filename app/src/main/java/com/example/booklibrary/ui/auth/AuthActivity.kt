package com.example.booklibrary.ui.auth

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.booklibrary.MainActivity
import com.example.booklibrary.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val btnPrimary = findViewById<MaterialButton>(R.id.btn_primary)
        val btnSecondary = findViewById<MaterialButton>(R.id.btn_secondary)
        val tvTitle = findViewById<TextView>(R.id.tv_auth_title)
        val progress = findViewById<ProgressBar>(R.id.auth_progress)

        btnSecondary.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                tvTitle.text = getString(R.string.auth_title_login)
                btnPrimary.text = getString(R.string.auth_btn_login)
                btnSecondary.text = getString(R.string.auth_btn_to_reg)
            } else {
                tvTitle.text = getString(R.string.auth_title_reg)
                btnPrimary.text = getString(R.string.auth_btn_reg)
                btnSecondary.text = getString(R.string.auth_btn_to_login)
            }
        }

        btnPrimary.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Valid email & password (min 6 chars) required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            btnPrimary.isEnabled = false

            if (isLoginMode) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            progress.visibility = View.GONE
                            btnPrimary.isEnabled = true
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            progress.visibility = View.GONE
                            btnPrimary.isEnabled = true
                            Toast.makeText(this, "Reg failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(newBase)
        val language = prefs.getString("language", "english") ?: "english"
        val locale = if (language == "russian") Locale("ru") else Locale("en")
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun applyOverrideConfiguration(overrideConfig: Configuration) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val language = prefs.getString("language", "english") ?: "english"
        val locale = if (language == "russian") Locale("ru") else Locale("en")
        overrideConfig.setLocale(locale)
        super.applyOverrideConfiguration(overrideConfig)
    }
}