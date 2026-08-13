package com.eina.app.ui.routine

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.eina.app.R
import com.eina.app.data.transfer.RoutineTransfer
import java.io.File

/** Sottocartella dedicata: i file di routine non devono sparire quando si condivide un'immagine. */
private const val ROUTINES_DIR = "shared/routines"

/**
 * Writes the routine to a cache file and opens the system chooser, from where it reaches a
 * messaging app, an email or the file manager. Returns false when no app can receive it.
 */
fun shareRoutineFile(context: Context, fileName: String, json: String): Boolean {
    val dir = File(context.cacheDir, ROUTINES_DIR).apply {
        mkdirs()
        listFiles()?.forEach { it.delete() }
    }
    val file = File(dir, "$fileName.${RoutineTransfer.FILE_EXTENSION}")
    file.writeText(json)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val send = Intent(Intent.ACTION_SEND).apply {
        type = RoutineTransfer.MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, context.getString(R.string.routine_export)).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(chooser) }.isSuccess
}

/** Legge il file scelto dal selettore di sistema. Null se non e' leggibile. */
fun readRoutineFile(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
}.getOrNull()

/**
 * Exchange file name: the routine name stripped of characters file systems reject, so the
 * recipient can tell what they are opening.
 */
fun routineFileName(routineName: String): String =
    routineName.trim()
        .replace(Regex("[^\\p{L}\\p{N} _-]"), "")
        .replace(' ', '-')
        .take(60)
        .ifBlank { "routine" }
