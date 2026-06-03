package com.notifrelay.app

import kotlinx.serialization.Serializable

@Serializable
data class WebhookLog(
    val id: String,
    val timestamp: Long,
    val url: String,
    val statusCode: Int?,
    val success: Boolean,
    val errorMessage: String?,
    // Source package of the forwarded notification (e.g. com.whatsapp)
    val sourcePackage: String? = null,
    val responseTimeMs: Long? = null,
    val payload: String? = null
)
