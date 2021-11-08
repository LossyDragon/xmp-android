package org.helllabs.android.xmp.ui.components

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsPadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.theme.XmpTheme3

private enum class Visibility {
    Visible,
    Gone,
}

@Composable
fun ScrollBackUp(
    enabled: Boolean,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(
        if (enabled) Visibility.Visible else Visibility.Gone,
        label = "ScrollBackUp Transition"
    )

    val bottomOffset by transition.animateDp(label = "ScrollBackUp offset") {
        if (it == Visibility.Gone) {
            (-24).dp
        } else {
            24.dp
        }
    }

    if (bottomOffset > 0.dp) {
        ExtendedFloatingActionButton(
            icon = {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    modifier = Modifier.height(18.dp),
                    contentDescription = null
                )
            },
            text = {
                Text(text = stringResource(id = R.string.scrollUp))
            },
            onClick = onClicked,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White,
            modifier = modifier
                .offset(x = 0.dp, y = -bottomOffset)
                .height(36.dp)
        )
    }
}

@Composable
fun PlaylistsFab(
    extended: Boolean,
    onFabClicked: () -> Unit,
) {
    FloatingActionButton(
        modifier = Modifier
            .navigationBarsPadding()
            .height(56.dp)
            .widthIn(min = 56.dp),
        onClick = onFabClicked,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = Color.White
    ) {
        AnimatingFabContent(
            extended = extended,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null
                )
            },
            text = { Text(text = stringResource(id = R.string.menu_new_playlist)) },
        )
    }
}

/************
 * Previews *
 ************/
@Preview
@Composable
private fun ScrollBackUpPreview() {
    XmpTheme3 {
        ScrollBackUp(true, {})
    }
}

@Preview
@Composable
private fun PlaylistsFabPreview() {
    XmpTheme3(false) {
        PlaylistsFab(extended = true) {}
    }
}
