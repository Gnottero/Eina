package com.eina.app.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eina.app.data.db.WeightType
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun CreateExerciseScreen(
    onSaved: () -> Unit,
    viewModel: CreateExerciseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::onMediaPicked)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text("Nuovo esercizio", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Descrizione") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.loggingInstructions,
            onValueChange = viewModel::onLoggingInstructionsChange,
            label = { Text("Come registrare") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.equipment,
            onValueChange = viewModel::onEquipmentChange,
            label = { Text("Attrezzatura (opzionale)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        WeightTypeDropdown(selected = uiState.weightType, onSelected = viewModel::onWeightTypeChange)

        Text("Muscoli primari", style = MaterialTheme.typography.titleSmall)
        CategoryChipRow(
            selected = uiState.primaryCategories,
            onToggle = viewModel::onPrimaryCategoryToggle
        )

        Text("Muscoli secondari (opzionale)", style = MaterialTheme.typography.titleSmall)
        CategoryChipRow(
            selected = uiState.secondaryCategories,
            onToggle = viewModel::onSecondaryCategoryToggle
        )

        Text("GIF / immagine", style = MaterialTheme.typography.titleSmall)
        if (uiState.mediaUri != null) {
            AsyncImage(
                model = uiState.mediaUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
        OutlinedButton(onClick = { mediaPicker.launch("image/*") }) {
            Icon(Icons.Outlined.Image, contentDescription = null)
            Text(" Scegli dalla galleria")
        }

        Button(
            onClick = viewModel::save,
            enabled = uiState.canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salva esercizio")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightTypeDropdown(selected: WeightType, onSelected: (WeightType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo di carico") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WeightType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label()) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryChipRow(selected: Set<MuscleGroupCategory>, onToggle: (MuscleGroupCategory) -> Unit) {
    Column {
        MuscleGroupCategory.entries.chunked(3).forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(row, selected, onToggle)
            }
        }
    }
}

@Composable
private fun Row(
    categories: List<MuscleGroupCategory>,
    selected: Set<MuscleGroupCategory>,
    onToggle: (MuscleGroupCategory) -> Unit
) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        categories.forEach { category ->
            FilterChip(
                selected = category in selected,
                onClick = { onToggle(category) },
                label = { Text(category.label) }
            )
        }
    }
}
