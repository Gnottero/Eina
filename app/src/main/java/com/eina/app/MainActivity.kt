package com.eina.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.eina.app.data.prefs.AppLocale
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.feedback.WorkoutFeedback
import com.eina.app.ui.theme.EinaTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    // The language chosen in Settings is applied here, before any resource is resolved. Changing
    // it recreates the Activity and comes back through this point.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light-only app: transparent system bars with dark icons, even under an OS dark theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            EinaApp()
        }
    }
}

@Composable
private fun EinaApp() {
    val feedback: WorkoutFeedback = koinInject()
    val hapticTap = remember(feedback) { { feedback.haptic() } }

    EinaTheme {
        CompositionLocalProvider(LocalHapticTap provides hapticTap) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                EinaNavHost()
            }
        }
    }
}
