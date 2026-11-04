package com.cano.launcher

import android.content.Intent
import android.content.pm.PackageManager

class AppRepository(private val packageManager: PackageManager) {

    fun getLaunchableApps(excludedPackage: String): List<AppEntry> {
        val seen = linkedSetOf<String>()
        val apps = mutableListOf<AppEntry>()

        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        )

        for (intent in intents) {
            val resolveInfoList = packageManager.queryIntentActivities(intent, 0) ?: continue
            for (resolveInfo in resolveInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == excludedPackage) continue

                val activityName = resolveInfo.activityInfo.name
                val key = "$packageName/$activityName"
                if (!seen.add(key)) continue

                val label = resolveInfo.loadLabel(packageManager).toString().ifBlank { packageName }
                apps.add(
                    AppEntry(
                        packageName = packageName,
                        activityName = activityName,
                        label = label,
                        icon = resolveInfo.loadIcon(packageManager)
                    )
                )
            }
        }

        return apps.sortedBy { it.label.lowercase() }
    }
}
