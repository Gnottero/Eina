package com.eina.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.data.prefs.AppLanguage
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import java.util.Locale

/**
 * Language picker, built like the exercise picker: search field on top and a scrolling list of
 * rows, one per language, with a tick on the active one.
 *
 * The chips it replaces showed every language at once, which only works while they are three: a
 * fourth line of pills would have pushed the rest of the settings down the page. A searchable list
 * takes the same room whether the app speaks three languages or thirty.
 */
@Composable
fun LanguagePickerSheet(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val island = EinaTheme.island
    var query by remember { mutableStateOf("") }

    // Labels are resolved here and not inside the list: the search matches what is on screen, and
    // stringResource cannot be called from the filter.
    val entries = AppLanguage.entries.map { it to stringResource(it.labelRes) }
    // System stays first, the real languages follow in alphabetical order of their own name — the
    // order the entries are declared in is not one a reader can predict.
    val sorted = remember(entries) {
        entries.sortedWith(
            compareBy(
                { it.first != AppLanguage.SYSTEM },
                { it.second.lowercase(Locale.ROOT) }
            )
        )
    }
    val filtered = remember(sorted, query) {
        if (query.isBlank()) sorted
        else sorted.filter { (entry, label) ->
            // The tag is searched too: someone looking for their language while the app speaks
            // another one is more likely to type "fr" than to guess the spelling shown.
            label.contains(query, ignoreCase = true) ||
                entry.tag?.contains(query, ignoreCase = true) == true
        }
    }

    IslandBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.settings_language_title)) {
        IslandTextField(
            value = query,
            onValueChange = { query = it },
            label = stringResource(R.string.settings_language_search),
            labelAsPlaceholder = true,
            leadingIcon = Icons.Outlined.Search,
            modifier = Modifier.fillMaxWidth()
        )

        if (filtered.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_language_search_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary,
                modifier = Modifier.padding(vertical = Spacing.lg)
            )
        } else {
            // Same rule as the exercise picker: the list window follows the screen instead of a
            // fixed height, and shrinks to the rows when they are few.
            val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(vertical = Spacing.xs)
            ) {
                items(filtered, key = { it.first.name }) { (entry, label) ->
                    LanguageRow(
                        label = label,
                        selected = entry == current,
                        onClick = { onSelect(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val island = EinaTheme.island
    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        elevation = 0.dp,
        color = island.sunken,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            // The tick marks the active language; nothing else changes, so the rows stay a list
            // and not a row of buttons.
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // Keeps the label of every row starting at the same place.
                Column(modifier = Modifier.size(20.dp)) {}
            }
        }
    }
}
