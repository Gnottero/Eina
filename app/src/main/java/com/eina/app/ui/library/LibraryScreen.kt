package com.eina.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.EinaCard
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.primaryCategoryFor
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onExerciseClick: (Long) -> Unit = {},
    onCreateExerciseClick: () -> Unit = {},
    viewModel: LibraryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateExerciseClick) {
                Icon(Icons.Filled.Add, contentDescription = "Nuovo esercizio")
            }
        }
    ) { innerPadding ->
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Cerca esercizio") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs)
        ) {
            items(MuscleGroupCategory.entries) { category ->
                val selected = uiState.selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onCategorySelected(category) },
                    label = { Text(category.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = category.color.copy(alpha = 0.2f),
                        selectedLabelColor = category.color
                    )
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(uiState.exercises, key = { it.id }) { exercise ->
                ExerciseListItem(exercise = exercise, onClick = { onExerciseClick(exercise.id) })
            }
        }
    }
    }
}

@Composable
private fun ExerciseListItem(exercise: ExerciseEntity, onClick: () -> Unit) {
    val category = primaryCategoryFor(exercise.muscleGroupsPrimary)
    EinaCard(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.padding(top = Spacing.xs)
                ) {
                    EinaBadge(text = category.label, color = category.color)
                }
            }
        }
    }
}
