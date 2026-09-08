package com.eina.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing

/**
 * Headline metric inside an island: icon and label centred over a large number.
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
    centered: Boolean = true
) {
    val island = EinaTheme.island
    val alignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    Column(
        modifier = modifier
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = alignment
    ) {
        Row(
            modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (centered) {
                Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally)
            } else {
                Arrangement.spacedBy(Spacing.xs)
            }
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = island.textSecondary,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start
        ) {
            // Mirror the unit on the left without drawing or announcing it. This keeps the number
            // itself on the same centre axis as the icon-label group instead of shifting it left.
            if (unit != null && centered) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Transparent,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clearAndSetSemantics { }
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}
