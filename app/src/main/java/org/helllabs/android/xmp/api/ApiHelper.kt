package org.helllabs.android.xmp.api

import kotlinx.coroutines.flow.Flow
import org.helllabs.android.xmp.core.Resource
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.SearchListResult

interface ApiHelper {

    suspend fun getArtistSearch(
        apiKey: String,
        request: String,
        query: String
    ): Flow<Resource<ArtistResult>>

    suspend fun getModuleById(
        apiKey: String,
        request: String,
        query: Int
    ): Flow<Resource<ModuleResult>>

    suspend fun getRandomModule(
        apiKey: String,
        request: String
    ): Flow<Resource<ModuleResult>>

    suspend fun getArtistById(
        apiKey: String,
        request: String,
        query: Int
    ): Flow<Resource<SearchListResult>>

    suspend fun getSearchByFileNameOrTitle(
        apiKey: String,
        request: String,
        type: String,
        query: String
    ): Flow<Resource<SearchListResult>>
}
