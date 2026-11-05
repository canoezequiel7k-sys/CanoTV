package com.cano.launcher

import android.content.Context

/** Guarda localmente los favoritos identificados por paquete y actividad. */
class FavoritesStore(context: Context) {
    private val preferences = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun getFavorites(): Set<String> = preferences.getStringSet(KEY_FAVORITES, emptySet())
        .orEmpty()
        .toSet()

    fun toggle(app: AppEntry): Boolean {
        val favorites = getFavorites().toMutableSet()
        val key = app.key()
        val added = if (favorites.add(key)) true else {
            favorites.remove(key)
            false
        }
        preferences.edit().putStringSet(KEY_FAVORITES, favorites).apply()
        return added
    }

    fun removeMissingApps(apps: List<AppEntry>) {
        val installed = apps.mapTo(mutableSetOf()) { it.key() }
        preferences.edit()
            .putStringSet(KEY_FAVORITES, getFavorites().intersect(installed))
            .apply()
    }

    companion object {
        private const val KEY_FAVORITES = "favorite_components"
    }
}

private fun AppEntry.key(): String = "$packageName/$activityName"
