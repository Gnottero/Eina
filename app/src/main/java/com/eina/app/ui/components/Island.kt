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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape

/** Ombra morbida e diffusa: e' quella che stacca l'isola dal background senza bordi marcati. */
fun Modifier.islandShadow(
    elevation: Dp,
    shape: Shape,
    shadowColor: Color,
    isDark: Boolean
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = shadowColor.copy(alpha = if (isDark) 0.6f else 0.10f),
    spotColor = shadowColor.copy(alpha = if (isDark) 0.6f else 0.12f)
)

@Composable
fun Modifier.islandShadow(elevation: Dp, shape: Shape): Modifier {
    val island = EinaTheme.island
    return islandShadow(elevation, shape, island.shadow, island.isDark)
}

/**
 * Contenitore base dello stile island: superficie flottante, angoli generosi, ombra morbida.
 * Tutto il resto della UI (card, tile, barra di navigazione, timer) e' costruito su questo.
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
                // Il tocco lungo apre le azioni dell'elemento: ha preso il posto dei tre puntini,
                // quindi va agganciato anche quando l'isola non ha un tocco breve suo.
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

/** Isola con padding interno e layout a colonna: il mattone piu' usato nelle schermate. */
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
 * Tile del "bento": icona + etichetta in alto, numero grande in basso.
 * `accentColor` non nullo = tile piena (stato in evidenza), altrimenti tile bianca.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    icon: ImageVector? = null,
    accentColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val island = EinaTheme.island
    val filled = accentColor != null
    val contentColor = if (filled) Color.White else MaterialTheme.colorScheme.onSurface
    val labelColor = if (filled) Color.White.copy(alpha = 0.85f) else island.textSecondary

    IslandCard(
        modifier = modifier,
        color = accentColor ?: MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
                maxLines = 1
            )
            if (unit != null) {
                Spacer(Modifier.size(Spacing.xs))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

/**
 * Intestazione di schermata: titolo grande + sottotitolo, con back tondo opzionale a sinistra
 * e slot per un'azione tonda a destra.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
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
        // Titolo e sottotitolo respirano: attaccati, la data e l'orario si leggevano come una riga sola.
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
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
 * Titoletto di sezione fra due gruppi di isole, con azione opzionale: se si passa `actionIcon`
 * l'azione e' un bottone tondo (piu' compatto e meno rumoroso di un'etichetta), altrimenti
 * si usa `actionLabel` come testo. `actionLabel` resta comunque la descrizione accessibile.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

/** Bottone tondo in stile island (usato negli header e come azione secondaria compatta). */
@Composable
fun IslandIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    IslandSurface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(percent = 50),
        color = containerColor,
        elevation = 6.dp,
        onClick = onClick
    ) {
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

/** Stato vuoto coerente: icona tenue, titolo, testo di supporto. */
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(island.sunken),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = island.textSecondary)
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
