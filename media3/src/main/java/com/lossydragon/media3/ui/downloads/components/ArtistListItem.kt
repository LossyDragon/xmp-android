package com.lossydragon.media3.ui.downloads.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.*
import com.lossydragon.media3.ui.theme.XmpTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ArtistListItem(alias: String, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        content = { Text(text = alias, style = MaterialTheme.typography.bodyLarge) },
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        Surface {
            ArtistListItem(alias = "Artist Alias", onClick = {})
        }
    }
}
