package com.notifrelay.app

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationDeliveryStatus {
    PENDING,
    DELIVERED,
    FAILED,
    IGNORED
}

@Serializable
data class NotificationQueueItem(
    val id: String,
    val createdAt: Long,
    val notification: NotificationData,
    val payload: String,
    val status: NotificationDeliveryStatus = NotificationDeliveryStatus.PENDING,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val deliveredAt: Long? = null,
    val lastError: String? = null
)
