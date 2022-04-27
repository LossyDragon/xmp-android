package org.helllabs.modarchive.api

import org.helllabs.modarchive.model.ArtistResult
import org.helllabs.modarchive.model.ModuleResult
import org.helllabs.modarchive.model.SearchListResult

interface ApiHelper {

    suspend fun getArtistSearch(apiKey: String, byArtist: String, query: String): ArtistResult

    suspend fun getModuleById(apiKey: String, byModuleId: String, query: Int): ModuleResult

    suspend fun getRandomModule(apiKey: String, byRandom: String): ModuleResult

    suspend fun getArtistById(apiKey: String, byArtistId: String, query: Int): SearchListResult

    suspend fun getSearchByFileNameOrTitle(
        apiKey: String,
        bySearch: String,
        typeFileOrTitle: String,
        query: String
    ): SearchListResult
}
