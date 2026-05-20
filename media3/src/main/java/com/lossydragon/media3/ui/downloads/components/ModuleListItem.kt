package com.lossydragon.media3.ui.downloads.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lossydragon.media3.model.Artist
import com.lossydragon.media3.model.ArtistInfo
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.ui.theme.XmpTheme
import org.koin.dsl.module

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ModuleListItem(module: Module, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xff404040), RoundedCornerShape(4.dp))
                    .border(
                        2.dp,
                        Color(0xff808080),
                        RoundedCornerShape(4.dp)
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
                text = module.songtitle.ifBlank { "(untitled)" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = module.artist,
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
private fun Preview() {
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
