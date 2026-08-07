package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing

@Composable
fun EinaBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Text(
        text = text,
        color = if (filled) Color.White else color,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(
                color = if (filled) color else color.copy(alpha = 0.14f),
                shape = PillShape
            )
            .padding(horizontal = Spacing.md, vertical = 5.dp)
    )
}
