package com.example.contactmanager

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide

class ContactManagerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        applySavedTheme()
    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("theme_pref", MODE_PRIVATE)
        val themeMode = sharedPref.getInt("theme_mode", 0)
        when (themeMode) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) {
            Glide.get(this).onTrimMemory(level)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).onLowMemory()
    }
}
