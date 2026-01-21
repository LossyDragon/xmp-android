package org.helllabs.android.xmp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.ui.theme.XmpTheme

@Composable
fun BottomBarButtons(
    isShuffle: Boolean,
    isLoop: Boolean,
    onShuffle: (Boolean) -> Unit,
    onLoop: (Boolean) -> Unit,
    onPlayAll: () -> Unit
) {
    BottomAppBar(
        actions = {
            ActionButton(
                checked = isShuffle,
                onCheckedChange = onShuffle,
                imageVector = Icons.Filled.Shuffle,
            )
            ActionButton(
                checked = isLoop,
                onCheckedChange = onLoop,
                imageVector = Icons.Filled.Repeat,
            )
        },
        floatingActionButton = { PlayAllButton(onClick = onPlayAll) },
    )
}

@Composable
private fun PlayAllButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
        content = {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
        }
    )
}

@Composable
private fun ActionButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    imageVector: ImageVector
) {
    IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = IconButtonDefaults.iconToggleButtonColors(checkedContentColor = Color.Green),
        content = { Icon(imageVector = imageVector, contentDescription = null) }
    )
}

@Preview
@Composable
private fun Preview_BottomBarButtons() {
    XmpTheme(useDarkTheme = true) {
        BottomBarButtons(
            isShuffle = true,
            isLoop = false,
            onShuffle = { },
            onLoop = { },
            onPlayAll = { }
        )
    }
}
