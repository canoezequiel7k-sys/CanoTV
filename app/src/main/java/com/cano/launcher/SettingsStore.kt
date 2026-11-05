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

    var language: String
        get() = preferences.getString(KEY_LANGUAGE, LANGUAGE_SPANISH) ?: LANGUAGE_SPANISH
        set(value) {
            preferences.edit()
                .putString(
                    KEY_LANGUAGE,
                    if (value == LANGUAGE_ENGLISH) LANGUAGE_ENGLISH else LANGUAGE_SPANISH
                )
                .apply()
        }

    var appFont: String
        get() = preferences.getString(KEY_APP_FONT, FONT_SANS) ?: FONT_SANS
        set(value) {
            preferences.edit()
                .putString(KEY_APP_FONT, value.takeIf { it in APP_FONTS } ?: FONT_SANS)
                .apply()
        }

    var appLabelSize: Int
        get() = preferences.getInt(KEY_APP_LABEL_SIZE, DEFAULT_APP_LABEL_SIZE)
            .coerceIn(MIN_APP_LABEL_SIZE, MAX_APP_LABEL_SIZE)
        set(value) {
            preferences.edit()
                .putInt(KEY_APP_LABEL_SIZE, value.coerceIn(MIN_APP_LABEL_SIZE, MAX_APP_LABEL_SIZE))
                .apply()
        }

    companion object {
        private const val KEY_FAVORITES_FIRST = "favorites_first"
        private const val KEY_BACKGROUND_URI = "background_uri"
        private const val KEY_THEME_ID = "theme_id"
        const val KEY_LANGUAGE = "language"
        private const val KEY_APP_FONT = "app_font"
        private const val KEY_APP_LABEL_SIZE = "app_label_size"

        const val LANGUAGE_SPANISH = "es"
        const val LANGUAGE_ENGLISH = "en"
        const val FONT_SANS = "sans"
        const val FONT_ROUNDED = "rounded"
        const val FONT_SERIF = "serif"
        const val DEFAULT_APP_LABEL_SIZE = 18
        const val MIN_APP_LABEL_SIZE = 16
        const val MAX_APP_LABEL_SIZE = 20

        private val APP_FONTS = setOf(FONT_SANS, FONT_ROUNDED, FONT_SERIF)
    }
}
