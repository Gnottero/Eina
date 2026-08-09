package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing

/**
 * Forma dei bottoni nel contesto corrente. In pagina restano pastiglie; dentro un foglio
 * (vedi IslandBottomSheet) diventano squircle a raggio piccolo, che e' la forma dei bottoni
 * larghi di un foglio di sistema — una pastiglia alta 52dp larga tutto lo schermo si legge
 * come un'etichetta, non come un tasto.
 */
val LocalButtonShape = compositionLocalOf<Shape> { PillShape }

/** Azione primaria: piena accento, pill in pagina e squircle nei fogli. */
@Composable
fun IslandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    shape: Shape = LocalButtonShape.current
) {
    val hapticTap = LocalHapticTap.current
    val accent = MaterialTheme.colorScheme.primary
    // L'azione primaria porta la rampa e una sua ombra colorata: e' l'unico elemento della
    // pagina che deve chiamare il tocco, e un arancio piatto non lo faceva.
    val ramped = enabled && containerColor == accent
    val fill = EinaTheme.island.accentRamp
    Button(
        onClick = { hapticTap(); onClick() },
        enabled = enabled,
        shape = shape,
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .then(
                if (ramped) {
                    Modifier
                        .shadow(
                            elevation = 14.dp,
                            shape = shape,
                            clip = false,
                            ambientColor = accent.copy(alpha = 0.35f),
                            spotColor = accent.copy(alpha = 0.45f)
                        )
                        .clip(shape)
                        .background(Brush.horizontalGradient(fill))
                } else {
                    Modifier
                }
            ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (ramped) Color.Transparent else containerColor,
            contentColor = contentColor,
            disabledContainerColor = EinaTheme.island.sunken,
            disabledContentColor = EinaTheme.island.textSecondary
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(Modifier.size(Spacing.sm))
        }
        // Niente a capo: nei bottoni affiancati (weight 1f) l'etichetta verrebbe spezzata.
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Azione secondaria: pill incassata, stesso ingombro della primaria. */
@Composable
fun IslandSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = LocalButtonShape.current
) {
    IslandButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        containerColor = EinaTheme.island.sunken,
        contentColor = contentColor,
        shape = shape
    )
}

/** Chip filtro a pill: selezionata = tinta della categoria, altrimenti superficie incassata. */
@Composable
fun IslandChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val hapticTap = LocalHapticTap.current
    // Anche da spenta la chip porta il suo colore, tenue: la fila dei gruppi muscolari era una
    // sequenza di pastiglie grigie tutte uguali, e il colore compariva solo dopo aver scelto.
    // Selezionata diventa piena, cosi' la differenza fra scelto e non scelto resta netta.
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.White else accentColor,
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) accentColor else accentColor.copy(alpha = 0.13f))
            .clickable { hapticTap(); onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    )
}

/** Campo di testo island: nessun bordo, superficie incassata, angoli morbidi. */
@Composable
fun IslandTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    // Nei campi di ricerca l'etichetta non deve restare: appena si scrive sparisce e il testo
    // digitato si prende tutta l'altezza della barra, invece di stringersi sotto l'etichetta.
    labelAsPlaceholder: Boolean = false
) {
    val island = EinaTheme.island
    val labelText: @Composable () -> Unit = {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (labelAsPlaceholder) island.textSecondary else Color.Unspecified
        )
    }
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = if (labelAsPlaceholder) null else labelText,
        placeholder = if (labelAsPlaceholder) labelText else null,
        singleLine = singleLine,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        shape = com.eina.app.ui.theme.squircle(18.dp),
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = island.textSecondary) } },
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = island.sunken,
            unfocusedContainerColor = island.sunken,
            disabledContainerColor = island.sunken,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = island.textSecondary,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
    )
}

/**
 * Campo numerico compatto in stile "tile": etichetta minuscola sopra, valore grande sotto.
 * Usato dove servono molti input piccoli affiancati (serie/reps/peso/recupero).
 */
@Composable
fun IslandNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val island = EinaTheme.island
    Column(modifier = modifier) {
        // Etichetta opzionale: in una pila di righe identiche si stampa solo sulla prima.
        if (label != null) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = island.textSecondary,
                modifier = Modifier.padding(start = Spacing.sm, bottom = 2.dp)
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .clip(com.eina.app.ui.theme.squircle(14.dp))
                .background(island.sunken)
                .padding(vertical = Spacing.md, horizontal = Spacing.sm),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            "–",
                            style = MaterialTheme.typography.titleMedium,
                            color = island.textSecondary
                        )
                    }
                    inner()
                }
            }
        )
    }
}

/**
 * Selettore orizzontale a "giorni" (vedi reference Activity): elemento attivo = isola bianca
 * flottante sopra la traccia incassata.
 */
@Composable
fun IslandSegmentedRow(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabels: List<String>? = null
) {
    val island = EinaTheme.island
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(com.eina.app.ui.theme.squircle(22.dp))
            .background(island.sunken)
            .padding(Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            SegmentedItem(
                label = label,
                secondary = secondaryLabels?.getOrNull(index),
                selected = selected,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RowScope.SegmentedItem(
    label: String,
    secondary: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    val shape = com.eina.app.ui.theme.squircle(18.dp)
    val base = modifier
        .clip(shape)
        .clickable { hapticTap(); onClick() }
    Column(
        modifier = if (selected) {
            base
                .islandShadow(6.dp, shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = Spacing.md)
        } else {
            base.padding(vertical = Spacing.md)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else island.textSecondary
        )
        if (secondary != null) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    island.textSecondary
                }
            )
        }
    }
}
