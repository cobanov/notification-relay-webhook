package com.notifrelay.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Captures every posted notification, applies the user's filter, and forwards the
 * allowed ones to the configured webhooks. Bound automatically by the system once
 * the user grants notification access.
 */
class NotificationRelayService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val prefs = PreferencesManager(applicationContext)

        val isGroupSummary = (notification.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val isOngoing = notification.isOngoing

        if (prefs.getIgnoreOngoing() && isOngoing) return
        if (prefs.getIgnoreGroupSummary() && isGroupSummary) return

        val data = extract(notification, isOngoing, isGroupSummary)

        // Drop notifications with no usable content (e.g. pure group placeholders).
        if (data.title.isNullOrBlank() && data.text.isNullOrBlank() && data.bigText.isNullOrBlank()) return

        RecentNotificationsStore.add(data)

        val json = NotificationPayloadBuilder.build(data)

        val mode = prefs.getForwardMode()
        val selected = prefs.getSelectedPackages()
        if (!ForwardMode.shouldForward(mode, selected, notification.packageName)) {
            prefs.enqueueNotification(
                notification = data,
                payload = json,
                status = NotificationDeliveryStatus.IGNORED,
                error = "Filtered by ${mode.displayName.lowercase()}"
            )
            return
        }

        val queueItem = prefs.enqueueNotification(data, json)
        LocalHttpServerManager.publishPayload(json)

        val configs = prefs.getWebhookConfigs()
        if (configs.none { it.isEnabled }) return

        scope.launch {
            val result = NotificationDelivery.sendPayload(
                context = applicationContext,
                payload = json,
                sourcePackage = data.packageName,
                configs = configs
            )
            prefs.updateNotificationQueueItem(
                id = queueItem.id,
                status = if (result.isSuccess) {
                    NotificationDeliveryStatus.DELIVERED
                } else {
                    NotificationDeliveryStatus.FAILED
                },
                error = result.exceptionOrNull()?.message
            )
        }
    }

    private fun extract(
        sbn: StatusBarNotification,
        isOngoing: Boolean,
        isGroupSummary: Boolean
    ): NotificationData {
        val extras = sbn.notification.extras
        fun str(key: String): String? =
            extras.getCharSequence(key)?.toString()?.takeIf { it.isNotBlank() }

        val appLabel = runCatching {
            val pm = applicationContext.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)

        return NotificationData(
            packageName = sbn.packageName,
            appLabel = appLabel,
            title = str(Notification.EXTRA_TITLE),
            text = str(Notification.EXTRA_TEXT),
            subText = str(Notification.EXTRA_SUB_TEXT),
            bigText = str(Notification.EXTRA_BIG_TEXT),
            category = sbn.notification.category,
            postTime = sbn.postTime,
            key = sbn.key,
            isOngoing = isOngoing,
            isGroupSummary = isGroupSummary
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
