package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing

/**
 * Button shape for the current context: pills on a page, small-radius squircles inside a sheet
 * (see IslandBottomSheet), where a full-width 52dp pill reads as a label rather than a button.
 */
val LocalButtonShape = compositionLocalOf<Shape> { PillShape }

/** Primary action: accent filled, pill on a page and squircle inside a sheet. */
@Composable
fun IslandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    shape: Shape = LocalButtonShape.current,
    /** Thin outline drawn over the fill; how a white button stays readable on a white surface. */
    borderColor: Color? = null
) {
    val hapticTap = LocalHapticTap.current
    val accent = MaterialTheme.colorScheme.primary
    // The primary action carries the ramp and a coloured shadow: it is the one element on the page
    // meant to attract the tap, and a flat orange did not.
    val ramped = enabled && containerColor == accent
    val fill = EinaTheme.island.accentRamp
    Button(
        onClick = { hapticTap(); onClick() },
        enabled = enabled,
        shape = shape,
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .then(
                if (ramped) {
                    Modifier
                        .shadow(
                            elevation = 14.dp,
                            shape = shape,
                            clip = false,
                            ambientColor = accent.copy(alpha = 0.35f),
                            spotColor = accent.copy(alpha = 0.45f)
                        )
                        .clip(shape)
                        .background(Brush.horizontalGradient(fill))
                } else {
                    Modifier
                }
            )
            .then(
                if (borderColor != null && enabled) Modifier.border(1.dp, borderColor, shape)
                else Modifier
            ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (ramped) Color.Transparent else containerColor,
            contentColor = contentColor,
            disabledContainerColor = EinaTheme.island.sunken,
            disabledContentColor = EinaTheme.island.textSecondary
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(Modifier.size(Spacing.sm))
        }
        // No wrapping: in side-by-side buttons (weight 1f) the label would be broken in two.
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Secondary action: white pill with a thin outline, same footprint as the primary one.
 *
 * It used to be filled with the sunken grey, which was the darkest thing on the page wherever the
 * button sat on the background ("add exercise", "save as routine"). White plus an outline reads as
 * one of the islands the rest of the app is made of, and keeps working on a white card too, where
 * a fill this light would vanish.
 */
@Composable
fun IslandSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = LocalButtonShape.current
) {
    val island = EinaTheme.island
    IslandButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = contentColor,
        shape = shape,
        borderColor = island.outlineSubtle
    )
}

/** Pill filter chip: filled with the category colour when selected, tinted softly otherwise. */
@Composable
fun IslandChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val hapticTap = LocalHapticTap.current
    // Even unselected the chip keeps its colour, softly: the muscle group row used to be a line of
    // identical grey pills that only gained colour after a choice.
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.White else accentColor,
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) accentColor else accentColor.copy(alpha = 0.13f))
            .clickable { hapticTap(); onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    )
}

/** Island text field: no border, sunken surface, soft corners. */
@Composable
fun IslandTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    // In search fields the label must not stay: it disappears as soon as text is typed, which then
    // takes the whole height of the bar instead of squeezing under the label.
    labelAsPlaceholder: Boolean = false
) {
    val island = EinaTheme.island
    val labelText: @Composable () -> Unit = {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (labelAsPlaceholder) island.textSecondary else Color.Unspecified
        )
    }
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = if (labelAsPlaceholder) null else labelText,
        placeholder = if (labelAsPlaceholder) labelText else null,
        singleLine = singleLine,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        shape = com.eina.app.ui.theme.squircle(18.dp),
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = island.textSecondary) } },
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = island.sunken,
            unfocusedContainerColor = island.sunken,
            disabledContainerColor = island.sunken,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = island.textSecondary,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
    )
}

/**
 * Compact numeric tile field: tiny label on top, large value below. Used where many small inputs
 * sit side by side (sets, reps, weight, rest).
 */
@Composable
fun IslandNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val island = EinaTheme.island
    Column(modifier = modifier) {
        // Optional label: in a stack of identical rows only the first one shows it.
        if (label != null) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = island.textSecondary,
                modifier = Modifier.padding(start = Spacing.sm, bottom = 2.dp)
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .clip(com.eina.app.ui.theme.squircle(14.dp))
                .background(island.sunken)
                .padding(vertical = Spacing.md, horizontal = Spacing.sm),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            "–",
                            style = MaterialTheme.typography.titleMedium,
                            color = island.textSecondary
                        )
                    }
                    inner()
                }
            }
        )
    }
}

/**
 * Horizontal day selector: the active item is a white island floating above the sunken track.
 */
@Composable
fun IslandSegmentedRow(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabels: List<String>? = null
) {
    val island = EinaTheme.island
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(com.eina.app.ui.theme.squircle(22.dp))
            .background(island.sunken)
            .padding(Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            SegmentedItem(
                label = label,
                secondary = secondaryLabels?.getOrNull(index),
                selected = selected,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RowScope.SegmentedItem(
    label: String,
    secondary: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    val shape = com.eina.app.ui.theme.squircle(18.dp)
    val base = modifier
        .clip(shape)
        .clickable { hapticTap(); onClick() }
    Column(
        modifier = if (selected) {
            base
                .islandShadow(6.dp, shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = Spacing.md)
        } else {
            base.padding(vertical = Spacing.md)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else island.textSecondary
        )
        if (secondary != null) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    island.textSecondary
                }
            )
        }
    }
}
