package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing

/**
 * Foglio che sale dal basso, in stile island: angoli grandi solo in alto, superficie bianca,
 * niente maniglia Material. Sostituisce i menu a tendina e i dialog dove le voci sono azioni da
 * colpire col pollice, non testo da leggere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val island = EinaTheme.island
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Sempre a tutta altezza: i fogli qui contengono liste e griglie, e lo stato intermedio
        // taglierebbe il contenuto a meta'.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = Spacing.md, bottom = Spacing.xs)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(PillShape)
                    .background(island.sunken)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = Spacing.sm)
                )
            }
            content()
        }
    }
}

/**
 * Riga d'azione di un foglio: icona in una pastiglia, etichetta grande, area di tocco piena.
 * `destructive` la tinge di rosso — resta l'ultima della lista.
 */
@Composable
fun SheetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    destructive: Boolean = false
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    val contentColor = if (destructive) DestructiveRed else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { hapticTap(); onClick() }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (destructive) DestructiveRed.copy(alpha = 0.10f) else island.sunken),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = contentColor)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = island.textSecondary
                )
            }
        }
    }
}

/** Rosso delle azioni distruttive: usato solo qui, non entra nella palette generale. */
val DestructiveRed = Color(0xFFE5484D)
