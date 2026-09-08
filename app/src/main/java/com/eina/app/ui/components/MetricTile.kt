package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape

/**
 * Headline metric inside an island: sunken tile, tiny icon and label, large number.
 *
 * Shared by the workout header and the summary, which is the same screen with the recording taken
 * away: the two must not drift apart.
 */
@Composable
fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    /** Icon colour; the number stays black, so a row of tiles is told apart without reading it. */
    tint: Color = MaterialTheme.colorScheme.primary,
    /**
     * Centred content. Three tiles side by side on a 360dp phone are narrower than their label, and
     * left-aligned they read as three ragged columns instead of one row of measurements.
     */
    centered: Boolean = false,
    /** Dashboard metrics already sit in a containing island and can be rendered without a tile. */
    showBackground: Boolean = true
) {
    val island = EinaTheme.island
    val alignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    Column(
        modifier = modifier
            .then(
                if (showBackground) Modifier.clip(TileShape).background(island.sunken)
                else Modifier
            )
            .padding(
                horizontal = if (showBackground) Spacing.md else 0.dp,
                vertical = Spacing.md
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = alignment
    ) {
        if (centered) {
            // The trailing spacer mirrors the icon and keeps the label itself on the exact centre
            // of the tile. Unlike an overlaid icon, it cannot collide with a long label.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = island.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(Spacing.xs + 15.dp))
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = island.textSecondary,
                    maxLines = 1
                )
            }
        }
        Row(
            modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}
