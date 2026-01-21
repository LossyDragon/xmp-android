package org.helllabs.android.xmp.ui.screens.playlist.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.components.BackButton
import org.helllabs.android.xmp.ui.theme.XmpTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistInfo(
    isScrolled: Boolean,
    onBack: () -> Unit,
    playlistName: String,
    playlistComment: String
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isScrolled) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = { BackButton(onClick = onBack) },
        title = {
            Column {
                Text(
                    text = playlistName,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = playlistComment.ifEmpty {
                        stringResource(id = R.string.error_no_comment)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        windowInsets = WindowInsets(right = 16.dp)
    )
}

@Preview
@Composable
private fun Preview_PlaylistInfo() {
    XmpTheme(useDarkTheme = true) {
        PlaylistInfo(
            isScrolled = false,
            onBack = { },
            playlistName = "Playlist Name",
            playlistComment = "Playlist Comment",
        )
    }
}
