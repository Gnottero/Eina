package com.eina.app.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.eina.app.R

/** Quanto resta a schermo la splash prima di sfumare sul contenuto. */
const val SPLASH_DURATION_MS = 600L
private const val FADE_OUT_MS = 260

/**
 * Il blocco e' alto 164dp con la tessera nei primi 108: centrarlo com'e' porterebbe la tessera
 * 28dp sopra il centro dello schermo, mentre la splash di sistema la mette esattamente al centro.
 * Con questo scarto la tessera non si muove quando la splash di sistema lascia il posto a questa.
 */
private val TILE_OFFSET = 28.dp

/**
 * Splash iniziale: fondo bianco, marchio e nome al centro.
 *
 * Disegna lo stesso vettoriale del windowBackground (@drawable/splash_screen), alla stessa
 * misura e nello stesso punto: il sistema mostra quell'immagine gia' prima che l'app esista e
 * qui la si tiene ferma finche' il contenuto non e' pronto, cosi' l'avvio non ha stacchi.
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
        Image(
            painter = painterResource(R.drawable.ic_eina_splash_mark),
            contentDescription = null,
            modifier = Modifier
                .offset(y = TILE_OFFSET)
                .width(200.dp)
                .height(164.dp)
        )
    }
}
