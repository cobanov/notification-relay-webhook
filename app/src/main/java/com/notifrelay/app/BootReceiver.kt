package com.notifrelay.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts the local HTTP server after the device reboots or the app is updated,
 * but only if the user had it enabled. The NotificationListenerService is rebound
 * by the system automatically, so it needs no handling here.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            if (PreferencesManager(context).isLocalHttpEnabled()) {
                LocalHttpServerService.start(context)
            }
        }
    }
}
