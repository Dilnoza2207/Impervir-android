package com.impervir.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.impervir.agent.navigation.ImpervirRoot
import com.impervir.agent.ui.theme.ImpervirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repository = (application as ImpervirApp).repository
        setContent {
            ImpervirTheme {
                ImpervirRoot(repository = repository)
            }
        }
    }
}
