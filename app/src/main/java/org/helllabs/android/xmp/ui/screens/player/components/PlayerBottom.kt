package org.helllabs.android.xmp.ui.screens.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import org.helllabs.android.xmp.ui.screens.player.PlayerButtonsState
import org.helllabs.android.xmp.ui.screens.player.PlayerInfoState
import org.helllabs.android.xmp.ui.screens.player.PlayerTimeState
import org.helllabs.android.xmp.ui.screens.player.RepeatMode
import org.helllabs.android.xmp.ui.theme.XmpTheme

/**
 * Height-less [androidx.compose.material3.BottomAppBar]
 */
@Composable
fun PlayerBottomAppBar(
    modifier: Modifier = Modifier,
    containerColor: Color = BottomAppBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomAppBarDefaults.ContainerElevation,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shape = RectangleShape,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Preview
@Composable
private fun Preview_PlayerBottomAppBar() {
    XmpTheme(useDarkTheme = true) {
        PlayerBottomAppBar {
            PlayerInfo(
                state = PlayerInfoState(
                    infoSpeed = "11",
                    infoBpm = "22",
                    infoPos = "33",
                    infoPat = "44"
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlayerSeekBar(
                state = PlayerTimeState(
                    timeNow = "00:00",
                    timeTotal = "00:00",
                    seekPos = 25f,
                    seekMax = 100f
                ),
                onSeek = { }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlayerControls(
                state = PlayerButtonsState(
                    isPlaying = false,
                    repeatMode = RepeatMode.OFF
                ),
                onEvent = { },
            )
        }
    }
}
