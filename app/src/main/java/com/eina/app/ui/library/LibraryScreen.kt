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
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
    // Custom exercises only: the seeder would overwrite a library one.
    onEditExerciseClick: ((Long) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    viewModel: LibraryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val island = EinaTheme.island
    val locale = currentLocale()
    val context = LocalContext.current
    // Only custom exercises (hand-made or imported with a routine) can be removed.
    var actionsFor by remember { mutableStateOf<ExerciseEntity?>(null) }
    var confirmDeleteFor by remember { mutableStateOf<ExerciseEntity?>(null) }
    val deleteBlocked = stringResource(R.string.library_delete_blocked)
    // Alphabetical in the active language: the database sorts them by English name.
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
                .padding(horizontal = Spacing.gutter)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(horizontal = Spacing.gutter, vertical = Spacing.md)
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
            Box(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
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
            if (onEditExerciseClick != null) {
                SheetActionRow(
                    icon = Icons.Outlined.Edit,
                    label = stringResource(R.string.library_edit_custom),
                    onClick = { actionsFor = null; onEditExerciseClick(exercise.id) }
                )
            }
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
                    // An exercise still referenced by a routine or the history is kept; say so
                    // instead of leaving the tap silent.
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
    // Short, regular rows instead of tall cards: the coloured muscle badge took a line of its own
    // and turned into noise across 197 entries. The colour survives as a dot, with group and
    // equipment on one line under the name.
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
            // First-frame thumbnail: the movement is recognisable without opening the detail.
            if (hasExerciseMedia(exercise.mediaUri)) {
                // No background and no coloured frame: the figure carries its own opaque white, so
                // a tinted background would be invisible and a frame would read as an empty slot.
                AsyncImage(
                    model = exercise.mediaUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        // Explicit radius, not TileShape: at 56dp its 24dp corners round the
                        // thumbnail into a circle.
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
                    // Muscle group in its colour, equipment in grey: two levels of information on
                    // one line, told apart by colour instead of a badge.
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
