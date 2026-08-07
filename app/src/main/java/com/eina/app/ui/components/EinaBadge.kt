package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.Spacing

@Composable
fun EinaBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
    )
}
