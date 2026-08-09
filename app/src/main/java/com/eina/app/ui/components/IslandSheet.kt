package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.squircle

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
    // Da attivare quando il contenuto puo' superare lo schermo (un calendario che si apre, per
    // dire): senza, la Column comprime i figli ad altezza fissa e i rulli di durata finiscono
    // scollati dalla loro banda di selezione.
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
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
        // Nel foglio i bottoni non sono pastiglie: vedi LocalButtonShape.
        CompositionLocalProvider(LocalButtonShape provides SheetButtonShape) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
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
}

/** Squircle a raggio piccolo: la forma dei tasti larghi di un foglio. */
val SheetButtonShape: Shape = squircle(16.dp)

/** Squircle delle righe d'azione: piu' generoso, sono tessere non tasti. */
private val SheetRowShape: Shape = squircle(20.dp)

/**
 * Riga d'azione di un foglio: tessera piena a tutta larghezza, icona in testa ed etichetta.
 * `destructive` la tinge di rosso — resta l'ultima della lista.
 *
 * La riga e' la tessera stessa (fondo incassato, angolo continuo) invece di un'icona in
 * pastiglia su fondo bianco: cosi' l'area toccabile si vede, che e' il punto di un menu che si
 * usa col pollice.
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
    val accent = MaterialTheme.colorScheme.primary
    val labelColor = if (destructive) DestructiveRed else MaterialTheme.colorScheme.onSurface
    // L'icona porta il colore, l'etichetta resta nera: tingere anche il testo faceva sembrare
    // ogni voce un avviso. La voce distruttiva e' l'eccezione, e deve saltare all'occhio.
    val iconColor = if (destructive) DestructiveRed else accent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SheetRowShape)
            .background(if (destructive) DestructiveRed.copy(alpha = 0.08f) else island.sunken)
            .clickable { hapticTap(); onClick() }
            .defaultMinSize(minHeight = 60.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = labelColor)
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

/**
 * Conferma di un'azione: titolo, una riga di spiegazione e due tasti larghi uguali.
 *
 * Sostituisce l'AlertDialog Material, che allineava a destra due scritte senza sfondo: da
 * toccare erano due bersagli piccoli in un angolo, e quale delle due fosse l'azione pericolosa
 * lo diceva solo il colore del testo. Qui i tasti sono pieni, alti quanto quelli dei fogli e
 * della stessa forma (vedi [SheetButtonShape]).
 */
@Composable
fun IslandAlertDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    val island = EinaTheme.island
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(IslandShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = island.textSecondary)
            Row(
                modifier = Modifier.padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                IslandSecondaryButton(
                    text = dismissLabel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = SheetButtonShape
                )
                IslandButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    // Il rosso resta pieno: e' l'azione da cui non si torna indietro.
                    containerColor = if (destructive) DestructiveRed else MaterialTheme.colorScheme.primary,
                    shape = SheetButtonShape
                )
            }
        }
    }
}

/** Rosso delle azioni distruttive: usato solo qui, non entra nella palette generale. */
val DestructiveRed = Color(0xFFE5484D)
