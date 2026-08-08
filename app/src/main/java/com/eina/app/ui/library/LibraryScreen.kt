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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.exerciseName
import com.eina.app.ui.components.EinaBadge
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.hasExerciseMedia
import com.eina.app.ui.theme.TileShape
import com.eina.app.ui.components.IslandChip
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandListScreen
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.islandListContentPadding
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.label
import com.eina.app.ui.theme.primaryCategoryFor
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onExerciseClick: (Long) -> Unit = {},
    onCreateExerciseClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    title: String? = null,
    viewModel: LibraryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val island = EinaTheme.island
    val locale = currentLocale()
    // Ordine alfabetico nella lingua attiva: il database li tiene ordinati per nome inglese.
    val exercises = remember(uiState.exercises, locale) {
        uiState.exercises.sortedBy { it.exerciseName().localized(locale).lowercase(locale) }
    }

    IslandListScreen(
        header = {
            ScreenHeader(
                title = title ?: stringResource(R.string.library_title),
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
                        secondaryColor = island.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    secondaryColor: androidx.compose.ui.graphics.Color
) {
    val category = primaryCategoryFor(exercise.muscleGroupsPrimary)
    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.lg),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Miniatura del primo fotogramma: si riconosce il movimento senza aprire la scheda.
            if (hasExerciseMedia(exercise.mediaUri)) {
                AsyncImage(
                    model = exercise.mediaUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(TileShape)
                        .background(EinaTheme.island.sunken)
                )
            }
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(exercise.localizedName(), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    EinaBadge(text = category.label(), color = category.color)
                    exercise.equipment?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = equipmentLabel(it),
                            style = MaterialTheme.typography.labelMedium,
                            color = secondaryColor,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = secondaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
