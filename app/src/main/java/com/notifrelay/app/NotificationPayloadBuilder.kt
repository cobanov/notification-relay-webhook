package com.notifrelay.app

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Serializes a [NotificationData] into the JSON payload sent to webhooks and the
 * local server. One notification per object; null fields are emitted as JSON null
 * (the kotlinx.serialization String? overload handles that automatically).
 */
object NotificationPayloadBuilder {

    fun build(data: NotificationData): String = buildJsonObject {
        put("package", data.packageName)
        put("packageName", data.packageName)
        put("app", data.appLabel)
        put("appName", data.appLabel)
        put("title", data.title)
        put("text", data.text)
        put("sub_text", data.subText)
        put("big_text", data.bigText)
        put("category", data.category)
        val postedAt = Instant.ofEpochMilli(data.postTime).toString()
        put("post_time", postedAt)
        put("postedAt", postedAt)
        put("key", data.key)
        put("notificationKey", data.key)
        put("ongoing", data.isOngoing)
        put("group_summary", data.isGroupSummary)
    }.toString()
}
