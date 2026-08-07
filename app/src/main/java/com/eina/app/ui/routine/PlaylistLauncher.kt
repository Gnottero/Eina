package com.eina.app.ui.routine

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.eina.app.data.db.PlaylistType

private val spotifyPlaylistIdRegex = Regex("""open\.spotify\.com/playlist/([a-zA-Z0-9]+)""")

/** Apre linkedPlaylistUri secondo le regole del deep link definite in CLAUDE.md, con fallback al browser. */
fun launchPlaylist(context: Context, uri: String, type: PlaylistType) {
    val intent = when (type) {
        PlaylistType.SPOTIFY -> {
            val playlistId = spotifyPlaylistIdRegex.find(uri)?.groupValues?.get(1)
            if (playlistId != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse("spotify:playlist:$playlistId"))
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
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }
}
