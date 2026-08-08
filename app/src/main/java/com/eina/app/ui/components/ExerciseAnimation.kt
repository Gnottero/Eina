package com.eina.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.eina.app.R
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.TileShape
import kotlinx.coroutines.delay

/**
 * Animazione dell'esercizio: i due fotogrammi del catalogo (inizio e fine del movimento) si
 * alternano in dissolvenza, come su Hevy. Due immagini statiche invece di una GIF o di una WebP
 * animata perche' cosi' la cadenza la decide la UI e non serve un decoder animato.
 *
 * I fotogrammi stanno in `assets/media/<cartella>/{0,1}.webp` e arrivano dal campo `mediaUri`
 * dell'esercizio; un esercizio custom ne ha uno solo (l'immagine scelta dalla galleria) e resta
 * fermo. Senza immagini il composable non disegna nulla: se ne accorge il chiamante con
 * [hasExerciseMedia].
 */
@Composable
fun ExerciseAnimation(
    mediaUri: String?,
    modifier: Modifier = Modifier,
    frameMillis: Long = 900L
) {
    val frames = remember(mediaUri) { exerciseFrames(mediaUri) }
    if (frames.isEmpty()) return

    var frameIndex by remember(frames) { mutableIntStateOf(0) }
    if (frames.size > 1) {
        LaunchedEffect(frames) {
            while (true) {
                delay(frameMillis)
                frameIndex = (frameIndex + 1) % frames.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 2f)
            .clip(TileShape)
            .background(EinaTheme.island.sunken)
    ) {
        Crossfade(
            targetState = frames[frameIndex],
            animationSpec = tween(durationMillis = 320),
            label = "exerciseFrame"
        ) { frame ->
            AsyncImage(
                model = frame,
                contentDescription = stringResource(R.string.exercise_animation_cd),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun hasExerciseMedia(mediaUri: String?): Boolean = exerciseFrames(mediaUri).isNotEmpty()

/**
 * Fotogrammi da mostrare per un esercizio. Il catalogo salva in `mediaUri` il primo fotogramma
 * bundlato (`file:///android_asset/media/.../0.webp`) e il secondo si ricava per convenzione
 * dal nome: nessuna colonna in piu' nel database. Un `mediaUri` qualunque (esercizio custom)
 * resta un'immagine sola.
 */
private fun exerciseFrames(mediaUri: String?): List<String> {
    val uri = mediaUri?.takeIf { it.isNotBlank() } ?: return emptyList()
    if (!uri.startsWith(BUNDLED_MEDIA_PREFIX) || !uri.endsWith(FIRST_FRAME_SUFFIX)) return listOf(uri)
    return listOf(uri, uri.removeSuffix(FIRST_FRAME_SUFFIX) + SECOND_FRAME_SUFFIX)
}

const val BUNDLED_MEDIA_PREFIX = "file:///android_asset/media/"
private const val FIRST_FRAME_SUFFIX = "0.webp"
private const val SECOND_FRAME_SUFFIX = "1.webp"
