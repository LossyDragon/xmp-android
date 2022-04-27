package org.helllabs.modarchive.api

import javax.inject.Inject
import org.helllabs.modarchive.Constants.BY_ARTIST
import org.helllabs.modarchive.Constants.BY_ARTIST_ID
import org.helllabs.modarchive.Constants.BY_MODULE_ID
import org.helllabs.modarchive.Constants.BY_RANDOM
import org.helllabs.modarchive.Constants.BY_SEARCH
import org.helllabs.modarchive.Constants.TYPE_FILE_OR_TITLE

class Repository @Inject constructor(private val apiHelper: ApiHelper) {

    suspend fun getModuleById(apiKey: String, query: Int) =
        apiHelper.getModuleById(apiKey, BY_MODULE_ID, query)

    suspend fun getArtistSearch(apiKey: String, query: String) =
        apiHelper.getArtistSearch(apiKey, BY_ARTIST, query)

    suspend fun getArtistById(apiKey: String, query: Int) =
        apiHelper.getArtistById(apiKey, BY_ARTIST_ID, query)

    suspend fun getFileNameOrTitle(apiKey: String, query: String) =
        apiHelper.getSearchByFileNameOrTitle(apiKey, BY_SEARCH, TYPE_FILE_OR_TITLE, query)

    suspend fun getRandomModule(apiKey: String) =
        apiHelper.getRandomModule(apiKey, BY_RANDOM)
}
