package com.notifrelay.app

import kotlinx.serialization.Serializable

/**
 * A full settings snapshot that can be exported/imported as JSON.
 */
@Serializable
data class SettingsExport(
    val appVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val webhookConfigs: List<WebhookConfig> = emptyList(),
    val forwardMode: String = ForwardMode.ALL.name,
    val selectedPackages: List<String> = emptyList(),
    val ignoreOngoing: Boolean = true,
    val ignoreGroupSummary: Boolean = true,
    val localHttpEnabled: Boolean = false,
    val localHttpPort: Int = 8787
)
