package org.helllabs.android.xmp.api

import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.SearchListResult
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    // Search modules by an Artist's ID.
    @GET("/xml-tools.php")
    suspend fun getArtistById(
        @Query("key") apiKey: String,
        @Query("request") request: String,
        @Query("query") query: Int
    ): SearchListResult

    // Search by Artist's name.
    @GET("/xml-tools.php")
    suspend fun getArtistSearch(
        @Query("key") apiKey: String,
        @Query("request") request: String,
        @Query("query") query: String
    ): ArtistResult

    // View a module ID.
    @GET("/xml-tools.php")
    suspend fun getModuleById(
        @Query("key") apiKey: String,
        @Query("request") request: String,
        @Query("query") query: Int
    ): ModuleResult

    // Search a random module.
    @GET("/xml-tools.php")
    suspend fun getRandomModule(
        @Query("key") apiKey: String,
        @Query("request") request: String
    ): ModuleResult

    // Search by Filename or by Song title.
    @GET("/xml-tools.php")
    suspend fun getSearchByFileNameOrTitle(
        @Query("key") apiKey: String,
        @Query("request") request: String,
        @Query("type") type: String,
        @Query("query") query: String
    ): SearchListResult
}
