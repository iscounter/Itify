package com.openreachtube.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.openreachtube.app.ui.navigation.OpenReachNavGraph
import com.openreachtube.app.ui.theme.OpenReachTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity Compose host. Mirrors the measured topology: MAIN/LAUNCHER plus
 * BROWSABLE VIEW filters for youtube.com / m.youtube.com / youtu.be deep links.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var pendingDeepLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            OpenReachTheme {
                OpenReachNavGraph(deepLink = pendingDeepLink)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.host?.endsWith("youtube.com") == true || data.host == "youtu.be") {
            pendingDeepLink = data.toString()
        }
    }
}
