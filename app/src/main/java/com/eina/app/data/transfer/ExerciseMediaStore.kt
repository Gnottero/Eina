package com.eina.app.data.transfer

import android.content.Context
import java.io.File
import java.util.UUID

/**
 * Custom exercise images in internal storage, as seen by the exchange format.
 *
 * A user-created exercise carries a `mediaUri` that is an absolute path on their phone, which means
 * nothing inside a routine file. Here the image bytes are read to be embedded in the JSON and
 * written back on import, where they become a new local path.
 *
 * Library exercises never pass through: their media is bundled in the assets.
 */
class ExerciseMediaStore(private val context: Context) {

    /**
     * Image bytes, or null when the file does not exist, is not local (a library exercise points at
     * the assets) or exceeds [MAX_MEDIA_BYTES].
     *
     * The cap keeps a routine from becoming a tens-of-megabytes attachment: above it the exercise
     * still travels, without its image.
     */
    fun read(mediaUri: String?): ByteArray? {
        val file = localFile(mediaUri) ?: return null
        if (!file.isFile || file.length() > MAX_MEDIA_BYTES) return null
        return runCatching { file.readBytes() }.getOrNull()
    }

    /** File extension, to restore it on import (`gif`, `webp`, `png`…). */
    fun extensionOf(mediaUri: String?): String? =
        localFile(mediaUri)?.extension?.takeIf { it.isNotBlank() }

    /** Writes an imported image next to the ones created on this phone and returns its path. */
    fun write(bytes: ByteArray, extension: String?): String? = runCatching {
        val directory = File(context.filesDir, MEDIA_DIRECTORY).apply { mkdirs() }
        val destination = File(directory, "${UUID.randomUUID()}.${extension?.ifBlank { null } ?: "gif"}")
        destination.writeBytes(bytes)
        destination.absolutePath
    }.getOrNull()

    private fun localFile(mediaUri: String?): File? {
        val path = mediaUri?.takeIf { it.isNotBlank() } ?: return null
        // App files only: a library `file:///android_asset/...` is not a readable File and there is
        // no point in sending it.
        if (!path.startsWith("/")) return null
        return File(path)
    }

    companion object {
        const val MEDIA_DIRECTORY = "exercise_media"

        /** 4 MB: an exercise GIF fits comfortably, a video disguised as one does not. */
        const val MAX_MEDIA_BYTES = 4L * 1024 * 1024
    }
}
