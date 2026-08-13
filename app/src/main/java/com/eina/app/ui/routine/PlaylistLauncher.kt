package com.eina.app.ui.routine

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.eina.app.data.db.PlaylistType

private val spotifyPlaylistIdRegex = Regex("""open\.spotify\.com/playlist/([a-zA-Z0-9]+)""")

/** Quanto si aspetta prima del tasto play: il tempo che l'app musicale carichi la playlist. */
private const val PLAYBACK_KEY_DELAY_MS = 1500L

/**
 * Opens linkedPlaylistUri following the deep link rules defined in CLAUDE.md, falling back to the
 * browser, and starts playback.
 *
 * DECISIONE: playback is triggered in two complementary ways, since neither is guaranteed. Spotify
 * accepts a ":play" suffix on the URI, which starts the playlist as it opens; for everything else
 * the PLAY media key is sent (not PLAY_PAUSE, which would pause an ongoing playback) to whichever
 * app has taken the audio session in the meantime.
 */
fun launchPlaylist(context: Context, uri: String, type: PlaylistType): Boolean {
    val intent = when (type) {
        PlaylistType.SPOTIFY -> {
            val playlistId = spotifyPlaylistIdRegex.find(uri)?.groupValues?.get(1)
            if (playlistId != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse("spotify:playlist:$playlistId:play"))
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            }
        }
        PlaylistType.YOUTUBE_MUSIC -> {
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                setPackage("com.google.android.apps.youtube.music")
            }
        }
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (e: ActivityNotFoundException) {
            // Neither the music app nor a browser: report it to the caller instead of crashing.
            return false
        }
    }

    requestPlaybackStart(context)
    return true
}

/** Manda il tasto multimediale PLAY all'app che detiene la sessione audio. */
private fun requestPlaybackStart(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    Handler(Looper.getMainLooper()).postDelayed({
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
    }, PLAYBACK_KEY_DELAY_MS)
}
