package com.eina.app.ui.history

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.eina.app.R
import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.SetType
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.countsAsWorking
import com.eina.app.domain.totalVolume
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.LocalButtonShape
import com.eina.app.ui.components.SheetButtonShape
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SetTypeIndicator
import com.eina.app.ui.components.setTypeAccent
import com.eina.app.ui.components.setTypeLabel
import com.eina.app.ui.components.StatTile
import com.eina.app.ui.components.formatDayMonth
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatDistanceAndTime
import com.eina.app.ui.components.formatDuration
import com.eina.app.ui.components.formatFullDate
import com.eina.app.ui.components.formatTime
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.library.localized
import com.eina.app.ui.share.ShareCardData
import com.eina.app.ui.share.copyImageToClipboard
import com.eina.app.ui.share.isInstagramInstalled
import com.eina.app.ui.share.needsLegacyStoragePermission
import com.eina.app.ui.share.openInstagramStoryCamera
import com.eina.app.ui.share.renderShareCard
import com.eina.app.ui.share.saveImageToGallery
import com.eina.app.ui.share.saveShareImage
import com.eina.app.ui.share.shareCardDataOf
import com.eina.app.ui.share.shareImage
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.MetricColors
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    justFinished: Boolean = false,
    viewModel: SessionDetailViewModel = koinViewModel { parametersOf(sessionId) }
) {
    val state by viewModel.uiState.collectAsState()
    val summary = state.summary
    val context = LocalContext.current
    var shareData by remember { mutableStateOf<ShareCardData?>(null) }

    shareData?.let { data ->
        ShareSheet(
            data = data,
            fileName = "eina-allenamento-$sessionId.png",
            onDismiss = { shareData = null }
        )
    }

    IslandScreen(
        header = {
            ScreenHeader(
                // Titolo corto: "Allenamento completato" andava a capo e finiva sotto il tasto indietro.
                title = if (justFinished) {
                    stringResource(R.string.session_completed)
                } else {
                    summary?.let { formatFullDate(it.startTime) } ?: stringResource(R.string.session_fallback_title)
                },
                subtitle = summary?.let { session ->
                    buildString {
                        // Data breve a fine allenamento: con la forma estesa il sottotitolo
                        // andava a capo e la durata finiva da sola sulla seconda riga.
                        if (justFinished) append("${formatDayMonth(session.startTime)} · ")
                        append(formatTime(session.startTime))
                        session.durationMinutes?.let { append(" · ${formatDuration(it)}") }
                        session.routineName?.let { append(" · $it") }
                    }
                },
                onBack = onBack,
                trailing = if (summary == null) null else {
                    {
                        IslandIconButton(
                            icon = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.session_share_cd),
                            onClick = { shareData = shareCardDataOf(summary) }
                        )
                    }
                }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (summary == null) {
            IslandEmptyState(
                title = stringResource(R.string.session_empty_title),
                description = stringResource(R.string.session_empty_description),
                icon = Icons.Outlined.History
            )
            return@IslandScreen
        }

        // A fine allenamento lo streak viene prima di tutto: e' il numero che fa tornare.
        if (justFinished) {
            StreakCard(weeks = state.streakWeeks)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = stringResource(R.string.stat_volume),
                value = formatVolume(summary.volumeKg),
                unit = stringResource(R.string.unit_kg),
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stat_sets),
                value = summary.setCount.toString(),
                tint = MetricColors.Sets,
                modifier = Modifier.weight(1f)
            )
        }

        state.exercises.forEachIndexed { index, exercise ->
            ExerciseSummaryCard(position = index + 1, exercise = exercise)
        }
    }
}

/** Streak di settimane consecutive con almeno un allenamento, in evidenza a fine allenamento. */
@Composable
private fun StreakCard(weeks: Int) {
    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = pluralStringResource(R.plurals.session_streak_weeks, weeks, weeks),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = if (weeks <= 1) {
                        stringResource(R.string.session_streak_start)
                    } else {
                        stringResource(R.string.session_streak_ongoing)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * Un blocco di lavoro della sessione: intestazione con posizione, nome e totali, poi la tabella
 * delle serie su superficie incassata. La chiave e' il workoutExerciseId, quindi lo stesso
 * esercizio ripetuto nella stessa sessione compare due volte, con i suoi numeri separati.
 */
@Composable
private fun ExerciseSummaryCard(position: Int, exercise: SessionExerciseDetail) {
    val island = EinaTheme.island
    // workingSets e totalVolume riscorrono la lista a ogni chiamata: le serie di un allenamento
    // gia' registrato non cambiano piu', quindi si calcolano una volta sola.
    val working = remember(exercise) { exercise.workingSets }
    val volume = remember(exercise) { totalVolume(exercise.sets) }

    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(island.sunken),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = position.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = island.textSecondary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exerciseName.localized(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(pluralStringResource(R.plurals.set_count, working.size, working.size))
                        if (volume > 0.0) append(" · ${formatVolume(volume)} kg")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = island.textSecondary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TileShape)
                .background(island.sunken)
                .padding(vertical = Spacing.xs)
        ) {
            // Come in sessione: il numero segue le sole serie di lavoro, i riscaldamenti portano W.
            var workingNumber = 0
            exercise.sets.forEach { set ->
                if (set.setType.countsAsWorking) workingNumber++
                SetRow(number = workingNumber, set = set)
            }
        }
    }
}

/** Riga serie: numero progressivo, valori allineati, badge solo quando dicono qualcosa. */
@Composable
private fun SetRow(number: Int, set: CompletedSetRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Numero o sigla del tipo, lo stesso segno della tabella in sessione.
        SetTypeIndicator(
            type = set.setType,
            number = number,
            isPR = false,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = LocalContext.current.setLabel(set),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (set.setType != SetType.NORMAL) {
            EinaBadge(text = setTypeLabel(set.setType), color = setTypeAccent(set.setType))
        }
        if (set.isPR) {
            EinaBadge(text = stringResource(R.string.badge_pr), color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Foglio di condivisione: una sola immagine, con l'interruttore dello sfondo trasparente.
 *
 * Con lo sfondo, l'immagine si regge da sola su qualsiasi supporto. Senza, resta il solo
 * testo: si salva nel rullino (e in parallelo finisce negli appunti), poi si apre Instagram,
 * si sceglie la propria foto di sfondo e lo si aggiunge come adesivo. E' il giro che fa
 * Strava, e resta l'unico modo di comporre foto propria + statistiche: l'intent ADD_TO_STORY
 * di Instagram accetta un adesivo ma impone lui lo sfondo.
 */
@Composable
private fun ShareSheet(
    data: ShareCardData,
    fileName: String,
    onDismiss: () -> Unit
) {
    val island = EinaTheme.island
    val context = LocalContext.current
    val hapticTap = LocalHapticTap.current
    var transparent by remember { mutableStateOf(true) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(transparent) {
        bitmap = withContext(Dispatchers.Default) { renderShareCard(context, data, transparent) }
    }

    // Fino ad Android 9 scrivere nel rullino richiede un permesso esplicito; concesso,
    // il salvataggio riparte da solo.
    var pendingSave by remember { mutableStateOf(false) }
    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val current = bitmap
        if (granted && pendingSave && current != null) {
            context.saveOverlay(current, fileName, openInstagram = true)
            onDismiss()
        } else if (!granted) {
            context.toast(R.string.share_permission_needed)
        }
        pendingSave = false
    }

    fun saveAndOpen() {
        val current = bitmap ?: return
        if (needsLegacyStoragePermission() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingSave = true
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        context.saveOverlay(current, fileName, openInstagram = true)
        onDismiss()
    }

    // Anche questo e' un menu flottante: i tasti prendono la forma dei fogli, non la pastiglia.
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalButtonShape provides SheetButtonShape) {
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleMedium)

            // Fondo scuro dietro l'anteprima: senza sfondo l'immagine e' testo bianco sul
            // nulla, su carta chiara non si vedrebbe affatto.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(IslandShape)
                    .background(if (transparent) PreviewBackdrop else island.sunken),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = stringResource(R.string.session_preview_cd),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    stringResource(R.string.share_transparent_toggle),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = transparent,
                    onCheckedChange = { hapticTap(); transparent = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = island.sunken,
                        uncheckedBorderColor = island.outlineSubtle
                    )
                )
            }

            if (transparent) {
                Text(
                    stringResource(R.string.share_transparent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = island.textSecondary
                )
            }

            if (isInstagramInstalled(context)) {
                IslandButton(
                    text = stringResource(R.string.share_save_and_open),
                    onClick = { saveAndOpen() },
                    icon = Icons.Outlined.AutoAwesome,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                IslandSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                IslandSecondaryButton(
                    text = stringResource(R.string.action_more),
                    onClick = {
                        val current = bitmap ?: return@IslandSecondaryButton
                        val uri = saveShareImage(context, current, fileName)
                        shareImage(context, uri, text = context.getString(R.string.session_share_text))
                        onDismiss()
                    },
                    icon = Icons.Outlined.Share,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        }
    }
}

/** Grigio scuro neutro: sta al posto della foto che l'utente mettera' sotto l'overlay. */
private val PreviewBackdrop = Color(0xFF2B2B2B)

/**
 * Salva l'overlay nel rullino e lo mette anche negli appunti, cosi' funzionano entrambe le
 * strade dentro Instagram: prenderlo dalla galleria come adesivo, oppure incollarlo.
 */
private fun Context.saveOverlay(bitmap: Bitmap, fileName: String, openInstagram: Boolean) {
    val galleryUri = saveImageToGallery(this, bitmap, fileName)
    if (galleryUri == null) {
        toast(R.string.share_save_failed)
        return
    }
    // La copia negli appunti passa dal FileProvider: l'Uri di MediaStore non e' leggibile
    // da un'altra app senza un permesso esplicito.
    val copied = copyImageToClipboard(this, saveShareImage(this, bitmap, fileName), getString(R.string.app_name))
    toast(if (copied) R.string.share_copied else R.string.share_saved)

    if (openInstagram && !openInstagramStoryCamera(this)) {
        toast(R.string.share_instagram_missing)
    }
}

private fun Context.toast(@StringRes message: Int) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

private fun Context.setLabel(set: CompletedSetRow): String = when (set.weightType) {
    WeightType.TIME_BASED -> "${set.actualReps ?: 0} s"
    WeightType.DISTANCE_BASED -> formatDistanceAndTime(set.weight, set.actualReps)
    WeightType.BODYWEIGHT -> getString(R.string.unit_reps_value, set.actualReps ?: 0)
    WeightType.BODYWEIGHT_PLUS_LOAD ->
        "+${formatDecimal(set.weight ?: 0.0)} kg × ${set.actualReps ?: 0}"
    WeightType.ASSISTED ->
        "-${formatDecimal(set.weight ?: 0.0)} kg × ${set.actualReps ?: 0}"
    else -> "${formatDecimal(set.weight ?: 0.0)} kg × ${set.actualReps ?: 0}"
}
