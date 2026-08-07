package com.eina.app.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

private const val SHARED_DIR = "shared"

/**
 * Salva il bitmap in cache/shared e ne restituisce l'Uri esposto dal FileProvider.
 * La cartella viene ripulita prima di scrivere: l'immagine e' usa e getta.
 */
fun saveShareImage(context: Context, bitmap: Bitmap, fileName: String): Uri {
    val dir = File(context.cacheDir, SHARED_DIR).apply {
        mkdirs()
        listFiles()?.forEach { it.delete() }
    }
    val file = File(dir, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Chooser di sistema con l'immagine allegata e permesso di lettura temporaneo. */
fun shareImage(context: Context, uri: Uri, text: String? = null) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        text?.let { putExtra(Intent.EXTRA_TEXT, it) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, "Condividi allenamento").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(chooser)
}
