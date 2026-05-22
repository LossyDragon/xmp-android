package com.lossydragon.media3.ui.screens.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.media3.common.Player
import com.lossydragon.media3.ui.theme.XmpTheme

@Composable
internal fun ChipList(
    isShuffle: Boolean,
    repeatMode: Int,
    isSubSongs: Boolean,
    onShuffle: () -> Unit,
    onLoop: () -> Unit,
    onModInfo: () -> Unit,
    onShowSongMessage: () -> Unit,
    onShowSongInstruments: () -> Unit,
    onPlaySubSongs: () -> Unit,
    onShowDurations: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally
        ),
        contentPadding = PaddingValues(horizontal = 6.dp),
        content = {
            item {
                FilterChip(
                    selected = isShuffle,
                    onClick = onShuffle,
                    label = {
                        val shuffleText = if (isShuffle) "Shuffle On" else "Shuffle Off"
                        Text(text = shuffleText)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = repeatMode != 0,
                    onClick = onLoop,
                    label = {
                        val loopText = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> "Repeating One"
                            Player.REPEAT_MODE_ALL -> "Repeating All"
                            Player.REPEAT_MODE_OFF -> "Repeat Off"
                            else -> throw IllegalArgumentException("Invalid Repeat Mode")
                        }
                        Text(text = loopText)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    leadingIcon = {
                        val loopIcon = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            Player.REPEAT_MODE_ALL -> Icons.Default.RepeatOn
                            Player.REPEAT_MODE_OFF -> Icons.Default.Repeat
                            else -> throw IllegalArgumentException("Invalid Repeat Mode")
                        }

                        Icon(
                            imageVector = loopIcon,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = isSubSongs,
                    onClick = onPlaySubSongs,
                    label = {
                        val shuffleText = if (isSubSongs) "Subsongs On" else "Subsongs Off"
                        Text(text = shuffleText)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
            item {
                AssistChip(
                    onClick = onShowDurations,
                    label = { Text(text = "Show Subsongs") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
            item {
                AssistChip(
                    onClick = onModInfo,
                    label = { Text(text = "Mod Info") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
            item {
                AssistChip(
                    onClick = onShowSongInstruments,
                    label = { Text(text = "Mod Instruments") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
            item {
                AssistChip(
                    onClick = onShowSongMessage,
                    label = { Text(text = "Song Message") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            ChipList(
                isShuffle = false,
                repeatMode = 1,
                isSubSongs = false,
                onShuffle = {},
                onLoop = {},
                onModInfo = {},
                onShowSongMessage = {},
                onShowSongInstruments = {},
                onPlaySubSongs = {},
                onShowDurations = {},
            )
        }
    }
}
