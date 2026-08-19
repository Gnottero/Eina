package com.eina.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eina.app.R
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
import com.eina.app.ui.theme.squircle

/**
 * Two-layer island shadow: a wide diffuse one for the distance from the background, and a tight
 * darker one under the edge for contact. Either alone reads wrong — printed on the background, or
 * cut out of it — at the cost of one extra draw.
 */
fun Modifier.islandShadow(
    elevation: Dp,
    shape: Shape,
    shadowColor: Color,
    isDark: Boolean
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = shadowColor.copy(alpha = if (isDark) 0.55f else 0.07f),
        spotColor = shadowColor.copy(alpha = if (isDark) 0.55f else 0.08f)
    )
    .shadow(
        elevation = (elevation / 4).coerceAtLeast(1.dp),
        shape = shape,
        clip = false,
        ambientColor = shadowColor.copy(alpha = if (isDark) 0.4f else 0.05f),
        spotColor = shadowColor.copy(alpha = if (isDark) 0.4f else 0.06f)
    )

@Composable
fun Modifier.islandShadow(elevation: Dp, shape: Shape): Modifier {
    val island = EinaTheme.island
    return islandShadow(elevation, shape, island.shadow, island.isDark)
}

/**
 * Base container of the island style: floating surface, generous corners, soft shadow. Every other
 * component (cards, tiles, nav bar, timer) is built on it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IslandSurface(
    modifier: Modifier = Modifier,
    shape: Shape = IslandShape,
    color: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 10.dp,
    outlined: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    Box(
        modifier = modifier
            .islandShadow(elevation, shape)
            .clip(shape)
            .background(color)
            .then(if (outlined) Modifier.border(1.dp, island.outlineSubtle, shape) else Modifier)
            .then(
                // Long press opens the item actions, so it must be wired even when the island has
                // no short click of its own.
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onLongClick = onLongClick?.let { { hapticTap(); it() } },
                        onClick = { onClick?.let { hapticTap(); it() } }
                    )
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

/** Island with inner padding and a column layout: the most used building block. */
@Composable
fun IslandCard(
    modifier: Modifier = Modifier,
    shape: Shape = TileShape,
    color: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Spacing.sm),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    IslandSurface(
        modifier = modifier,
        shape = shape,
        color = color,
        elevation = elevation,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

/**
 * Bento tile: icon and label on top, large number below. A non-null [accentColor] makes the tile
 * filled (highlighted state); otherwise it stays white.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    icon: ImageVector? = null,
    /** Small line under the number: a secondary value not worth a tile of its own. */
    description: String? = null,
    accentColor: Color? = null,
    /** Tint of the white tile: colours icon and label, not the number. */
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val island = EinaTheme.island
    val filled = accentColor != null
    val contentColor = if (filled) Color.White else MaterialTheme.colorScheme.onSurface
    val labelColor = if (filled) Color.White.copy(alpha = 0.82f) else tint
    // The filled tile uses a diagonal ramp instead of a flat fill: at this size a solid orange
    // reads like a sticker.
    val fillBrush = accentColor?.let {
        Brush.linearGradient(if (it == MaterialTheme.colorScheme.primary) island.accentRamp else listOf(it, it))
    }

    IslandSurface(
        modifier = modifier,
        shape = TileShape,
        color = if (filled) Color.Transparent else MaterialTheme.colorScheme.surface,
        elevation = 8.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillBrush != null) Modifier.background(fillBrush) else Modifier)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (icon != null) {
                    // The icon sits in a soft circle rather than bare next to the text, which also
                    // aligns every tile to the same height.
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                if (filled) Color.White.copy(alpha = 0.22f)
                                else tint.copy(alpha = 0.14f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (filled) Color.White else tint,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    color = contentColor,
                    maxLines = 1
                )
                if (unit != null) {
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelLarge,
                        // Same colour as the number: tinted apart, the unit read as its own label.
                        color = contentColor,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (filled) Color.White.copy(alpha = 0.82f) else island.textSecondary
                )
            }
        }
    }
}

/**
 * Screen header: large title and subtitle, with an optional round back button on the left and a
 * slot for a round action on the right.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** Tiny line above the title (date, context), as in "WEDNESDAY 9 AUGUST / Dashboard". */
    eyebrow: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    val island = EinaTheme.island
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (onBack != null) {
            IslandIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack
            )
        }
        // Title and subtitle need spacing: joined, date and time read as a single line.
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    // Accent, not grey: it is the only touch of colour at the top of the page.
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HeaderTitle(title)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = island.textSecondary
                )
            }
        }
        trailing?.invoke(this)
    }
}

/**
 * Header title: one line that shrinks to fit rather than wrapping.
 *
 * The header keeps a back button and up to two round actions, so a long word ("Completato" in the
 * summary of a workout just finished) was broken mid-word and left a single letter on the second
 * line. It is drawn only once the size is settled: measuring at full size first would otherwise
 * show one frame of the oversized title.
 */
@Composable
private fun HeaderTitle(title: String) {
    val base = MaterialTheme.typography.headlineMedium
    var style by remember(title) { mutableStateOf(base) }
    var settled by remember(title) { mutableStateOf(false) }
    Text(
        text = title,
        style = style,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.drawWithContent { if (settled) drawContent() },
        onTextLayout = { layout ->
            if (layout.didOverflowWidth && style.fontSize > MinHeaderTitleSize) {
                style = style.copy(fontSize = style.fontSize * 0.92f)
            } else {
                settled = true
            }
        }
    )
}

/** Floor of the header title: below this the title would be smaller than the subtitle. */
private val MinHeaderTitleSize = 20.sp

/**
 * Section title between two groups of islands, with an optional action: with [actionIcon] the
 * action is a round button, otherwise [actionLabel] is rendered as text. [actionLabel] is the
 * accessible description in both cases.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    /** Colour of the rule left of the title, identifying the section. */
    tint: Color = MaterialTheme.colorScheme.primary,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Coloured rule: a plain black section title got lost between the islands.
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(PillShape)
                .background(tint)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (actionIcon != null && onAction != null) {
            IslandIconButton(
                icon = actionIcon,
                contentDescription = actionLabel,
                onClick = onAction,
                size = 36.dp
            )
        } else if (actionLabel != null && onAction != null) {
            val hapticTap = LocalHapticTap.current
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable { hapticTap(); onAction() }
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            )
        }
    }
}

/**
 * Card header: tinted icon badge, title and supporting line. It gives colour to islands holding a
 * chart rather than a number, which otherwise read as white rectangles with black text.
 */
@Composable
fun IslandCardHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val island = EinaTheme.island
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(squircle(11.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = island.textSecondary
                )
            }
        }
    }
}

/** Round island button, used in headers and as a compact secondary action. */
@Composable
fun IslandIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Disabled must not react to touch either: otherwise the button vibrates and nothing happens
    // (as with "start" while a workout is already running).
    enabled: Boolean = true,
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    // The accent-filled button carries the same ramp as the primary CTA: two different oranges on
    // one screen are noticeable.
    val ramped = containerColor == MaterialTheme.colorScheme.primary
    val ramp = EinaTheme.island.accentRamp
    IslandSurface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(percent = 50),
        color = if (ramped) Color.Transparent else containerColor,
        elevation = 6.dp,
        onClick = onClick.takeIf { enabled }
    ) {
        if (ramped) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(ramp))
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp)
        )
    }
}

/** Shared empty state: soft icon, title, supporting text. */
@Composable
fun IslandEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null
) {
    val island = EinaTheme.island
    IslandCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (icon != null) {
            // Accent circle rather than grey: the empty state is the first thing a new user sees
            // and must not look like a broken screen.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(Spacing.xs))
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
        }
    }
}
