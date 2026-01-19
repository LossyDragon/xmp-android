package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.tooling.preview.*
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerButtonsState
import org.helllabs.android.xmp.compose.ui.player.RepeatMode

@Stable
sealed class PlayerControlsEvent {
    data object OnStop : PlayerControlsEvent()
    data object OnPrev : PlayerControlsEvent()
    data object OnPlay : PlayerControlsEvent()
    data object OnNext : PlayerControlsEvent()
    data class OnRepeat(val value: RepeatMode) : PlayerControlsEvent()
}

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    onEvent: (PlayerControlsEvent) -> Unit,
    state: PlayerButtonsState
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onEvent(PlayerControlsEvent.OnStop) }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.Stop,
                contentDescription = null
            )
        }
        IconButton(onClick = { onEvent(PlayerControlsEvent.OnPrev) }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = null
            )
        }
        FloatingActionButton(
            onClick = { onEvent(PlayerControlsEvent.OnPlay) },
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (state.isPlaying) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                contentDescription = null
            )
        }
        IconButton(onClick = { onEvent(PlayerControlsEvent.OnNext) }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.SkipNext,
                contentDescription = null
            )
        }
        IconButton(
            onClick = {
                val nextMode = when (state.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.REPEAT
                    RepeatMode.REPEAT -> RepeatMode.REPEAT_ONE
                    RepeatMode.REPEAT_ONE -> RepeatMode.OFF
                }
                onEvent(PlayerControlsEvent.OnRepeat(nextMode))
            },
        ) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = when (state.repeatMode) {
                    RepeatMode.OFF -> Icons.Default.Repeat
                    RepeatMode.REPEAT -> Icons.Default.RepeatOn
                    RepeatMode.REPEAT_ONE -> Icons.Default.RepeatOneOn
                },
                tint = if (state.repeatMode != RepeatMode.OFF) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
                contentDescription = null
            )
        }
    }
}

private class PlayerButtonsPreview : PreviewParameterProvider<PlayerButtonsState> {
    override fun getDisplayName(index: Int): String {
        return when (index) {
            0 -> RepeatMode.OFF.name
            1 -> RepeatMode.REPEAT.name
            2 -> RepeatMode.REPEAT_ONE.name
            else -> "Unknown"
        }
    }

    override val values: Sequence<PlayerButtonsState>
        get() = sequenceOf(
            PlayerButtonsState(isPlaying = true, repeatMode = RepeatMode.OFF),
            PlayerButtonsState(isPlaying = false, repeatMode = RepeatMode.REPEAT),
            PlayerButtonsState(isPlaying = true, repeatMode = RepeatMode.REPEAT_ONE),
        )
}

@Preview
@Composable
private fun Preview_PlayerButtons(
    @PreviewParameter(PlayerButtonsPreview::class) state: PlayerButtonsState
) {
    XmpTheme {
        PlayerBottomAppBar {
            PlayerControls(
                state = state,
                onEvent = { },
            )
        }
    }
}
