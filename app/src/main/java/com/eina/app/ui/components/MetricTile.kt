package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    centered: Boolean = false
) {
    val island = EinaTheme.island
    val alignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    Column(
        modifier = modifier
            .clip(TileShape)
            .background(island.sunken)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = alignment
    ) {
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
        Row(verticalAlignment = Alignment.Bottom) {
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
