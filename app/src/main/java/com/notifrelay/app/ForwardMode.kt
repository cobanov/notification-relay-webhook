package com.notifrelay.app

/**
 * Which notifications get forwarded.
 * - ALL: forward every app's notifications.
 * - ALLOWLIST: forward only the apps in the selected set.
 * - BLOCKLIST: forward everything except the apps in the selected set.
 */
enum class ForwardMode {
    ALL, ALLOWLIST, BLOCKLIST;

    val displayName: String
        get() = when (this) {
            ALL -> "All apps"
            ALLOWLIST -> "Allowlist"
            BLOCKLIST -> "Blocklist"
        }

    val description: String
        get() = when (this) {
            ALL -> "Forward notifications from every app"
            ALLOWLIST -> "Forward only the apps you select"
            BLOCKLIST -> "Forward all apps except the ones you select"
        }

    companion object {
        fun fromName(name: String?): ForwardMode =
            entries.firstOrNull { it.name == name } ?: ALL

        /**
         * Whether a notification from [packageName] should be forwarded given the
         * mode and the user's [selectedPackages].
         */
        fun shouldForward(mode: ForwardMode, selectedPackages: Set<String>, packageName: String): Boolean =
            when (mode) {
                ALL -> true
                ALLOWLIST -> packageName in selectedPackages
                BLOCKLIST -> packageName !in selectedPackages
            }
    }
}
