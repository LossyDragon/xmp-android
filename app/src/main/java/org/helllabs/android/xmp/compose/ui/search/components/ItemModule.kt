package org.helllabs.android.xmp.compose.ui.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.persistentListOf
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.Module

@Composable
fun ItemModule(
    modifier: Modifier = Modifier,
    item: Module,
    onClick: () -> Unit
) {
    ListItem(
        modifier = modifier.clickable { onClick() },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color(0xff404040),
                        RoundedCornerShape(2.dp)
                    )
                    .clip(RoundedCornerShape(2.dp))
                    .border(2.dp, Color(0xff808080)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier,
                    text = item.format,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        },
        headlineContent = {
            Text(
                text = item.songtitle,
                maxLines = 1,
                fontSize = 18.sp,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = item.artist,
                maxLines = 1,
                fontSize = 14.sp,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Text(
                text = stringResource(id = R.string.kb, item.byteSize),
                maxLines = 1,
                fontSize = 14.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Preview
@Composable
private fun Preview_ItemModule() {
    XmpTheme(useDarkTheme = true) {
        ItemModule(
            item = Module(
                format = "XM",
                songtitle = "Some History Song Title",
                artistInfo = ArtistInfo(
                    artist = persistentListOf(Artist(alias = "Some History Artist Info"))
                ),
                bytes = 6690000
            ),
            onClick = {}
        )
    }
}
