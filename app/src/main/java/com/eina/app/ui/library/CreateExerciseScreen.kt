package com.eina.app.ui.library

import android.content.ActivityNotFoundException
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
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eina.app.R
import com.eina.app.data.db.WeightType
import com.eina.app.ui.components.DestructiveRed
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
import com.eina.app.ui.theme.label
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
                title = stringResource(R.string.create_exercise_title),
                subtitle = stringResource(R.string.create_exercise_subtitle),
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            IslandTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.field_name),
                modifier = Modifier.fillMaxWidth()
            )
            IslandTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = stringResource(R.string.field_description),
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
            IslandTextField(
                value = uiState.loggingInstructions,
                onValueChange = viewModel::onLoggingInstructionsChange,
                label = stringResource(R.string.field_how_to_log),
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
            IslandTextField(
                value = uiState.equipment,
                onValueChange = viewModel::onEquipmentChange,
                label = stringResource(R.string.field_equipment),
                modifier = Modifier.fillMaxWidth()
            )
            WeightTypeDropdown(
                selected = uiState.weightType,
                onSelected = viewModel::onWeightTypeChange
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.create_primary_muscles), style = MaterialTheme.typography.titleMedium)
            CategoryChips(
                selected = uiState.primaryCategories,
                onToggle = viewModel::onPrimaryCategoryToggle
            )
            Text(
                stringResource(R.string.create_secondary_muscles),
                style = MaterialTheme.typography.titleMedium
            )
            CategoryChips(
                selected = uiState.secondaryCategories,
                onToggle = viewModel::onSecondaryCategoryToggle
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.create_media), style = MaterialTheme.typography.titleMedium)
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
                text = stringResource(R.string.create_pick_gallery),
                icon = Icons.Outlined.Image,
                onClick = {
                    try {
                        mediaPicker.launch("image/*")
                    } catch (e: ActivityNotFoundException) {
                        viewModel.onMediaPickerUnavailable()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            uiState.mediaError?.let { error ->
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = DestructiveRed
                )
            }
        }

        IslandButton(
            text = stringResource(R.string.create_save),
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
            label = stringResource(R.string.field_weight_type),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                text = category.label(),
                selected = category in selected,
                accentColor = category.color,
                onClick = { onToggle(category) }
            )
        }
    }
}
