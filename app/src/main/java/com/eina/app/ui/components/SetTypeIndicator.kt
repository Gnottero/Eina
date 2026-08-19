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
import com.eina.app.data.db.SetType
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.MuscleGroupColors
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing

/**
 * Letter marking the set: W warmup, F failure, D drop set. A normal set has no letter and keeps its
 * number. They are initials, not words, so they stay the same in every language and the column can
 * remain 40dp wide.
 */
val SetType.glyph: String?
    get() = when (this) {
        SetType.WARMUP -> "W"
        SetType.NORMAL -> null
        SetType.FAILURE -> "F"
        SetType.DROP -> "D"
    }

/** Marker colour: yellow warms up, red fails, blue drops. A normal set stays secondary text. */
@Composable
fun setTypeAccent(type: SetType): Color = when (type) {
    SetType.WARMUP -> MuscleGroupColors.Arms
    SetType.NORMAL -> EinaTheme.island.textSecondary
    SetType.FAILURE -> DestructiveRed
    SetType.DROP -> MuscleGroupColors.BackPull
}

@Composable
fun setTypeLabel(type: SetType): String = stringResource(
    when (type) {
        SetType.WARMUP -> R.string.set_type_warmup
        SetType.NORMAL -> R.string.set_type_normal
        SetType.FAILURE -> R.string.set_type_failure
        SetType.DROP -> R.string.set_type_drop
    }
)

@Composable
fun setTypeDescription(type: SetType): String = stringResource(
    when (type) {
        SetType.WARMUP -> R.string.set_type_warmup_description
        SetType.NORMAL -> R.string.set_type_normal_description
        SetType.FAILURE -> R.string.set_type_failure_description
        SetType.DROP -> R.string.set_type_drop_description
    }
)

/**
 * Set marker at the head of the row: type letter or number, and "PR" when the set is a record.
 *
 * A record always shows the letters PR: on a failure or drop set the pill used to keep the type
 * letter and only change colour, which said "record" to nobody. The type is not lost — its letter
 * moves under the pill, in its own colour.
 */
@Composable
fun SetTypeIndicator(
    type: SetType,
    number: Int,
    isPR: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val hapticTap = LocalHapticTap.current
    val accent = setTypeAccent(type)
    val text = type.glyph ?: number.toString()
    // The marker is a single letter: without an action label TalkBack would announce "W, double
    // tap to activate" without saying what it activates.
    val clickLabel = stringResource(R.string.set_type_cd)

    Box(
        modifier = modifier
            .clip(PillShape)
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(onClickLabel = clickLabel) { hapticTap(); onClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isPR -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = stringResource(R.string.badge_pr),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, PillShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
                type.glyph?.let { glyph ->
                    Text(
                        text = glyph,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }

            type == SetType.NORMAL -> Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )

            else -> Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                modifier = Modifier
                    .background(accent.copy(alpha = 0.12f), PillShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/** Set type picker sheet, opened by tapping the marker at the head of the row. */
@Composable
fun SetTypeSheet(
    current: SetType,
    onSelect: (SetType) -> Unit,
    onDismiss: () -> Unit
) {
    val hapticTap = LocalHapticTap.current
    IslandBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.set_type_title)) {
        SetType.entries.forEach { type ->
            SetTypeRow(
                type = type,
                selected = type == current,
                onClick = {
                    hapticTap()
                    onSelect(type)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun SetTypeRow(type: SetType, selected: Boolean, onClick: () -> Unit) {
    val island = EinaTheme.island
    val accent = setTypeAccent(type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            .padding(vertical = Spacing.sm, horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // The initial instead of an icon: the same marker that will appear on the set row.
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(PillShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = type.glyph ?: "1",
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(setTypeLabel(type), style = MaterialTheme.typography.titleSmall)
            Text(
                setTypeDescription(type),
                style = MaterialTheme.typography.bodySmall,
                color = island.textSecondary
            )
        }
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
