package com.eina.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eina.app.data.prefs.AppLocale
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.feedback.WorkoutFeedback
import com.eina.app.ui.splash.SPLASH_DURATION_MS
import com.eina.app.ui.splash.SplashOverlay
import com.eina.app.ui.theme.EinaTheme
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    // La lingua scelta in Impostazioni si applica qui, prima che vengano risolte le risorse.
    // Cambiarla ricrea l'Activity e ripassa da questo punto.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App solo in light mode: barre di sistema trasparenti con icone scure, anche se
        // il sistema e' in tema scuro.
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
    // La splash copre il contenuto per il tempo che serve a comporlo, poi sfuma.
    var splashVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        splashVisible = false
    }

    EinaTheme {
        CompositionLocalProvider(LocalHapticTap provides hapticTap) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EinaNavHost()
                }
                SplashOverlay(visible = splashVisible)
            }
        }
    }
}
