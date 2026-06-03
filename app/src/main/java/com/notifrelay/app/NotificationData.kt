package com.notifrelay.app

import kotlinx.serialization.Serializable

/**
 * A captured notification, flattened to the fields we forward. [postTime] is epoch ms.
 */
@Serializable
data class NotificationData(
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
    val category: String?,
    val postTime: Long,
    val key: String?,
    val isOngoing: Boolean,
    val isGroupSummary: Boolean
)
