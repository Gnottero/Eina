package com.eina.app.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing

/**
 * Headline metric inside an island: tiny icon and label over a large number.
 *
 * No sunken fill under it: a grey box behind every figure was the darkest thing on a white card and
 * read as a table dropped into the page. The numbers sit straight on the surface that holds them.
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
    // Icon and label are one unit: a trailing spacer used to centre the label alone on the tile,
    // which left the icon hanging off to one side, away from the words it belongs to. The group is
    // centred as a whole, so it sits on the same axis as the number below.
    val labelRow: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            // Ellipsis and not the default clip: a label too long for the tile used to be cut
            // mid-word without a sign.
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = island.textSecondary,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    val valueText: @Composable () -> Unit = {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
    val unitText: (@Composable () -> Unit)? = unit?.let {
        {
            Text(
                text = " $it",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (centered) {
        // The label rides on the number's centre, not on the centre of number plus unit.
        MetricColumn(
            modifier = modifier.padding(vertical = Spacing.md),
            gap = Spacing.xs,
            label = labelRow,
            value = valueText,
            unit = unitText
        )
    } else {
        Column(
            modifier = modifier.padding(vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            horizontalAlignment = Alignment.Start
        ) {
            labelRow()
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                valueText()
                if (unitText != null) {
                    Column(modifier = Modifier.padding(bottom = 3.dp)) { unitText() }
                }
            }
        }
    }
}
