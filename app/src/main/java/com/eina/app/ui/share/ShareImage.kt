package com.eina.app.ui.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.eina.app.R
import java.io.File
import java.io.FileOutputStream

private const val SHARED_DIR = "shared"

/** Sottocartella in Immagini dove finiscono gli overlay salvati. */
private const val GALLERY_FOLDER = "Eina"

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

/**
 * Copia il bitmap nella galleria, in Immagini/Eina, e ne restituisce l'Uri pubblico.
 *
 * Serve al giro alla Strava: l'overlay va salvato prima, perche' e' dal rullino che Instagram
 * lo ripesca come adesivo sopra la foto di sfondo. PNG e non JPEG: la trasparenza e' il punto.
 * Da Android 10 basta MediaStore; sotto serve scrivere davvero il file nella cartella pubblica,
 * quindi il permesso di scrittura (dichiarato in manifest con maxSdkVersion 28).
 * Ritorna null se il salvataggio non riesce: il chiamante lo dice invece di fingere.
 */
fun saveImageToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? = runCatching {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/$GALLERY_FOLDER"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        } else {
            val folder = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                GALLERY_FOLDER
            ).apply { mkdirs() }
            put(MediaStore.Images.Media.DATA, File(folder, displayName).absolutePath)
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@runCatching null
    resolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    } ?: return@runCatching null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    }
    uri
}.getOrNull()

/** Il permesso di scrittura serve solo fino ad Android 9: dopo ci pensa MediaStore. */
fun needsLegacyStoragePermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

/**
 * Mette l'immagine negli appunti. Instagram, come le altre app di storie, offre "incolla"
 * quando trova un'immagine copiata: e' la scorciatoia per chi non vuole passare dal rullino.
 */
fun copyImageToClipboard(context: Context, uri: Uri, label: String): Boolean = runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return@runCatching false
    val clip = ClipData.newUri(context.contentResolver, label, uri)
    clipboard.setPrimaryClip(clip)
    true
}.getOrDefault(false)

const val INSTAGRAM_PACKAGE = "com.instagram.android"

/** Instagram installato: senza l'app il giro dell'overlay non ha dove finire. */
fun isInstagramInstalled(context: Context): Boolean = runCatching {
    context.packageManager.getPackageInfo(INSTAGRAM_PACKAGE, 0)
}.isSuccess

/**
 * Apre Instagram sulla schermata di creazione storia, cosi' si arriva direttamente al punto
 * in cui si sceglie la foto di sfondo. Se quella schermata non e' raggiungibile si ripiega
 * sull'app in generale; se manca anche quella ritorna false.
 */
fun openInstagramStoryCamera(context: Context): Boolean {
    val storyCamera = Intent(Intent.ACTION_VIEW, Uri.parse("instagram://story-camera"))
        .setPackage(INSTAGRAM_PACKAGE)
    if (runCatching { context.startActivity(storyCamera) }.isSuccess) return true

    val launch = context.packageManager.getLaunchIntentForPackage(INSTAGRAM_PACKAGE) ?: return false
    return runCatching { context.startActivity(launch) }.isSuccess
}

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
    val chooser = Intent.createChooser(send, context.getString(R.string.share_chooser_title)).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(chooser) }
        .onFailure { if (it !is ActivityNotFoundException) throw it }
}
