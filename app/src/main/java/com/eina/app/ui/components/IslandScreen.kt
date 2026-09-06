package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.Spacing

/** Trailing space so the floating nav bar does not cover the last island. */
@Composable
fun islandBottomSpace(extra: Dp = Spacing.xl): Dp =
    IslandNavBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + extra

/**
 * Island screen shell: full background, fixed header, scrollable content with a constant side
 * gutter and trailing space for the floating nav bar.
 */
@Composable
fun IslandScreen(
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    horizontalPadding: Dp = Spacing.gutter,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Spacing.md),
    floatingBottom: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            header?.invoke()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding)
                    // The floating CTA sits above the nav bar, so the scroll has to clear both or
                    // the last island ends up under the button.
                    .padding(bottom = islandBottomSpace(extra = if (floatingBottom != null) 76.dp else Spacing.xl)),
                verticalArrangement = verticalArrangement,
                content = content
            )
        }
        if (floatingBottom != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = islandBottomSpace(extra = Spacing.sm))
                    .padding(horizontal = Spacing.gutter)
            ) {
                floatingBottom()
            }
        }
    }
}

/**
 * Variant for long lists: fixed header and a free slot below (typically a LazyColumn), with the
 * same background and inset handling.
 */
@Composable
fun IslandListScreen(
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            header?.invoke()
            content()
        }
    }
}

/** Trailing padding for the LazyColumns inside [IslandListScreen]. */
@Composable
fun islandListContentPadding(top: Dp = Spacing.sm): PaddingValues = PaddingValues(
    start = Spacing.gutter,
    end = Spacing.gutter,
    top = top,
    bottom = islandBottomSpace()
)
