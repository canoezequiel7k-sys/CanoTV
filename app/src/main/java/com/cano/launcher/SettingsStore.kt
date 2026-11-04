package com.cano.launcher

import android.content.Context
import android.net.Uri

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var favoritesFirst: Boolean
        get() = preferences.getBoolean(KEY_FAVORITES_FIRST, true)
        set(value) {
            preferences.edit().putBoolean(KEY_FAVORITES_FIRST, value).apply()
        }

    var backgroundUri: Uri?
        get() = preferences.getString(KEY_BACKGROUND_URI, null)?.let(Uri::parse)
        set(value) {
            preferences.edit().putString(KEY_BACKGROUND_URI, value?.toString()).apply()
        }

    companion object {
        private const val KEY_FAVORITES_FIRST = "favorites_first"
        private const val KEY_BACKGROUND_URI = "background_uri"
    }
}
