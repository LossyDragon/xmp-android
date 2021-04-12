package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.utils.internalTextGenerator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemModule(
    item: Module,
    onClick: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .height(72.dp)
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        val (icon, size, title, artist) = createRefs()
        Box(
            modifier = Modifier
                .constrainAs(icon) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start, 16.dp)
                }
                .size(40.dp)
                .background(
                    Color(0xff404040),
                    RoundedCornerShape(2.dp)
                )
                .clip(RoundedCornerShape(2.dp))
                .border(2.dp, Color(0xff808080)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier,
                text = item.format!!,
                fontSize = 12.sp,
                color = Color.White
            )
        }
        Text(
            modifier = Modifier.constrainAs(title) {
                width = Dimension.fillToConstraints
                top.linkTo(icon.top)
                bottom.linkTo(artist.top)
                start.linkTo(icon.end, 16.dp)
                end.linkTo(size.start, 16.dp)
            },
            text = item.getSongTitle(),
            maxLines = 1,
            fontSize = 18.sp,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier.constrainAs(artist) {
                width = Dimension.fillToConstraints
                top.linkTo(title.bottom)
                bottom.linkTo(icon.bottom)
                start.linkTo(icon.end, 16.dp)
                end.linkTo(size.start)
            },
            text = item.getArtist(),
            maxLines = 1,
            fontSize = 14.sp,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.constrainAs(size) {
                top.linkTo(artist.top)
                bottom.linkTo(artist.bottom)
                start.linkTo(artist.end, 16.dp)
                end.linkTo(parent.end, 16.dp)
            },
            text = stringResource(id = R.string.size_kb, item.getBytesFormatted()),
            maxLines = 1,
            fontSize = 14.sp,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
fun ItemModulePreview() {
    AppTheme(false) {
        ItemModule(
            item = Module(
                format = "XM",
                songtitle = internalTextGenerator(),
                artistInfo = ArtistInfo(artist = Artist(alias = internalTextGenerator())),
                bytes = 6690000
            )
        ) {}
    }
}

@Preview
@Composable
fun ItemModulePreviewDark() {
    AppTheme(true) {
        ItemModule(
            item = Module(
                format = "XM",
                songtitle = internalTextGenerator(),
                artistInfo = ArtistInfo(artist = Artist(alias = internalTextGenerator())),
                bytes = 6690000
            )
        ) {}
    }
}
