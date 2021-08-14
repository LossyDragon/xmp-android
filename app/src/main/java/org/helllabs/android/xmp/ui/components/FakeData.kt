package org.helllabs.android.xmp.ui.components

import androidx.compose.ui.text.buildAnnotatedString
import org.helllabs.android.xmp.model.*

fun fakeDataPlaylistMenu(): List<PlaylistItem> {
    val list = mutableListOf<PlaylistItem>()
    repeat(50) {
        list.add(
            PlaylistItem(
                type = PlaylistType.TYPE_PLAYLIST,
                name = "Playlist $it",
                comment = "Comment $it",
                id = it,
                file = null,
            )
        )
    }

    return list
}

fun fakeModuleResult(): ModuleResult {
    val instruments = buildAnnotatedString {
        repeat(20) {
            append("Some Instrument $it\n")
        }
    }
    return ModuleResult(
        sponsor = Sponsor(
            details = SponsorDetails(
                link = "",
                text = "Some Sponsor Text"
            )
        ),
        module = Module(
            filename = "",
            bytes = 669669,
            format = "XM",
            artistInfo = ArtistInfo(artist = Artist(alias = "Some Artist")),
            infopage = "",
            license = License(
                title = "Some License Title",
                legalurl = "",
                description = "Some License Description"
            ),
            comment = "Some Comment",
            instruments = instruments.toString(),
        )
    )
}
