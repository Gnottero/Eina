package com.eina.app.data.transfer

import android.content.Context
import java.io.File
import java.util.UUID

/**
 * Immagini degli esercizi custom nello storage interno, viste dal formato di scambio.
 *
 * L'esercizio inventato dall'utente porta un `mediaUri` che e' un percorso assoluto nel suo
 * telefono: mandarlo dentro un file di routine non servirebbe a niente. Qui il byte dell'immagine
 * si legge per infilarlo nel JSON e si riscrive all'import, dove diventa un percorso nuovo, locale
 * a chi importa.
 *
 * Gli esercizi di libreria non passano di qui: il loro media e' bundlato negli asset e chi importa
 * ce l'ha gia'.
 */
class ExerciseMediaStore(private val context: Context) {

    /**
     * Byte dell'immagine, o null se il file non esiste, non e' un file locale (un esercizio di
     * libreria punta agli asset) o e' piu' grande di [MAX_MEDIA_BYTES].
     *
     * Il tetto serve a non trasformare una scheda in un allegato da decine di MB: sopra la soglia
     * l'esercizio viaggia lo stesso, senza immagine.
     */
    fun read(mediaUri: String?): ByteArray? {
        val file = localFile(mediaUri) ?: return null
        if (!file.isFile || file.length() > MAX_MEDIA_BYTES) return null
        return runCatching { file.readBytes() }.getOrNull()
    }

    /** Estensione del file, per rimetterla al posto giusto all'import (`gif`, `webp`, `png`…). */
    fun extensionOf(mediaUri: String?): String? =
        localFile(mediaUri)?.extension?.takeIf { it.isNotBlank() }

    /** Scrive l'immagine importata accanto a quelle create sul telefono. Ritorna il percorso. */
    fun write(bytes: ByteArray, extension: String?): String? = runCatching {
        val directory = File(context.filesDir, MEDIA_DIRECTORY).apply { mkdirs() }
        val destination = File(directory, "${UUID.randomUUID()}.${extension?.ifBlank { null } ?: "gif"}")
        destination.writeBytes(bytes)
        destination.absolutePath
    }.getOrNull()

    private fun localFile(mediaUri: String?): File? {
        val path = mediaUri?.takeIf { it.isNotBlank() } ?: return null
        // Solo i file dell'app: un `file:///android_asset/...` di libreria non e' un File leggibile
        // e non ha senso spedirlo.
        if (!path.startsWith("/")) return null
        return File(path)
    }

    companion object {
        const val MEDIA_DIRECTORY = "exercise_media"

        /** 4 MB: una GIF di esercizio ci sta comoda, un video mascherato da GIF no. */
        const val MAX_MEDIA_BYTES = 4L * 1024 * 1024
    }
}
