package org.helllabs.android.xmp.ui.modarchive

import org.helllabs.android.xmp.BuildConfig

object ModArchiveConstants {
    const val apiKey: String = BuildConfig.API_KEY

    const val BASE_URL: String = "https://api.modarchive.org"
    const val BY_ARTIST: String = "search_artist"
    const val BY_ARTIST_ID: String = "view_modules_by_artistid"
    const val BY_MODULE_ID: String = "view_by_moduleid"
    const val BY_RANDOM: String = "random"
    const val BY_SEARCH: String = "search"
    const val TYPE_FILE_OR_TITLE = "filename_or_songtitle"

    const val SEARCH_TEXT = "search_text"
    const val MODULE_ID = "module_id"
    const val ARTIST_ID = "artist_id"
    const val ERROR = "error"
}
