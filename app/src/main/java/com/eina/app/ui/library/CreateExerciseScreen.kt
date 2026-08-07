package com.eina.app.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eina.app.data.db.WeightType
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandChip
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
import org.koin.androidx.compose.koinViewModel

@Composable
fun CreateExerciseScreen(
    onSaved: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: CreateExerciseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::onMediaPicked)
    }

    IslandScreen(
        header = {
            ScreenHeader(
                title = "Nuovo esercizio",
                subtitle = "Sara' utilizzabile come uno di libreria",
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            IslandTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = "Nome",
                modifier = Modifier.fillMaxWidth()
            )
            IslandTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = "Descrizione",
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
            IslandTextField(
                value = uiState.loggingInstructions,
                onValueChange = viewModel::onLoggingInstructionsChange,
                label = "Come registrare",
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
            IslandTextField(
                value = uiState.equipment,
                onValueChange = viewModel::onEquipmentChange,
                label = "Attrezzatura (opzionale)",
                modifier = Modifier.fillMaxWidth()
            )
            WeightTypeDropdown(
                selected = uiState.weightType,
                onSelected = viewModel::onWeightTypeChange
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Muscoli primari", style = MaterialTheme.typography.titleMedium)
            CategoryChips(
                selected = uiState.primaryCategories,
                onToggle = viewModel::onPrimaryCategoryToggle
            )
            Text(
                "Muscoli secondari (opzionale)",
                style = MaterialTheme.typography.titleMedium
            )
            CategoryChips(
                selected = uiState.secondaryCategories,
                onToggle = viewModel::onSecondaryCategoryToggle
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("GIF / immagine", style = MaterialTheme.typography.titleMedium)
            if (uiState.mediaUri != null) {
                AsyncImage(
                    model = uiState.mediaUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(TileShape)
                )
            }
            IslandSecondaryButton(
                text = "Scegli dalla galleria",
                icon = Icons.Outlined.Image,
                onClick = { mediaPicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        IslandButton(
            text = "Salva esercizio",
            onClick = viewModel::save,
            enabled = uiState.canSave,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightTypeDropdown(selected: WeightType, onSelected: (WeightType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        IslandTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = "Tipo di carico",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    selected: Set<MuscleGroupCategory>,
    onToggle: (MuscleGroupCategory) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        MuscleGroupCategory.entries.forEach { category ->
            IslandChip(
                text = category.label,
                selected = category in selected,
                accentColor = category.color,
                onClick = { onToggle(category) }
            )
        }
    }
}
