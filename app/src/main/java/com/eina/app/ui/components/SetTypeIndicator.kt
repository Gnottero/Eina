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
 * Lettera che segna la serie: W riscaldamento, F cedimento, D drop set. Una serie normale non ha
 * lettera, tiene il suo numero progressivo. Sono sigle, non parole: restano uguali in tutte le
 * lingue, come su Hevy, cosi' la colonna resta larga 40dp.
 */
val SetType.glyph: String?
    get() = when (this) {
        SetType.WARMUP -> "W"
        SetType.NORMAL -> null
        SetType.FAILURE -> "F"
        SetType.DROP -> "D"
    }

/** Colore del segno: giallo scalda, rosso cede, blu scarica. Il normale resta testo secondario. */
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
 * Segno della serie in testa alla riga: lettera del tipo o numero progressivo, e "PR" quando la
 * serie e' un record. Una serie di tipo speciale che e' anche record tiene la sua lettera dentro
 * la pastiglia arancio: la lettera dice cosa e' stata, l'arancio dice che e' un record.
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
    val text = type.glyph ?: if (isPR) stringResource(R.string.badge_pr) else number.toString()
    // Il segno e' una sigla di una lettera: senza etichetta sull'azione, TalkBack leggerebbe
    // "W, doppio tocco per attivare" senza dire cosa attiva.
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
            isPR -> Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, PillShape)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )

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

/** Foglio di scelta del tipo di serie: si apre toccando il segno in testa alla riga. */
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
        // La sigla al posto dell'icona: e' lo stesso segno che comparira' nella riga della serie.
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
