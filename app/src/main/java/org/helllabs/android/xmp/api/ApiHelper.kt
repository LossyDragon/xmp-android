package org.helllabs.android.xmp.api

import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.SearchListResult

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
