package com.eina.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.squircle

/**
 * Row inside an island: leading square, name, one supporting line, trailing slot.
 *
 * It is not a card. Lists of like things — routines, exercises, settings — used to be a stack of
 * white islands on a cream page, which spent a shadow and 12dp of air on every entry and made a
 * library of 197 movements read as 197 separate objects. One island holding low rows says the same
 * with the rhythm of a list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IslandRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleSmall,
    /** Two lines where names are user-written and long, as routines are. */
    titleMaxLines: Int = 1,
    subtitleMaxLines: Int = 1,
    /** Replaces the supporting line with a custom one, for rows whose subtitle carries colour. */
    subtitleContent: @Composable (() -> Unit)? = null
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The touch target is a rounded rectangle inside the island, so the press ripple keeps
            // clear of the island's own corners.
            .clip(squircle(18.dp))
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onLongClick = onLongClick?.let { { hapticTap(); it() } },
                        onClick = { onClick?.let { hapticTap(); it() } }
                    )
                } else {
                    Modifier
                }
            )
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = titleStyle,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitleContent != null) {
                subtitleContent()
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = island.textSecondary,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke(this)
    }
}

/**
 * Tinted square that opens a row. It carries the colour of the thing the row is about — the muscle
 * group of an exercise, the leading group of a routine — so a long list is scannable by colour
 * before it is read.
 */
@Composable
fun RowLeadingTile(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(squircle(size / 2.6f))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

/**
 * Small tag on a row's supporting line. The muscle group carries its own colour, so a list read by
 * scrolling is sorted by eye before it is read; the equipment stays neutral, because it is a
 * property of the movement and not what the movement is.
 */
@Composable
fun RowTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    val island = EinaTheme.island
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (filled) color else island.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(squircle(8.dp))
            .background(if (filled) color.copy(alpha = 0.14f) else island.sunken)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}
