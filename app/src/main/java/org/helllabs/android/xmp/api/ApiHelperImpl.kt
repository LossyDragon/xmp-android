package org.helllabs.android.xmp.api

import kotlinx.coroutines.flow.Flow
import org.helllabs.android.xmp.core.Resource
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.SearchListResult

class ApiHelperImpl(private val apiService: ApiService) : ApiHelper {
    override suspend fun getArtistById(
        apiKey: String,
        request: String,
        query: Int
    ): Flow<Resource<SearchListResult>> =
        apiService.getArtistById(apiKey, request, query)

    override suspend fun getArtistSearch(
        apiKey: String,
        request: String,
        query: String
    ): Flow<Resource<ArtistResult>> =
        apiService.getArtistSearch(apiKey, request, query)

    override suspend fun getModuleById(
        apiKey: String,
        request: String,
        query: Int
    ): Flow<Resource<ModuleResult>> =
        apiService.getModuleById(apiKey, request, query)

    override suspend fun getRandomModule(
        apiKey: String,
        request: String
    ): Flow<Resource<ModuleResult>> =
        apiService.getRandomModule(apiKey, request)

    override suspend fun getSearchByFileNameOrTitle(
        apiKey: String,
        request: String,
        type: String,
        query: String
    ): Flow<Resource<SearchListResult>> =
        apiService.getSearchByFileNameOrTitle(apiKey, request, type, query)
}
