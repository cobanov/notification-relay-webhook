package com.notifrelay.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory ring buffer of the most recently captured notifications. Feeds the live
 * "Recent" screen (via [recent]) and the local HTTP server. Not persisted; cleared
 * when the process dies.
 */
object RecentNotificationsStore {
    private const val MAX = 300

    private val _recent = MutableStateFlow<List<NotificationData>>(emptyList())
    val recent: StateFlow<List<NotificationData>> = _recent.asStateFlow()

    @Synchronized
    fun add(item: NotificationData) {
        _recent.value = (listOf(item) + _recent.value).take(MAX)
    }

    @Synchronized
    fun clear() {
        _recent.value = emptyList()
    }

    fun snapshot(): List<NotificationData> = _recent.value
}
