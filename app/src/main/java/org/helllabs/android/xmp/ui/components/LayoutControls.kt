package org.helllabs.android.xmp.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.green

val layoutControlsHeight = 64.dp

@Composable
fun LayoutControls(
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
    onLoop: () -> Unit,
    onShuffle: () -> Unit,
    isLoopEnabled: Boolean,
    isShuffleEnabled: Boolean
) {
    val background = MaterialTheme.colorScheme.surface
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        color = background,
        contentColor = MaterialTheme.colorScheme.contentColorFor(background),
        tonalElevation = 3.0.dp,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(layoutControlsHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onPlay() }) {
                Icon(
                    modifier = Modifier.scale(1.2f),
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(id = R.string.button_play_all),
                    tint = LocalContentColor.current
                )
            }
            IconButton(onClick = { onLoop() }) {
                Icon(
                    modifier = Modifier.scale(1.2f),
                    imageVector = Icons.Default.Repeat,
                    contentDescription = stringResource(id = R.string.button_toggle_loop),
                    tint = if (isLoopEnabled) green else LocalContentColor.current
                )
            }
            IconButton(onClick = { onShuffle() }) {
                Icon(
                    modifier = Modifier.scale(1.2f),
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = stringResource(id = R.string.button_toggle_shuffle),
                    tint = if (isShuffleEnabled) green else LocalContentColor.current
                )
            }
        }
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
private fun LayoutControlsPreview() {
    XmpTheme3 {
        LayoutControls(
            onPlay = {},
            onLoop = {},
            onShuffle = {},
            isLoopEnabled = true,
            isShuffleEnabled = false
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun LayoutControlsDarkPreview() {
    XmpTheme3 {
        LayoutControls(
            onPlay = {},
            onLoop = {},
            onShuffle = {},
            isLoopEnabled = true,
            isShuffleEnabled = false
        )
    }
}
