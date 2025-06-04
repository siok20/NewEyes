package com.neweyes

import android.content.Context

object PreferenceManager {
    private const val PREF_NAME = "app_prefs"
    private const val KEY_HIGH_CONTRAST = "high_contrast"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isHighContrast(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HIGH_CONTRAST, false)
    }

    fun setHighContrast(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HIGH_CONTRAST, value)
            .apply()
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }
}
