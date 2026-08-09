package com.eina.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing

/** Altezza riservata alla nav flottante: le schermate la usano come padding di coda del contenuto. */
// Misurata sul layout reale: 12dp di margine verticale + 8dp di padding interno + 46dp di voce,
// per lato. Sottostimarla fa finire l'ultima isola sotto la nav.
val IslandNavBarHeight = 92.dp

data class IslandNavItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * Barra di navigazione flottante a pill (stile reference): non tocca i bordi dello schermo,
 * la voce attiva si espande in una pastiglia colorata con etichetta.
 */
@Composable
fun IslandNavBar(
    items: List<IslandNavItem>,
    modifier: Modifier = Modifier
) {
    IslandSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        shape = PillShape,
        // Non del tutto opaca: il contenuto che le scorre sotto si intravede appena, cosi' la
        // barra galleggia sulla pagina invece di tagliarla in due.
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        elevation = 18.dp,
        outlined = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Le voci si dimensionano sul contenuto: con un weight fisso l'etichetta della voce
            // attiva verrebbe tagliata (etichette di lunghezza molto diversa fra loro).
            items.forEach { item -> IslandNavBarItem(item) }
        }
    }
}

@Composable
private fun IslandNavBarItem(item: IslandNavItem) {
    val island = EinaTheme.island
    // La voce attiva e' una pastiglia piena con la rampa dell'accento e contenuto bianco: a
    // colpo d'occhio si vede dove si e', anche in uno screenshot rimpicciolito.
    val contentColor by animateColorAsState(
        targetValue = if (item.selected) Color.White else island.textSecondary,
        label = "navItemColor"
    )
    val fill = remember(island.accentRamp) { Brush.horizontalGradient(island.accentRamp) }
    val horizontalPadding by animateDpAsState(
        targetValue = if (item.selected) Spacing.lg else Spacing.md,
        label = "navItemPadding"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val hapticTap = LocalHapticTap.current

    Row(
        modifier = Modifier
            .clip(PillShape)
            .then(if (item.selected) Modifier.background(fill) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { hapticTap(); item.onClick() }
            )
            .padding(horizontal = horizontalPadding, vertical = Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        if (item.selected) {
            Box(Modifier.width(Spacing.sm))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
