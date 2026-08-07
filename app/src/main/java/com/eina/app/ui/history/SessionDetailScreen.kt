package com.eina.app.ui.history

import android.graphics.Bitmap
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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.WeightType
import com.eina.app.domain.totalVolume
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.StatTile
import com.eina.app.ui.components.formatDayMonth
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatDuration
import com.eina.app.ui.components.formatFullDate
import com.eina.app.ui.components.formatTime
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.share.renderShareCard
import com.eina.app.ui.share.saveShareImage
import com.eina.app.ui.share.shareCardDataOf
import com.eina.app.ui.share.shareImage
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    var shareBitmap by remember { mutableStateOf<Bitmap?>(null) }

    shareBitmap?.let { bitmap ->
        SharePreviewDialog(
            bitmap = bitmap,
            onDismiss = { shareBitmap = null },
            onShare = {
                val uri = saveShareImage(context, bitmap, "eina-allenamento-$sessionId.png")
                shareImage(context, uri, text = "Allenamento registrato con Eina")
                shareBitmap = null
            }
        )
    }

    IslandScreen(
        header = {
            ScreenHeader(
                // Titolo corto: "Allenamento completato" andava a capo e finiva sotto il tasto indietro.
                title = if (justFinished) {
                    "Completato"
                } else {
                    summary?.let { formatFullDate(it.startTime) } ?: "Allenamento"
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
                            contentDescription = "Condividi allenamento",
                            onClick = {
                                scope.launch {
                                    val data = shareCardDataOf(summary)
                                    shareBitmap = withContext(Dispatchers.Default) { renderShareCard(context, data) }
                                }
                            }
                        )
                    }
                }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (summary == null) {
            IslandEmptyState(
                title = "Sessione senza serie completate",
                description = "Questo allenamento non ha serie registrate.",
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
                label = "Volume",
                value = formatVolume(summary.volumeKg),
                unit = "kg",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Serie",
                value = summary.setCount.toString(),
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
                    text = if (weeks == 1) "1 settimana di fila" else "$weeks settimane di fila",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = if (weeks <= 1) {
                        "Lo streak parte da qui: allenati anche la prossima settimana."
                    } else {
                        "Streak in corso. Torna la prossima settimana per non perderlo."
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
    val working = exercise.workingSets
    val volume = totalVolume(exercise.sets)

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
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(if (working.size == 1) "1 serie" else "${working.size} serie")
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
            exercise.sets.forEachIndexed { index, set ->
                SetRow(number = index + 1, set = set)
            }
        }
    }
}

/** Riga serie: numero progressivo, valori allineati, badge solo quando dicono qualcosa. */
@Composable
private fun SetRow(number: Int, set: CompletedSetRow) {
    val island = EinaTheme.island
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Stesso stile e stessa linea di base del valore: con labelMedium dentro una size fissa
        // il numero risultava piccolo e disallineato rispetto a "kg × rip".
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = island.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = setLabel(set),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (set.isWarmup) {
            EinaBadge(text = "Risc.", color = island.textSecondary)
        }
        if (set.isPR) {
            EinaBadge(text = "PR", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SharePreviewDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Anteprima", style = MaterialTheme.typography.titleMedium)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Riepilogo allenamento",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(IslandShape)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                IslandSecondaryButton(
                    text = "Annulla",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                IslandButton(
                    text = "Condividi",
                    onClick = onShare,
                    icon = Icons.Outlined.Share,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun setLabel(set: CompletedSetRow): String = when (set.weightType) {
    WeightType.TIME_BASED -> "${set.actualReps ?: 0} s"
    WeightType.BODYWEIGHT -> "${set.actualReps ?: 0} rip."
    WeightType.BODYWEIGHT_PLUS_LOAD ->
        "+${formatDecimal(set.weight ?: 0.0)} kg × ${set.actualReps ?: 0}"
    WeightType.ASSISTED ->
        "-${formatDecimal(set.weight ?: 0.0)} kg × ${set.actualReps ?: 0}"
    else -> "${formatDecimal(set.weight ?: 0.0)} kg × ${set.actualReps ?: 0}"
}
