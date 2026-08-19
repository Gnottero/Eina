package com.eina.app.ui.components

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

/** Height reserved for the floating nav bar; screens use it as trailing content padding. */
// Measured on the real layout: 12dp outer margin + 8dp inner padding + 46dp item, per side.
// Underestimating it leaves the last island hidden behind the bar.
val IslandNavBarHeight = 92.dp

data class IslandNavItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * Floating pill navigation bar: it never touches the screen edges, and the active item expands
 * into a coloured pill with its label.
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
        // Slightly translucent, so content scrolling underneath shows through and the bar floats
        // over the page instead of cutting it in two.
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        elevation = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            // SpaceBetween rather than a centred group: the coloured pill must keep the same 8dp
            // distance from the container edge. Centred, the side margin depended on the length of
            // the active label and the difference was visible.
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Items size themselves on their content: with a fixed weight the active label would
            // be clipped, since the labels differ a lot in length.
            items.forEach { item -> IslandNavBarItem(item) }
        }
    }
}

@Composable
private fun IslandNavBarItem(item: IslandNavItem) {
    val island = EinaTheme.island
    // The active item is a filled pill with the accent ramp and white content. The colour is not
    // animated: an animated one started from grey and the freshly shown label read grey on orange
    // for the duration of the transition.
    val contentColor = if (item.selected) Color.White else island.textSecondary
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
