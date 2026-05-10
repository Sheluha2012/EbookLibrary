package com.example.booklibrary

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.imagekit.android.ImageKit
import com.imagekit.android.entity.TransformationPosition

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val isNightMode = prefs.getBoolean("night_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        ImageKit.init(
            context = this,
            publicKey = "public_tVvP41567/wDGdW86vaUQJqOlqQ=",
            urlEndpoint = "https://ik.imagekit.io/6qjc3wybl/",
            transformationPosition = TransformationPosition.PATH
        )
    }
}