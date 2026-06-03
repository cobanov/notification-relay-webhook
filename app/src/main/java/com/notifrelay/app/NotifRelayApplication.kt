package com.notifrelay.app

import android.app.Application

class NotifRelayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Start the local HTTP server on launch if the user previously enabled it.
        if (PreferencesManager(this).isLocalHttpEnabled()) {
            LocalHttpServerService.start(this)
        }
    }
}
