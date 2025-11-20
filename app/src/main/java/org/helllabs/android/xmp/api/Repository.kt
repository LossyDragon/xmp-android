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

class Repository(private val apiService: ApiService) {

    fun getModuleById(query: Int): Flow<Resource<ModuleResult>> =
        apiService.getModuleById(BY_MODULE_ID, query)

    fun getArtistSearch(query: String): Flow<Resource<ArtistResult>> =
        apiService.getArtistSearch(BY_ARTIST, query)

    fun getArtistById(query: Int): Flow<Resource<SearchListResult>> =
        apiService.getArtistById(BY_ARTIST_ID, query)

    fun getFileNameOrTitle(query: String): Flow<Resource<SearchListResult>> =
        apiService.getSearchByFileNameOrTitle(BY_SEARCH, TYPE_FILE_OR_TITLE, query)

    fun getRandomModule(): Flow<Resource<ModuleResult>> =
        apiService.getRandomModule(BY_RANDOM)
}
