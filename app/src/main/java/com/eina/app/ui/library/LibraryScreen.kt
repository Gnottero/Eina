package com.eina.app.ui.library

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eina.app.R
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.exerciseName
import com.eina.app.ui.components.IslandAlertDialog
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandChip
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandListScreen
import com.eina.app.ui.components.IslandRow
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.components.RowLeadingTile
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.components.hasExerciseMedia
import com.eina.app.ui.components.islandBottomSpace
import com.eina.app.ui.components.islandShadow
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.label
import com.eina.app.ui.theme.primaryCategoryFor
import com.eina.app.ui.theme.squircle
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
                eyebrow = pluralStringResource(R.plurals.exercise_count, exercises.size, exercises.size),
                title = stringResource(R.string.library_title),
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
        // Search, filters and the 197 movements live in a single island that runs off the bottom
        // edge. As one card per exercise the page was a column of shadows, and the search bar was
        // a floating object of its own above them.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Spacing.gutter)
                .islandShadow(8.dp, IslandShape)
                .clip(IslandShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = islandBottomSpace())
            ) {
                item {
                    IslandTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        label = stringResource(R.string.library_search),
                        labelAsPlaceholder = true,
                        leadingIcon = Icons.Outlined.Search,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.md)
                    )
                    // The chips run from edge to edge of the island, not inside its padding: cut
                    // at the padding, the last one lost a couple of letters and read as broken
                    // rather than as a row that scrolls.
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md)
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
                }

                if (exercises.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(Spacing.lg)) {
                            Text(
                                text = stringResource(R.string.library_empty_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.library_empty_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = island.textSecondary
                            )
                        }
                    }
                } else {
                    items(exercises, key = { it.id }) { exercise ->
                        ExerciseListItem(
                            exercise = exercise,
                            onClick = { onExerciseClick(exercise.id) },
                            onLongClick = if (exercise.isCustom) {
                                { actionsFor = exercise }
                            } else {
                                null
                            }
                        )
                    }
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

/**
 * Library row: the first frame of the animation, the name, and "group · equipment" on one line.
 *
 * Where an exercise has no animation the square carries the first two letters of its muscle group
 * in that group's colour, rather than an empty frame: the row keeps its rhythm, and the colour says
 * the same thing the picture would have.
 */
@Composable
private fun ExerciseListItem(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val island = EinaTheme.island
    val category = primaryCategoryFor(exercise.muscleGroupsPrimary)
    val categoryLabel = category.label()
    val equipment = exercise.equipment?.takeIf { it.isNotBlank() }?.let { equipmentLabel(it) }

    IslandRow(
        modifier = Modifier.padding(horizontal = Spacing.sm),
        title = exercise.localizedName(),
        titleStyle = MaterialTheme.typography.bodyLarge,
        // Two lines. On one, "Affondi camminati a corpo libero" and "Affondi camminati con
        // bilanciere" both ellipsised to "Affondi camminati a corp…" and the list stopped
        // distinguishing the movements it exists to list.
        titleMaxLines = 2,
        subtitle = listOfNotNull(categoryLabel, equipment).joinToString(" · "),
        onClick = onClick,
        onLongClick = onLongClick,
        leading = {
            if (hasExerciseMedia(exercise.mediaUri)) {
                // No background and no coloured frame: the figure carries its own opaque white, so
                // a tinted background would be invisible and a frame would read as an empty slot.
                AsyncImage(
                    model = exercise.mediaUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(squircle(17.dp))
                )
            } else {
                RowLeadingTile(color = category.color) {
                    Text(
                        text = categoryLabel.take(2).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = category.color
                    )
                }
            }
        },
        trailing = {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = island.textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    )
}
