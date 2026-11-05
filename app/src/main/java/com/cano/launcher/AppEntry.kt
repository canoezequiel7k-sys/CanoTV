package com.cano.launcher

import android.graphics.drawable.Drawable

/** Datos de una actividad de otra app que CanoTV puede abrir. */
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val launchCategory: String,
    val label: String,
    val banner: Drawable?,
    val icon: Drawable?
)
