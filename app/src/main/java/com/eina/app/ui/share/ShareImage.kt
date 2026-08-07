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

const val INSTAGRAM_PACKAGE = "com.instagram.android"

/** Instagram installato: senza l'app la condivisione come sticker non esiste come opzione. */
fun isInstagramInstalled(context: Context): Boolean = runCatching {
    context.packageManager.getPackageInfo(INSTAGRAM_PACKAGE, 0)
}.isSuccess

/**
 * Condivide la card come *sticker* di una storia Instagram invece che come immagine di sfondo:
 * con ACTION_SEND normale Instagram usa il PNG come sfondo e lo scala a tutto il canvas, qui
 * l'immagine resta un adesivo ridimensionabile sopra lo sfondo (in tinta con l'accento).
 * Ritorna false se l'intent non e' gestibile: il chiamante ricade sul chooser di sistema.
 */
fun shareToInstagramStory(
    context: Context,
    stickerUri: Uri,
    topColor: String = "#F97348",
    bottomColor: String = "#FFB07A"
): Boolean {
    val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        setPackage(INSTAGRAM_PACKAGE)
        type = "image/png"
        putExtra("interactive_asset_uri", stickerUri)
        putExtra("top_background_color", topColor)
        putExtra("bottom_background_color", bottomColor)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (intent.resolveActivity(context.packageManager) == null) return false
    // Lo sticker viaggia come extra, non come EXTRA_STREAM: il permesso va concesso a mano.
    context.grantUriPermission(INSTAGRAM_PACKAGE, stickerUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return runCatching { context.startActivity(intent) }.isSuccess
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
