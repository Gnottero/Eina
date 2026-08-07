package com.eina.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eina.app.ui.theme.AccentPrimary
import com.eina.app.ui.theme.Spacing

@Composable
fun BottomTimerBar(
    remainingSeconds: Int,
    onMinus15: () -> Unit,
    onPlus15: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMinus15) {
                Icon(Icons.Outlined.Remove, contentDescription = "-15s")
            }
            Text(
                text = formatRestTime(remainingSeconds),
                style = MaterialTheme.typography.headlineMedium,
                color = AccentPrimary
            )
            IconButton(onClick = onPlus15) {
                Icon(Icons.Outlined.Add, contentDescription = "+15s")
            }
            IconButton(onClick = onSkip) {
                Icon(Icons.Outlined.SkipNext, contentDescription = "Salta recupero")
            }
        }
    }
}

private fun formatRestTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
