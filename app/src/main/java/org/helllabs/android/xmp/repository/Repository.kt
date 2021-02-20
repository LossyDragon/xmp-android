package org.helllabs.android.xmp.repository

import javax.inject.Inject
import org.helllabs.android.xmp.api.ApiHelper
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.BY_ARTIST
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.BY_ARTIST_ID
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.BY_MODULE_ID
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.BY_RANDOM
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.BY_SEARCH
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.TYPE_FILE_OR_TITLE
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.apiKey

class Repository @Inject constructor(private val apiHelper: ApiHelper) {

    suspend fun getModuleById(query: Int) =
        apiHelper.getModuleById(apiKey, BY_MODULE_ID, query)

    suspend fun getArtistSearch(query: String) =
        apiHelper.getArtistSearch(apiKey, BY_ARTIST, query)

    suspend fun getArtistById(query: Int) =
        apiHelper.getArtistById(apiKey, BY_ARTIST_ID, query)

    suspend fun getFileNameOrTitle(query: String) =
        apiHelper.getSearchByFileNameOrTitle(apiKey, BY_SEARCH, TYPE_FILE_OR_TITLE, query)

    suspend fun getRandomModule() =
        apiHelper.getRandomModule(apiKey, BY_RANDOM)
}
