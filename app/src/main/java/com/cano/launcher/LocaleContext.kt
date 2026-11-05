package com.cano.launcher

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

/** Aplica el idioma guardado a los recursos de cada pantalla. */
object LocaleContext {
    fun wrap(context: Context): Context {
        val language = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString(SettingsStore.KEY_LANGUAGE, SettingsStore.LANGUAGE_SPANISH)
            ?: SettingsStore.LANGUAGE_SPANISH
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return ContextWrapper(context.createConfigurationContext(configuration))
    }
}
