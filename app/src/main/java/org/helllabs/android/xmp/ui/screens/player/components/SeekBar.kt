package org.helllabs.android.xmp.ui.screens.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import org.helllabs.android.xmp.ui.screens.player.PlayerTimeState
import org.helllabs.android.xmp.ui.theme.XmpTheme

@Stable
sealed class SeekEvent {
    data class OnSeek(val isSeeking: Boolean, val value: Float) : SeekEvent()
}

@Composable
fun PlayerSeekBar(
    state: PlayerTimeState,
    onSeek: (SeekEvent) -> Unit
) {
    val newPosition = remember { mutableFloatStateOf(0F) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.wrapContentWidth(Alignment.Start),
            text = state.timeNow,
            fontWeight = FontWeight.Bold
        )

        Slider(
            modifier = Modifier
                .fillMaxWidth(.8f)
                .height(20.dp),
            value = if (state.isSeeking) newPosition.floatValue else state.seekPos,
            valueRange = 0f..state.seekMax,
            onValueChange = {
                newPosition.floatValue = it
                onSeek(SeekEvent.OnSeek(true, newPosition.floatValue))
            },
            onValueChangeFinished = {
                onSeek(SeekEvent.OnSeek(false, newPosition.floatValue))
            }
        )

        Text(
            modifier = Modifier.wrapContentWidth(Alignment.End),
            text = state.timeTotal,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
private fun Preview_PlayerSeekBar() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            PlayerSeekBar(
                state = PlayerTimeState(
                    timeNow = "00:40",
                    timeTotal = "66:90",
                    seekPos = 6f,
                    seekMax = 100f,
                    isSeeking = false,
                ),
                onSeek = {}
            )
        }
    }
}
