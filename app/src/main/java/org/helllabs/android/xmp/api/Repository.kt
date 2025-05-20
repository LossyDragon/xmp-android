package org.helllabs.android.xmp.api

import kotlinx.coroutines.flow.Flow
import org.helllabs.android.xmp.core.Constants.APIKEY
import org.helllabs.android.xmp.core.Constants.BY_ARTIST
import org.helllabs.android.xmp.core.Constants.BY_ARTIST_ID
import org.helllabs.android.xmp.core.Constants.BY_MODULE_ID
import org.helllabs.android.xmp.core.Constants.BY_RANDOM
import org.helllabs.android.xmp.core.Constants.BY_SEARCH
import org.helllabs.android.xmp.core.Constants.TYPE_FILE_OR_TITLE
import org.helllabs.android.xmp.core.Resource
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.SearchListResult

class Repository(private val apiHelper: ApiHelper) {

    suspend fun getModuleById(query: Int): Flow<Resource<ModuleResult>> =
        apiHelper.getModuleById(APIKEY, BY_MODULE_ID, query)

    suspend fun getArtistSearch(query: String): Flow<Resource<ArtistResult>> =
        apiHelper.getArtistSearch(APIKEY, BY_ARTIST, query)

    suspend fun getArtistById(query: Int): Flow<Resource<SearchListResult>> =
        apiHelper.getArtistById(APIKEY, BY_ARTIST_ID, query)

    suspend fun getFileNameOrTitle(query: String): Flow<Resource<SearchListResult>> =
        apiHelper.getSearchByFileNameOrTitle(APIKEY, BY_SEARCH, TYPE_FILE_OR_TITLE, query)

    suspend fun getRandomModule(): Flow<Resource<ModuleResult>> =
        apiHelper.getRandomModule(APIKEY, BY_RANDOM)
}
