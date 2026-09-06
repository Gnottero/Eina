package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.squircle

/**
 * Bottom sheet in island style: large top corners, white surface, custom handle. Used instead of
 * dropdown menus and dialogs wherever the entries are thumb-sized actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    // Enable when the content can exceed the screen (an expanding calendar, for instance):
    // otherwise the Column squeezes its children and the duration wheels drift away from their
    // selection band.
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    val island = EinaTheme.island
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Always fully expanded: these sheets hold lists and grids, and the partial state would
        // cut the content in half.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = Spacing.md, bottom = Spacing.xs)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(PillShape)
                    .background(island.sunken)
            )
        },
        modifier = modifier
    ) {
        // Inside a sheet buttons are not pills; see LocalButtonShape.
        CompositionLocalProvider(LocalButtonShape provides SheetButtonShape) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .nestedScroll(SheetContentNestedScroll)
                    .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.xl)
                    .padding(bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = Spacing.sm)
                    )
                }
                content()
            }
        }
    }
}

/**
 * Consumes the scroll left over by the sheet content so it never reaches the sheet itself.
 *
 * ModalBottomSheet listens to the nested scroll of its children: once the list is at the top, any
 * further downward drag — or fling — became a sheet drag and closed it mid-scroll, and the tug of
 * war between list and sheet could stall the scroll entirely. By eating the leftovers, the sheet
 * can still be dragged from the handle and the areas without a list.
 */
private val SheetContentNestedScroll = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = if (source == NestedScrollSource.UserInput) available else Offset.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

/** Small-radius squircle: the shape of the wide buttons inside a sheet. */
val SheetButtonShape: Shape = squircle(16.dp)

/** Squircle of the action rows: more generous, since they are tiles rather than buttons. */
private val SheetRowShape: Shape = squircle(20.dp)

/**
 * Action row of a sheet: full-width filled tile with a leading icon and a label. [destructive]
 * turns the icon red and the row stays last in the list.
 *
 * The row is the tile itself (sunken background, continuous corner) rather than an icon badge on
 * white, so the touch target is visible.
 */
@Composable
fun SheetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    destructive: Boolean = false
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    val accent = MaterialTheme.colorScheme.primary
    // The icon carries the colour and the label stays black, destructive rows included: a red
    // label on a red-tinted tile made the whole row read as already dangerous. Here the bin alone
    // is red, on the same surface as every other action.
    val iconColor = if (destructive) DestructiveRed else accent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SheetRowShape)
            .background(island.sunken)
            .clickable { hapticTap(); onClick() }
            .defaultMinSize(minHeight = 60.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = island.textSecondary
                )
            }
        }
    }
}

/**
 * Action confirmation: title, one explanatory line and two equally wide buttons.
 *
 * Replaces the Material AlertDialog, which right-aligned two backgroundless labels: small targets
 * in a corner, with only the text colour marking the dangerous one. Here the buttons are the ones
 * the rest of the app uses — filled pills, primary carrying the accent ramp — instead of the
 * small-radius rectangles that read as a stock Android dialog.
 */
@Composable
fun IslandAlertDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit
) {
    val island = EinaTheme.island
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(IslandShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = island.textSecondary)
            Row(
                modifier = Modifier.padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                IslandSecondaryButton(
                    text = dismissLabel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = PillShape
                )
                // The confirm pill wears the app's ramp like every other primary action. A flat red
                // one was the only button in the app painted a colour of its own, and it read as a
                // system dialog dropped into the page; the warning is in the words, and the way back
                // is the button next to it.
                IslandButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = PillShape
                )
            }
        }
    }
}

/** Red of destructive actions; used only here and not part of the general palette. */
val DestructiveRed = Color(0xFFE5484D)
