package com.cano.launcher

import android.content.Intent
import android.content.pm.PackageManager

/** Consulta aplicaciones visibles para launcher/TV y elimina componentes duplicados. */
class AppRepository(private val packageManager: PackageManager) {

    fun getLaunchableApps(excludedPackage: String): List<AppEntry> {
        val appsByComponent = linkedMapOf<String, AppEntry>()
        val categories = listOf(
            Intent.CATEGORY_LEANBACK_LAUNCHER,
            Intent.CATEGORY_LAUNCHER
        )

        for (category in categories) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (resolveInfo in packageManager.queryIntentActivities(intent, 0)) {
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == excludedPackage) continue

                val activityName = resolveInfo.activityInfo.name
                val key = "$packageName/$activityName"
                if (key in appsByComponent) continue

                val label = resolveInfo.loadLabel(packageManager).toString().ifBlank { packageName }
                val bannerResource = resolveInfo.activityInfo.banner
                    .takeIf { it != 0 }
                    ?: resolveInfo.activityInfo.applicationInfo.banner
                appsByComponent[key] = AppEntry(
                    packageName = packageName,
                    activityName = activityName,
                    launchCategory = category,
                    label = label,
                    banner = if (bannerResource != 0) {
                        packageManager.getDrawable(
                            packageName,
                            bannerResource,
                            resolveInfo.activityInfo.applicationInfo
                        )
                    } else {
                        null
                    },
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
        }

        return appsByComponent.values.sortedBy { it.label.lowercase() }
    }
}
