package com.lossydragon.media3.ui.screens.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.dp
import com.lossydragon.media3.model.FrameSnapshot
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PatternInfoRow(
    modifier: Modifier = Modifier,
    frame: FrameSnapshot
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            InfoChip(label = "Pos", value = "${frame.position}")
            InfoChip(label = "Pat", value = "${frame.pattern}")
            InfoChip(label = "Row", value = "${frame.row}/${frame.numRows}")
            InfoChip(label = "Spd", value = "${frame.speed}")
            InfoChip(label = "BPM", value = "${frame.bpm}")
        }
    )
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        Surface {
            PatternInfoRow(
                frame = FrameSnapshot(
                    position = 3,
                    pattern = 4,
                    row = 6,
                    numRows = 64,
                    speed = 12,
                    bpm = 128,
                    timeMs = 0,
                    totalTimeMs = 0,
                    channels = persistentListOf(),
                )
            )
        }
    }
}
