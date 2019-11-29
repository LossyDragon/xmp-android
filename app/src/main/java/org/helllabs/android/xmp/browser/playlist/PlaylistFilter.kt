package org.helllabs.android.xmp.browser.playlist

import java.io.File
import java.io.FilenameFilter

internal class PlaylistFilter : FilenameFilter {
    override fun accept(dir: File, name: String): Boolean {
        return name.endsWith(Playlist.PLAYLIST_SUFFIX)
    }
}
