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

/** Sub-folder of Pictures where saved overlays land. */
private const val GALLERY_FOLDER = "Eina"

/**
 * Saves the bitmap in cache/shared and returns the Uri exposed by the FileProvider. The folder is
 * emptied before writing: the image is single-use.
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
 * Copies the bitmap to the gallery, under Pictures/Eina, and returns its public Uri.
 *
 * The overlay must be saved first, because Instagram picks it from the gallery as a sticker over
 * the background photo. PNG and not JPEG: transparency is the point. From Android 10 on MediaStore
 * is enough; below that the file must really be written to the public folder, hence the write
 * permission (declared in the manifest with maxSdkVersion 28).
 *
 * Returns null when the save fails, so the caller can say so instead of pretending.
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

/** The write permission is only needed up to Android 9; after that MediaStore handles it. */
fun needsLegacyStoragePermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

/**
 * Puts the image on the clipboard. Instagram, like other story apps, offers "paste" when it finds a
 * copied image: the shortcut for users who would rather skip the gallery.
 */
fun copyImageToClipboard(context: Context, uri: Uri, label: String): Boolean = runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return@runCatching false
    val clip = ClipData.newUri(context.contentResolver, label, uri)
    clipboard.setPrimaryClip(clip)
    true
}.getOrDefault(false)

const val INSTAGRAM_PACKAGE = "com.instagram.android"

/** Whether Instagram is installed; without it the overlay flow has nowhere to go. */
fun isInstagramInstalled(context: Context): Boolean = runCatching {
    context.packageManager.getPackageInfo(INSTAGRAM_PACKAGE, 0)
}.isSuccess

/**
 * Opens Instagram on the story camera, right where the background photo is chosen. If that screen
 * is unreachable it falls back to the app itself, and returns false when even that is missing.
 */
fun openInstagramStoryCamera(context: Context): Boolean {
    val storyCamera = Intent(Intent.ACTION_VIEW, Uri.parse("instagram://story-camera"))
        .setPackage(INSTAGRAM_PACKAGE)
    if (runCatching { context.startActivity(storyCamera) }.isSuccess) return true

    val launch = context.packageManager.getLaunchIntentForPackage(INSTAGRAM_PACKAGE) ?: return false
    return runCatching { context.startActivity(launch) }.isSuccess
}

/** System chooser with the image attached and a temporary read grant. */
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
