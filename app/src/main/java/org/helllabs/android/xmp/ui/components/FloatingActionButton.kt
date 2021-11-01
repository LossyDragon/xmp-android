package org.helllabs.android.xmp.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsPadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.darkPrimary
import org.helllabs.android.xmp.util.upperCase

@Composable
fun PlaylistsFab(
    modifier: Modifier = Modifier,
    extended: Boolean,
    onFabClicked: () -> Unit,
) {
    key(true) {
        androidx.compose.material3.FloatingActionButton(
            onClick = onFabClicked,
            modifier = modifier
                .padding(16.dp)
                .navigationBarsPadding()
                .height(48.dp)
                .widthIn(min = 48.dp),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White
        ) {
            AnimatingFabContent(
                extended = extended,
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null
                    )
                },
                text = {
                    androidx.compose.material3.Text(
                        text = stringResource(id = R.string.menu_new_playlist),
                    )
                },
            )
        }
    }
}

@Composable
fun ExtendedFab(
    onClick: () -> Unit
) {
    ExtendedFloatingActionButton(
        modifier = Modifier.navigationBarsPadding(),
        icon = {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.menu_new_playlist),
            )
        },
        text = { Text(text = stringResource(id = R.string.menu_new_playlist).upperCase()) },
        onClick = { onClick() },
        elevation = FloatingActionButtonDefaults.elevation(8.dp),
        contentColor = Color.White,
        backgroundColor = darkPrimary
    )
}

@Composable
fun ScrollFab(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    shouldPadBottom: Boolean
) {
    FloatingActionButton(
        modifier = modifier
            .padding(12.dp)
            .navigationBarsPadding(bottom = shouldPadBottom),
        onClick = { onClick() },
        elevation = FloatingActionButtonDefaults.elevation(8.dp),
        contentColor = Color.White,
        backgroundColor = darkPrimary
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Scroll to top",
        )
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
private fun PlaylistsFabPreview() {
    XmpTheme3(false) {
        PlaylistsFab(extended = true) {}
    }
}

@Preview
@Composable
private fun FabPreview() {
    XmpTheme(false) {
        ExtendedFab {}
    }
}

@Preview
@Composable
private fun ScrollFabPreview() {
    XmpTheme(false) {
        ScrollFab(onClick = { }, shouldPadBottom = false)
    }
}
