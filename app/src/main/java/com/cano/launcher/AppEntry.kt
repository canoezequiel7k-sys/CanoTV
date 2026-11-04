package com.cano.launcher

import android.graphics.drawable.Drawable

data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?
)
