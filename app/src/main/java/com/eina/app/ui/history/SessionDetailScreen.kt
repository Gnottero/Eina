package com.eina.app.ui.history

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.WeightType
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.StatTile
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatDuration
import com.eina.app.ui.components.formatFullDate
import com.eina.app.ui.components.formatTime
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.share.ShareCardExercise
import com.eina.app.ui.share.bestSetLabel
import com.eina.app.ui.share.renderShareCard
import com.eina.app.ui.share.saveShareImage
import com.eina.app.ui.share.shareCardDataOf
import com.eina.app.ui.share.shareImage
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = koinViewModel { parametersOf(sessionId) }
) {
    val island = EinaTheme.island
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
                title = summary?.let { formatFullDate(it.startTime) } ?: "Allenamento",
                subtitle = summary?.let { session ->
                    buildString {
                        append(formatTime(session.startTime))
                        session.durationMinutes?.let { append(" · ${formatDuration(it)}") }
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
                                    val data = shareCardDataOf(summary, state.exercises.map { it.toShareCardExercise() })
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

        state.exercises.forEach { exercise ->
            IslandCard(modifier = Modifier.fillMaxWidth()) {
                Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                exercise.sets.forEach { set ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Text(
                            text = "${set.setIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = island.textSecondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = setLabel(set),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (set.isWarmup) {
                            EinaBadge(text = "Riscaldamento", color = island.textSecondary)
                        }
                        if (set.isPR) {
                            EinaBadge(text = "PR", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
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

/** La "serie migliore" e' quella con carico maggiore, a parita' di carico quella con piu' ripetizioni. */
private fun SessionExerciseDetail.toShareCardExercise(): ShareCardExercise {
    val working = sets.filter { !it.isWarmup }
    val best = working.maxWithOrNull(
        compareBy({ it.weight ?: 0.0 }, { it.actualReps ?: 0 })
    )
    return ShareCardExercise(
        name = exerciseName,
        setCount = working.size,
        bestSetLabel = best?.let { bestSetLabel(it.weightType, it.actualReps, it.weight) },
        hasPr = working.any { it.isPR }
    )
}

private fun setLabel(set: CompletedSetRow): String = when (set.weightType) {
    WeightType.TIME_BASED -> "${set.actualReps ?: 0} s"
    WeightType.BODYWEIGHT -> "${set.actualReps ?: 0} rip."
    WeightType.BODYWEIGHT_PLUS_LOAD ->
        "${set.actualReps ?: 0} rip. · +${formatDecimal(set.weight ?: 0.0)} kg"
    WeightType.ASSISTED ->
        "${set.actualReps ?: 0} rip. · -${formatDecimal(set.weight ?: 0.0)} kg"
    else -> "${set.actualReps ?: 0} rip. · ${formatDecimal(set.weight ?: 0.0)} kg"
}
