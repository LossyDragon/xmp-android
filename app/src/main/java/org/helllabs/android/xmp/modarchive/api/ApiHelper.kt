package org.helllabs.android.xmp.modarchive.api

import org.helllabs.android.xmp.modarchive.model.ArtistResult
import org.helllabs.android.xmp.modarchive.model.ModuleResult
import org.helllabs.android.xmp.modarchive.model.SearchListResult

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
