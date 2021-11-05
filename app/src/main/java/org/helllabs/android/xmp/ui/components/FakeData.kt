package org.helllabs.android.xmp.ui.components

import androidx.compose.ui.text.buildAnnotatedString
import org.helllabs.android.xmp.model.*

fun fakeBreadCrumbData(): List<BreadCrumb> {
    val list = mutableListOf<BreadCrumb>()
    repeat(20) {
        list.add(
            BreadCrumb("Path $it", "")
        )
    }

    return list
}

fun fakeExplorerData(): List<PlaylistItem> {
    val list = mutableListOf<PlaylistItem>()
    repeat(20) {
        list.add(
            PlaylistItem(
                type = if (it % 2 == 0) PlaylistType.TYPE_FILE else PlaylistType.TYPE_DIRECTORY,
                name = "Item $it",
                comment = "Comment $it",
                id = it,
                file = null,
            )
        )
    }

    return list
}

fun fakeDataPlaylistDetail(): List<PlaylistItem> {
    val list = mutableListOf<PlaylistItem>()
    repeat(20) {
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

fun fakeDataPlaylistMenu(): List<PlaylistItem> {
    val list = mutableListOf<PlaylistItem>()
    repeat(20) {
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
