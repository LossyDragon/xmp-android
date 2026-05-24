package com.lossydragon.media3.ui.screens.downloads.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.lossydragon.media3.model.Artist
import com.lossydragon.media3.model.ArtistInfo
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.ui.theme.XmpTheme
import com.lossydragon.media3.util.fromHtml

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ArtistListItem(alias: String, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(
            shape = MaterialTheme.shapes.small,
            focusedShape = MaterialTheme.shapes.small,
            pressedShape = MaterialTheme.shapes.small,
        ),
        content = {
            Text(
                text = alias.ifEmpty { "(untitled)" },
                style = MaterialTheme.typography.bodyLarge
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ModuleListItem(module: Module, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(
            shape = MaterialTheme.shapes.small,
            focusedShape = MaterialTheme.shapes.small,
            pressedShape = MaterialTheme.shapes.small,
        ),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xff404040), shape = MaterialTheme.shapes.extraSmall)
                    .border(
                        2.dp,
                        Color(0xff808080),
                        shape = MaterialTheme.shapes.extraSmall
                    ),
                contentAlignment = Alignment.Center,
                content = {
                    Text(
                        text = module.format,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            )
        },
        content = {
            Text(
                text = module.songtitle.ifBlank { "(untitled)" }.fromHtml(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = module.artist.fromHtml(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Text(text = "${module.sizeKb} KB", style = MaterialTheme.typography.labelSmall)
        },
    )
}

@Preview
@Composable
private fun ArtistListItem_Preview() {
    XmpTheme {
        Surface {
            ArtistListItem(alias = "Artist Alias", onClick = {})
        }
    }
}

@Preview
@Composable
private fun ModuleListItem_Preview() {
    XmpTheme {
        Surface {
            ModuleListItem(
                module = Module(
                    format = "669",
                    songtitle = "Song Title",
                    artistInfo = ArtistInfo(artist = listOf(Artist(alias = "Song Artist"))),
                    bytes = 99999999,
                ),
                onClick = {},
            )
        }
    }
}
