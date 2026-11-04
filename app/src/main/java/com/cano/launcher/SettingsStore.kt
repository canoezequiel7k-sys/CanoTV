package com.cano.launcher

import android.content.Context

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    init {
        if (preferences.contains(KEY_BACKGROUND_URI)) {
            preferences.edit().remove(KEY_BACKGROUND_URI).apply()
        }
    }

    var favoritesFirst: Boolean
        get() = preferences.getBoolean(KEY_FAVORITES_FIRST, true)
        set(value) {
            preferences.edit().putBoolean(KEY_FAVORITES_FIRST, value).apply()
        }

    var themeId: String
        get() = preferences.getString(KEY_THEME_ID, ThemePalette.PURPLE) ?: ThemePalette.PURPLE
        set(value) {
            preferences.edit()
                .putString(KEY_THEME_ID, value)
                .remove(KEY_BACKGROUND_URI)
                .apply()
        }

    companion object {
        private const val KEY_FAVORITES_FIRST = "favorites_first"
        private const val KEY_BACKGROUND_URI = "background_uri"
        private const val KEY_THEME_ID = "theme_id"
    }
}
