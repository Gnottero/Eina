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
 * Exercise animation.
 *
 * The catalog holds two kinds of media (see ExerciseSeeder.bundledMediaUri):
 *   - `anim.webp`: the anatomical figure performing the movement with the worked muscles coloured,
 *     an animated WebP played as is;
 *   - `0.webp` + `1.webp`: the two photographic frames (start and end of the movement) of the few
 *     exercises without an animation, cross-faded.
 *
 * A custom exercise has a single still image. With no media the composable draws nothing; callers
 * check with [hasExerciseMedia].
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
    // The anatomical animations are square on a white background, so the frame is square and uses
    // the card white; otherwise two sunken bands would flank the figure. The photos fill their
    // frame and keep the sunken background.
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
 * Animated WebP. The animated decoders are requested per image and not on the global ImageLoader,
 * so the library thumbnails stay on their first frame instead of running 197 animations in a list.
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
                    // ImageDecoder does not exist below API 28, and GifDecoder cannot read animated
                    // WebP, so those stay on their first frame.
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
 * Frames to show for an exercise. An animation (`anim.webp`) is a single entry; for the still
 * photographic exercises the catalog stores the first bundled frame in `mediaUri` and the second
 * is derived from its name by convention, with no extra database column. Any other `mediaUri`
 * (custom exercise) stays a single image.
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
