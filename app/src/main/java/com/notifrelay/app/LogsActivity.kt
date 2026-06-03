package com.notifrelay.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.notifrelay.app.screens.LogsScreen
import com.notifrelay.app.ui.theme.NotifRelayTheme

class LogsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotifRelayTheme {
                LogsScreen()
            }
        }
    }
}
