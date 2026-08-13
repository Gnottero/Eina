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
 * Superset colours, enough to tell the rounds of one workout apart at a glance. Taken from the
 * muscle group palette rather than a new one.
 */
private val SupersetColors = listOf(
    MuscleGroupColors.Shoulders,
    MuscleGroupColors.Core,
    MuscleGroupColors.Cardio,
    MuscleGroupColors.Legs,
    MuscleGroupColors.BackPull
)

/** Superset colour from its letter: A is always purple, B always teal, and so on. */
fun supersetColor(letter: String): Color {
    val index = (letter.firstOrNull() ?: 'A') - 'A'
    return SupersetColors[((index % SupersetColors.size) + SupersetColors.size) % SupersetColors.size]
}

/** An existing superset, as the picker sheet sees it. */
data class SupersetOption(
    val group: Int,
    val letter: String,
    val members: List<String>
)

/** Superset badge on a card: the round's letter in the round's colour. */
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
 * Superset choice for an exercise: a new round, one of the open ones, or none. An exercise joining
 * a round moves next to its members, since a superset is a sequence and not a scattered set.
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
