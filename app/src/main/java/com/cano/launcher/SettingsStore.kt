package com.cano.launcher

import android.content.Context

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var favoritesFirst: Boolean
        get() = preferences.getBoolean(KEY_FAVORITES_FIRST, true)
        set(value) {
            preferences.edit().putBoolean(KEY_FAVORITES_FIRST, value).apply()
        }

    companion object {
        private const val KEY_FAVORITES_FIRST = "favorites_first"
    }
}
