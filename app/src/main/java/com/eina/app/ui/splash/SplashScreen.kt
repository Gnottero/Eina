package com.eina.app.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.eina.app.ui.components.EinaLogo
import com.eina.app.ui.theme.EinaFontFamily
import com.eina.app.ui.theme.LightOnBackground
import com.eina.app.ui.theme.Spacing

/** Quanto resta a schermo la splash prima di sfumare sul contenuto. */
const val SPLASH_DURATION_MS = 750L
private const val FADE_OUT_MS = 260

/**
 * Splash iniziale: fondo bianco pieno, marchio al centro e il nome sotto in maiuscolo spaziato.
 * Sta sopra il contenuto (non lo sostituisce), cosi' l'app sotto e' gia' composta quando sfuma.
 */
@Composable
fun SplashOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = FADE_OUT_MS, easing = LinearEasing),
        label = "splashAlpha"
    )
    if (alpha == 0f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            EinaLogo(size = 104.dp)
            Text(
                text = "EINA",
                fontFamily = EinaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = 8.sp,
                color = LightOnBackground
            )
        }
    }
}
