package com.notifrelay.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

/**
 * Lists installed apps the user can choose to forward (or block). Sorted by label,
 * with the user's own app excluded. System apps without a launcher are still
 * included since many of them post notifications.
 */
object InstalledAppsProvider {

    fun listApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val self = context.packageName
        return pm.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != self }
            .filter { it.enabled }
            .map { appInfo: ApplicationInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
