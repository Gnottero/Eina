package com.eina.app.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eina.app.ui.theme.Spacing

/**
 * Card standard dell'app. Alias sottile di [IslandCard]: mantiene il nome usato dalle schermate
 * esistenti, ma eredita superficie flottante, angoli morbidi e ombra dello stile island.
 */
@Composable
fun EinaCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    IslandCard(
        modifier = modifier,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content
    )
}
