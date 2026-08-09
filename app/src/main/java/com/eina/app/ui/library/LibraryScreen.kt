package com.eina.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.eina.app.R
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.exerciseName
import com.eina.app.ui.components.EinaBadge
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandAlertDialog
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.components.hasExerciseMedia
import com.eina.app.ui.components.IslandChip
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandListScreen
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.islandListContentPadding
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.squircle
import com.eina.app.ui.theme.label
import com.eina.app.ui.theme.primaryCategoryFor
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onExerciseClick: (Long) -> Unit = {},
    onCreateExerciseClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    viewModel: LibraryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val island = EinaTheme.island
    val locale = currentLocale()
    val context = LocalContext.current
    // Solo gli esercizi custom (creati a mano o arrivati con una routine importata) si possono
    // togliere: quelli di libreria li riscriverebbe comunque il seeder.
    var actionsFor by remember { mutableStateOf<ExerciseEntity?>(null) }
    var confirmDeleteFor by remember { mutableStateOf<ExerciseEntity?>(null) }
    val deleteBlocked = stringResource(R.string.library_delete_blocked)
    // Ordine alfabetico nella lingua attiva: il database li tiene ordinati per nome inglese.
    val exercises = remember(uiState.exercises, locale) {
        uiState.exercises.sortedBy { it.exerciseName().localized(locale).lowercase(locale) }
    }

    IslandListScreen(
        header = {
            ScreenHeader(
                title = stringResource(R.string.library_title),
                subtitle = pluralStringResource(R.plurals.exercise_count, exercises.size, exercises.size),
                onBack = onBack,
                trailing = {
                    if (onCreateExerciseClick != null) {
                        IslandIconButton(
                            icon = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.library_new_exercise),
                            onClick = onCreateExerciseClick
                        )
                    }
                }
            )
        }
    ) {
        IslandTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            label = stringResource(R.string.library_search),
            labelAsPlaceholder = true,
            leadingIcon = Icons.Outlined.Search,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md)
        ) {
            items(MuscleGroupCategory.entries) { category ->
                IslandChip(
                    text = category.label(),
                    selected = uiState.selectedCategory == category,
                    accentColor = category.color,
                    onClick = { viewModel.onCategorySelected(category) }
                )
            }
        }

        if (exercises.isEmpty()) {
            Box(modifier = Modifier.padding(horizontal = Spacing.xl)) {
                IslandEmptyState(
                    title = stringResource(R.string.library_empty_title),
                    description = stringResource(R.string.library_empty_description)
                )
            }
        } else {
            LazyColumn(
                contentPadding = islandListContentPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseListItem(
                        exercise = exercise,
                        onClick = { onExerciseClick(exercise.id) },
                        onLongClick = if (exercise.isCustom) {
                            { actionsFor = exercise }
                        } else {
                            null
                        },
                        secondaryColor = island.textSecondary
                    )
                }
            }
        }
    }

    actionsFor?.let { exercise ->
        IslandBottomSheet(onDismiss = { actionsFor = null }, title = exercise.localizedName()) {
            SheetActionRow(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.library_delete_exercise),
                description = stringResource(R.string.library_delete_exercise_description),
                destructive = true,
                onClick = { actionsFor = null; confirmDeleteFor = exercise }
            )
        }
    }

    confirmDeleteFor?.let { exercise ->
        IslandAlertDialog(
            title = stringResource(R.string.library_delete_confirm_title, exercise.localizedName()),
            text = stringResource(R.string.library_delete_confirm_text),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDeleteFor = null
                viewModel.deleteCustomExercise(exercise) { deleted ->
                    // Un esercizio ancora citato da una routine o dallo storico resta:
                    // dirlo e' meglio di un tocco che non fa niente.
                    if (!deleted) Toast.makeText(context, deleteBlocked, Toast.LENGTH_LONG).show()
                }
            },
            dismissLabel = stringResource(R.string.action_cancel),
            onDismiss = { confirmDeleteFor = null }
        )
    }
}

@Composable
private fun ExerciseListItem(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    secondaryColor: androidx.compose.ui.graphics.Color,
    onLongClick: (() -> Unit)? = null
) {
    val category = primaryCategoryFor(exercise.muscleGroupsPrimary)
    // Riga bassa e regolare invece di card alte: il badge colorato del gruppo muscolare occupava
    // una seconda riga per conto suo e in un elenco di 197 voci diventava rumore. Ora il colore
    // resta (un punto), e gruppo e attrezzo stanno su una riga sola sotto il nome.
    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Miniatura del primo fotogramma: si riconosce il movimento senza aprire la scheda.
            if (hasExerciseMedia(exercise.mediaUri)) {
                // Miniatura piena, senza fondo ne' cornice colorata: la figura ha il suo bianco
                // opaco, quindi un fondo tinto non si vedrebbe e una cornice la fa sembrare un
                // segnaposto vuoto. Il colore del gruppo muscolare sta nel punto e nel testo qui
                // sotto, che bastano a dare ritmo alla lista.
                AsyncImage(
                    model = exercise.mediaUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        // Raggio esplicito e non TileShape: su 56dp i 24dp della tile
                        // arrotondano fino a farla diventare un cerchio.
                        .clip(squircle(18.dp))
                )
            }
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = exercise.localizedName(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(category.color)
                    )
                    val equipment = exercise.equipment?.takeIf { it.isNotBlank() }?.let { equipmentLabel(it) }
                    // Gruppo muscolare nel suo colore, attrezzo in grigio: due informazioni di
                    // peso diverso sulla stessa riga, distinte dal colore invece che da un badge.
                    Text(
                        text = category.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = category.color,
                        maxLines = 1
                    )
                    if (equipment != null) {
                        Text(
                            text = "· $equipment",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = secondaryColor.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
