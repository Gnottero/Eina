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

/** Spazio da lasciare in fondo al contenuto perche' la nav flottante non copra l'ultima isola. */
@Composable
fun islandBottomSpace(extra: Dp = Spacing.xl): Dp =
    IslandNavBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + extra

/**
 * Shell di schermata in stile island: background pieno, header fisso in alto, contenuto
 * scrollabile con gutter laterale costante e spazio finale per la nav flottante.
 */
@Composable
fun IslandScreen(
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    horizontalPadding: Dp = Spacing.xl,
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
                    .padding(bottom = islandBottomSpace()),
                verticalArrangement = verticalArrangement,
                content = content
            )
        }
        if (floatingBottom != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = islandBottomSpace(extra = Spacing.sm))
                    .padding(horizontal = Spacing.xl)
            ) {
                floatingBottom()
            }
        }
    }
}

/**
 * Variante per liste lunghe: header fisso e slot libero (tipicamente una LazyColumn) sotto,
 * con lo stesso background e la stessa gestione degli inset.
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

/** Padding di coda per le LazyColumn dentro [IslandListScreen]. */
@Composable
fun islandListContentPadding(top: Dp = Spacing.sm): PaddingValues = PaddingValues(
    start = Spacing.xl,
    end = Spacing.xl,
    top = top,
    bottom = islandBottomSpace()
)
