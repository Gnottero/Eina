package com.eina.app.ui.components

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.eina.app.R
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.TileShape
import kotlinx.coroutines.delay

/**
 * Animazione dell'esercizio.
 *
 * Il catalogo ha due tipi di immagini (vedi ExerciseSeeder.bundledMediaUri):
 *   - `anim.webp`: la figura anatomica che esegue il movimento coi muscoli lavorati colorati,
 *     una WebP animata riprodotta cosi' com'e';
 *   - `0.webp` + `1.webp`: i due fotogrammi fotografici (inizio e fine del movimento) dei pochi
 *     esercizi senza animazione, alternati in dissolvenza.
 *
 * Un esercizio custom ha un'immagine sola (quella scelta dalla galleria) e resta fermo. Senza
 * immagini il composable non disegna nulla: se ne accorge il chiamante con [hasExerciseMedia].
 */
@Composable
fun ExerciseAnimation(
    mediaUri: String?,
    modifier: Modifier = Modifier,
    frameMillis: Long = 900L
) {
    val frames = remember(mediaUri) { exerciseFrames(mediaUri) }
    if (frames.isEmpty()) return

    val animated = isAnimatedMedia(mediaUri)
    // Le animazioni anatomiche sono quadrate e su fondo bianco: riquadro quadrato e superficie
    // dello stesso bianco della card, altrimenti restano due bande di superficie incassata ai
    // lati della figura. Le foto, che riempiono il riquadro, tengono il fondo incassato.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (animated) 1f else 3f / 2f)
            .clip(TileShape)
            .background(if (animated) MaterialTheme.colorScheme.surface else EinaTheme.island.sunken)
    ) {
        if (animated) {
            AnimatedMedia(uri = frames.first(), modifier = Modifier.fillMaxSize())
        } else {
            CrossfadingFrames(
                frames = frames,
                frameMillis = frameMillis,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * WebP animata. I decoder animati si chiedono sulla singola richiesta e non sull'ImageLoader
 * globale, cosi' le miniature della libreria restano ferme sul primo fotogramma senza far
 * girare 197 animazioni in una lista.
 */
@Composable
private fun AnimatedMedia(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(uri) {
        ImageRequest.Builder(context)
            .data(uri)
            .decoderFactory(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoderDecoder.Factory()
                } else {
                    // Sotto API 28 ImageDecoder non c'e': GifDecoder non legge le WebP animate,
                    // che restano quindi sul primo fotogramma. Accettabile su Android 8.
                    GifDecoder.Factory()
                }
            )
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = stringResource(R.string.exercise_animation_cd),
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun CrossfadingFrames(
    frames: List<String>,
    frameMillis: Long,
    modifier: Modifier = Modifier
) {
    var frameIndex by remember(frames) { mutableIntStateOf(0) }
    if (frames.size > 1) {
        LaunchedEffect(frames) {
            while (true) {
                delay(frameMillis)
                frameIndex = (frameIndex + 1) % frames.size
            }
        }
    }
    Crossfade(
        targetState = frames[frameIndex],
        animationSpec = tween(durationMillis = 320),
        label = "exerciseFrame"
    ) { frame ->
        AsyncImage(
            model = frame,
            contentDescription = stringResource(R.string.exercise_animation_cd),
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}

fun hasExerciseMedia(mediaUri: String?): Boolean = exerciseFrames(mediaUri).isNotEmpty()

private fun isAnimatedMedia(mediaUri: String?): Boolean =
    mediaUri != null && mediaUri.startsWith(BUNDLED_MEDIA_PREFIX) && mediaUri.endsWith(ANIMATION_SUFFIX)

/**
 * Fotogrammi da mostrare per un esercizio. Un'animazione (`anim.webp`) e' un elemento solo; per
 * gli esercizi ancora fotografici il catalogo salva in `mediaUri` il primo fotogramma bundlato
 * (`file:///android_asset/media/.../0.webp`) e il secondo si ricava per convenzione dal nome:
 * nessuna colonna in piu' nel database. Un `mediaUri` qualunque (esercizio custom) resta
 * un'immagine sola.
 */
private fun exerciseFrames(mediaUri: String?): List<String> {
    val uri = mediaUri?.takeIf { it.isNotBlank() } ?: return emptyList()
    if (!uri.startsWith(BUNDLED_MEDIA_PREFIX) || !uri.endsWith(FIRST_FRAME_SUFFIX)) return listOf(uri)
    return listOf(uri, uri.removeSuffix(FIRST_FRAME_SUFFIX) + SECOND_FRAME_SUFFIX)
}

const val BUNDLED_MEDIA_PREFIX = "file:///android_asset/media/"
private const val FIRST_FRAME_SUFFIX = "0.webp"
private const val SECOND_FRAME_SUFFIX = "1.webp"
private const val ANIMATION_SUFFIX = "anim.webp"
