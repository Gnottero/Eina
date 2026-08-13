package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eina.app.R
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.exerciseName
import com.eina.app.data.db.matchesQuery
import com.eina.app.ui.library.currentLocale
import com.eina.app.ui.library.equipmentLabel
import com.eina.app.ui.library.localized
import com.eina.app.ui.library.localizedName
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.label
import com.eina.app.ui.theme.primaryCategoryFor

/**
 * Exercise picker: search by name, muscle group filter and a thumbnail of the first animation
 * frame. The same sheet is used by the workout screen and the routine editor.
 *
 * The thumbnail lives here only: in the workout and routine lists the name is enough, and an image
 * on every row would take space from the numbers.
 */
@Composable
fun ExercisePickerSheet(
    exercises: List<ExerciseEntity>,
    onPick: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit,
    // The same sheet also replaces an exercise: only the title changes.
    title: String = stringResource(R.string.active_add_exercise_sheet_title)
) {
    val island = EinaTheme.island
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<MuscleGroupCategory?>(null) }

    val locale = currentLocale()
    // Alphabetical in the active language: the database sorts them by English name. The sort does
    // not depend on the query, otherwise every keystroke would re-sort the whole library.
    val sorted = remember(exercises, locale) {
        exercises.sortedBy { it.exerciseName().localized(locale).lowercase(locale) }
    }
    val filtered = remember(sorted, query, category) {
        sorted.filter { exercise ->
            val matchesQuery = query.isBlank() || exercise.matchesQuery(query)
            val matchesCategory = category == null ||
                primaryCategoryFor(exercise.muscleGroupsPrimary) == category
            matchesQuery && matchesCategory
        }
    }

    IslandBottomSheet(onDismiss = onDismiss, title = title) {
        IslandTextField(
            value = query,
            onValueChange = { query = it },
            label = stringResource(R.string.library_search),
            labelAsPlaceholder = true,
            leadingIcon = Icons.Outlined.Search,
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(vertical = Spacing.sm)
        ) {
            items(MuscleGroupCategory.entries) { entry ->
                IslandChip(
                    text = entry.label(),
                    selected = category == entry,
                    accentColor = entry.color,
                    onClick = { category = if (category == entry) null else entry }
                )
            }
        }

        if (filtered.isEmpty()) {
            Text(
                text = if (exercises.isEmpty()) {
                    stringResource(R.string.active_library_empty)
                } else {
                    stringResource(R.string.active_search_empty)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary,
                modifier = Modifier.padding(vertical = Spacing.lg)
            )
        } else {
            // Height tied to the screen instead of a fixed 380dp: on a tall phone that left half
            // the sheet empty under a small list window.
            val listHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp
            LazyColumn(
                modifier = Modifier.height(listHeight),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    val exerciseCategory = primaryCategoryFor(exercise.muscleGroupsPrimary)
                    IslandCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(Spacing.md),
                        elevation = 0.dp,
                        color = island.sunken,
                        onClick = { onPick(exercise) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            // First animation frame, as in the library: the movement is
                            // recognisable without opening the detail.
                            if (hasExerciseMedia(exercise.mediaUri)) {
                                AsyncImage(
                                    model = exercise.mediaUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        // Explicit radius, not TileShape: at 48dp its 24dp corners
                                        // round the thumbnail into a circle.
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exercise.localizedName(), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = listOfNotNull(
                                        exerciseCategory.label(),
                                        exercise.equipment?.takeIf { it.isNotBlank() }
                                            ?.let { equipmentLabel(it) }
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = island.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
