package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing

/**
 * Coloured band filling the top of an island edge to edge: the accent ramp behind white text.
 *
 * It replaces the tinted headline the cards used to carry as loose text on white. A page now has
 * exactly one of these — the running session, the open-session banner, the progress total — and it
 * is the thing the eye lands on first. The island clips it, so the band inherits the continuous
 * corners instead of drawing its own.
 */
@Composable
fun RampBand(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.lg),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Spacing.xs),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(EinaTheme.island.accentRamp))
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content
    )
}
