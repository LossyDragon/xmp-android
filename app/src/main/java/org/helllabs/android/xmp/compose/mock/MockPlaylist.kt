package org.helllabs.android.xmp.compose.mock

import androidx.core.net.toUri
import kotlinx.collections.immutable.persistentListOf
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem

val MockPlaylist = Playlist(
    name = "Retro Gaming Classics",
    comment = "A collection of classic tracker modules from the golden age of demoscene and video games",
    useFileName = false,
    isLoop = true,
    isShuffle = false,
    uri = "content://playlists/1".toUri(),
    list = persistentListOf(
        PlaylistItem(
            name = "Space Debris",
            type = "MOD",
            uri = "content://modules/1".toUri(),
            id = 0
        ),
        PlaylistItem(
            name = "Crystal Dream II",
            type = "S3M",
            uri = "content://modules/2".toUri(),
            id = 1
        ),
        PlaylistItem(
            name = "Beyond Music",
            type = "XM",
            uri = "content://modules/3".toUri(),
            id = 2
        ),
        PlaylistItem(
            name = "Stardust Memories",
            type = "IT",
            uri = "content://modules/4".toUri(),
            id = 3
        ),
        PlaylistItem(
            name = "Digital Emotions",
            type = "XM",
            uri = "content://modules/5".toUri(),
            id = 4
        ),
        PlaylistItem(
            name = "Forbidden Paradise",
            type = "S3M",
            uri = "content://modules/6".toUri(),
            id = 5
        ),
        PlaylistItem(
            name = "Mental Surgery",
            type = "MOD",
            uri = "content://modules/7".toUri(),
            id = 6
        ),
        PlaylistItem(
            name = "Chromium Dioxide",
            type = "IT",
            uri = "content://modules/8".toUri(),
            id = 7
        ),
        PlaylistItem(
            name = "Cyber Ride",
            type = "XM",
            uri = "content://modules/9".toUri(),
            id = 8
        ),
        PlaylistItem(
            name = "Elysium",
            type = "S3M",
            uri = "content://modules/10".toUri(),
            id = 9
        ),
        PlaylistItem(
            name = "Purple Motion",
            type = "MOD",
            uri = "content://modules/11".toUri(),
            id = 10
        ),
        PlaylistItem(
            name = "Techno Trance",
            type = "XM",
            uri = "content://modules/12".toUri(),
            id = 11
        ),
        PlaylistItem(
            name = "Ambient Power",
            type = "IT",
            uri = "content://modules/13".toUri(),
            id = 12
        ),
        PlaylistItem(
            name = "Desert Dream",
            type = "S3M",
            uri = "content://modules/14".toUri(),
            id = 13
        ),
        PlaylistItem(
            name = "Chip Symphony",
            type = "MOD",
            uri = "content://modules/15".toUri(),
            id = 14
        ),
        PlaylistItem(
            name = "Future Shock",
            type = "XM",
            uri = "content://modules/16".toUri(),
            id = 15
        ),
        PlaylistItem(
            name = "Ice Frontier",
            type = "IT",
            uri = "content://modules/17".toUri(),
            id = 16
        ),
        PlaylistItem(
            name = "Mega Bass",
            type = "S3M",
            uri = "content://modules/18".toUri(),
            id = 17
        ),
        PlaylistItem(
            name = "Ocean Loader",
            type = "MOD",
            uri = "content://modules/19".toUri(),
            id = 18
        ),
        PlaylistItem(
            name = "Rave Heaven",
            type = "XM",
            uri = "content://modules/20".toUri(),
            id = 19
        ),
        PlaylistItem(
            name = "Synthesis",
            type = "IT",
            uri = "content://modules/21".toUri(),
            id = 20
        ),
        PlaylistItem(
            name = "Virtual Reality",
            type = "S3M",
            uri = "content://modules/22".toUri(),
            id = 21
        ),
        PlaylistItem(
            name = "Wonderland",
            type = "XM",
            uri = "content://modules/23".toUri(),
            id = 22
        )
    )
)
