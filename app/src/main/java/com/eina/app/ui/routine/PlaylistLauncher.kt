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
 * Apre linkedPlaylistUri secondo le regole del deep link definite in CLAUDE.md, con fallback al
 * browser, e avvia la riproduzione.
 *
 * DECISIONE: la riproduzione parte in due modi complementari, perche' nessuno dei due e' garantito.
 * Spotify accetta il suffisso ":play" sull'URI, che fa partire la playlist appena aperta; per tutto
 * il resto si manda il tasto multimediale PLAY (non PLAY_PAUSE, che metterebbe in pausa una
 * riproduzione gia' in corso) all'app che nel frattempo ha preso la sessione audio.
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
            // Ne' l'app musicale ne' un browser: si segnala al chiamante invece di crashare.
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
