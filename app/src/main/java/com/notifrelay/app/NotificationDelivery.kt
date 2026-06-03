package com.notifrelay.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

object NotificationDelivery {
    suspend fun sendPayload(
        context: Context,
        payload: String,
        sourcePackage: String?,
        configs: List<WebhookConfig> = PreferencesManager(context).getWebhookConfigs()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        WebhookManager(
            webhookConfigs = configs,
            context = context.applicationContext,
            sourcePackage = sourcePackage,
            payload = payload
        ).postData(payload)
    }

    fun buildTestPayload(): String {
        val now = Instant.now().toString()
        return """
            {"package":"com.notifrelay.app","packageName":"com.notifrelay.app","app":"Notification Relay","appName":"Notification Relay","title":"Test notification","text":"This is a test webhook delivery.","sub_text":null,"big_text":null,"category":"status","post_time":"$now","postedAt":"$now","key":"test-$now","notificationKey":"test-$now","ongoing":false,"group_summary":false,"test":true}
        """.trimIndent()
    }
}
