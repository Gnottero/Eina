package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.MuscleGroupColors
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing

/**
 * Colori dei superset: bastano a distinguere a colpo d'occhio i giri di uno stesso allenamento.
 * Sono presi dalla palette dei gruppi muscolari, non e' una tavolozza nuova.
 */
private val SupersetColors = listOf(
    MuscleGroupColors.Shoulders,
    MuscleGroupColors.Core,
    MuscleGroupColors.Cardio,
    MuscleGroupColors.Legs,
    MuscleGroupColors.BackPull
)

/** Colore del superset dalla sua lettera: A e' sempre viola, B sempre verde acqua, e cosi' via. */
fun supersetColor(letter: String): Color {
    val index = (letter.firstOrNull() ?: 'A') - 'A'
    return SupersetColors[((index % SupersetColors.size) + SupersetColors.size) % SupersetColors.size]
}

/** Un superset gia' esistente, come lo vede il foglio di scelta. */
data class SupersetOption(
    val group: Int,
    val letter: String,
    val members: List<String>
)

/** Marchio del superset sulla card: lettera del giro, colore del giro. */
@Composable
fun SupersetBadge(letter: String, modifier: Modifier = Modifier) {
    val color = supersetColor(letter)
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), PillShape)
            .padding(horizontal = Spacing.md, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Icon(
            Icons.Outlined.Repeat,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(R.string.superset_badge, letter),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

/**
 * Scelta del superset per un esercizio: un giro nuovo, uno di quelli gia' aperti, o nessuno.
 * L'esercizio che entra in un giro si sposta accanto ai suoi compagni: un superset e' una
 * sequenza, non un insieme sparso per la lista.
 */
@Composable
fun SupersetSheet(
    exerciseName: String,
    current: Int?,
    options: List<SupersetOption>,
    onSelect: (Int?) -> Unit,
    onNewGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    val island = EinaTheme.island
    IslandBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.superset_title)) {
        Text(
            text = stringResource(R.string.superset_description, exerciseName),
            style = MaterialTheme.typography.bodySmall,
            color = island.textSecondary
        )

        options.forEach { option ->
            SupersetRow(
                letter = option.letter,
                label = stringResource(R.string.superset_badge, option.letter),
                description = option.members.joinToString(" · "),
                selected = option.group == current,
                onClick = { onSelect(option.group); onDismiss() }
            )
        }

        SheetActionRow(
            icon = Icons.Outlined.Repeat,
            label = stringResource(R.string.superset_new),
            description = stringResource(R.string.superset_new_description),
            onClick = { onNewGroup(); onDismiss() }
        )

        if (current != null) {
            SheetActionRow(
                icon = Icons.Outlined.LinkOff,
                label = stringResource(R.string.superset_leave),
                onClick = { onSelect(null); onDismiss() }
            )
        }
    }
}

@Composable
private fun SupersetRow(
    letter: String,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    val color = supersetColor(letter)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { hapticTap(); onClick() }
            .background(if (selected) color.copy(alpha = 0.10f) else Color.Transparent)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(PillShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(letter, style = MaterialTheme.typography.titleMedium, color = color)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (description.isNotBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = island.textSecondary
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
