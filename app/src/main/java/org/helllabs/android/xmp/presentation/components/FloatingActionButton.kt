package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsPadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.darkPrimary
import org.helllabs.android.xmp.util.upperCase

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
fun FabPreview() {
    AppTheme(false) {
        ExtendedFab {}
    }
}

@Preview
@Composable
fun ScrollFabPreview() {
    AppTheme(false) {
        ScrollFab(onClick = { }, shouldPadBottom = false)
    }
}
