package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.sectionBackground

@Composable
fun LayoutControls(
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
    onLoop: () -> Unit,
    onShuffle: () -> Unit,
    isLoopEnabled: Boolean,
    isShuffleEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(sectionBackground)
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onPlay() }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(id = R.string.button_play_all),
                tint = Color.White
            )
        }
        IconButton(onClick = { onLoop() }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.Repeat,
                contentDescription = stringResource(id = R.string.button_toggle_loop),
                tint = if (isLoopEnabled) Color.Green else Color.White
            )
        }
        IconButton(onClick = { onShuffle() }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(id = R.string.button_toggle_shuffle),
                tint = if (isShuffleEnabled) Color.Green else Color.White
            )
        }
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
fun LayoutControlsPreview() {
    AppTheme(false) {
        LayoutControls(
            onPlay = {},
            onLoop = {},
            onShuffle = {},
            isLoopEnabled = true,
            isShuffleEnabled = false
        )
    }
}

@Preview
@Composable
fun LayoutControlsPreviewDark() {
    AppTheme(true) {
        LayoutControls(
            onPlay = {},
            onLoop = {},
            onShuffle = {},
            isLoopEnabled = true,
            isShuffleEnabled = false
        )
    }
}
