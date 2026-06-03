package com.notifrelay.app

import kotlinx.serialization.Serializable

@Serializable
data class WebhookConfig(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = true
) {
    fun getHeaderCount(): Int = headers.size

    fun withHeader(key: String, value: String): WebhookConfig =
        copy(headers = headers + (key to value))

    fun withoutHeader(key: String): WebhookConfig =
        copy(headers = headers - key)

    companion object {
        fun fromUrl(url: String): WebhookConfig = WebhookConfig(url = url, headers = emptyMap())
    }
}
