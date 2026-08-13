package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.usesDistance
import com.eina.app.data.db.usesDuration
import com.eina.app.data.db.usesWeight
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape

/**
 * Intestazione della tabella serie, condivisa fra allenamento ed editor routine.
 *
 * In routine non c'e' niente da confrontare con "l'ultima volta" e non c'e' niente da spuntare:
 * la colonna precedente e il pulsante di fine serie si spengono, il resto e' la stessa tabella.
 */
@Composable
fun SetTableHeader(
    weightType: WeightType,
    showPrevious: Boolean = true,
    trailingSlot: Boolean = true
) {
    Row(
        // Stesso rientro laterale delle righe della tabella (vedi SetRow): senza, le colonne
        // dell'intestazione partono 4dp piu' a sinistra e le etichette non stanno sopra i campi.
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        TableLabel(stringResource(R.string.table_set), Modifier.width(40.dp))
        if (showPrevious) {
            TableLabel(stringResource(R.string.table_previous), Modifier.weight(previousColumnWeight(weightType)))
        }
        // Senza carico da digitare la colonna kg non compare: lo spazio va alle ripetizioni.
        // Sugli esercizi a distanza lo stesso campo decimale porta i chilometri.
        if (weightType.usesWeight) {
            TableLabel(stringResource(R.string.table_kg), Modifier.weight(1f))
        } else if (weightType.usesDistance) {
            TableLabel(stringResource(R.string.table_km), Modifier.weight(1f))
        }
        TableLabel(
            stringResource(
                when {
                    weightType.usesDuration -> R.string.table_seconds
                    weightType.usesDistance -> R.string.table_minutes
                    else -> R.string.table_reps
                }
            ),
            Modifier.weight(1f)
        )
        if (trailingSlot) {
            Box(Modifier.size(42.dp))
        }
    }
}

/**
 * Larghezza della colonna "precedente". Sulla distanza il riepilogo e' lungo il doppio
 * ("5,2km·30" contro "60kg×8") e nella colonna stretta finiva tagliato a meta'.
 */
fun previousColumnWeight(weightType: WeightType): Float =
    if (weightType.usesDistance) 1.7f else 1.1f

/**
 * Riga di tabella che si butta via trascinandola verso sinistra.
 *
 * Il tocco lungo restava l'unico modo di togliere una serie, e su un esercizio senza colonna kg
 * i campi numerici si prendono quasi tutta la riga: il gesto trovava solo qualche millimetro di
 * bordo. Lo scorrimento laterale non ha questo problema — i campi non lo intercettano — e non
 * toglie niente: il tocco lungo resta al suo posto per il resto delle azioni.
 *
 * La soglia e' mezza riga invece del 50% di default con lancio: cosi' non si cancella una serie
 * di striscio mentre si scorre la pagina.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteSetRow(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hapticTap = LocalHapticTap.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                hapticTap()
                onDelete()
            }
            // Non si conferma mai lo stato: la riga sparisce perche' il dato sparisce, e se la
            // cancellazione non va in porto la riga torna al suo posto invece di restare
            // fuori schermo.
            false
        },
        positionalThreshold = { distance -> distance * 0.5f }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Il rosso si vede solo mentre si trascina: disegnato sempre, tingeva di rosa la
            // riga ferma, che sopra non ha un fondo suo.
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(TileShape)
                        .background(DestructiveRed.copy(alpha = 0.12f))
                        .padding(horizontal = Spacing.lg),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.active_delete_set),
                        tint = DestructiveRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = modifier,
        content = {
            // La riga che scorre porta il suo fondo, se no il rosso si vede anche attraverso.
            Box(
                modifier = Modifier
                    .clip(TileShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                content()
            }
        }
    )
}

@Composable
fun TableLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = EinaTheme.island.textSecondary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
    )
}

/**
 * Campo numerico della tabella serie: nessuna etichetta, segnaposto grigio col valore target o
 * dell'ultima volta (che resta un suggerimento, non un dato registrato).
 */
@Composable
fun SetValueField(
    value: String,
    placeholder: String?,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .clip(PillShape)
            .background(island.sunkenSoft)
            .padding(vertical = Spacing.md, horizontal = Spacing.xs),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder ?: "–",
                        style = MaterialTheme.typography.titleMedium,
                        color = island.textSecondary
                    )
                }
                inner()
            }
        }
    )
}
