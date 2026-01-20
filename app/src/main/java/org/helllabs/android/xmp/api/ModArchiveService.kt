package org.helllabs.android.xmp.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import nl.adaptivity.xmlutil.serialization.*
import org.helllabs.android.xmp.core.Constants.BY_ARTIST
import org.helllabs.android.xmp.core.Constants.BY_ARTIST_ID
import org.helllabs.android.xmp.core.Constants.BY_MODULE_ID
import org.helllabs.android.xmp.core.Constants.BY_RANDOM
import org.helllabs.android.xmp.core.Constants.BY_SEARCH
import org.helllabs.android.xmp.core.Constants.TYPE_FILE_OR_TITLE
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.SearchListResult

class ModArchiveService(private val client: HttpClient, private val apiKey: String) {

    private suspend inline fun <reified T> executeRequest(
        request: String,
        additionalParams: Map<String, Any?> = emptyMap()
    ): Result<T> {
        return try {
            val response = client.get("/xml-tools.php") {
                parameter("key", apiKey)
                parameter("request", request)
                additionalParams.forEach { (key, value) ->
                    if (value != null) parameter(key, value)
                }
            }.bodyAsText()

            val data: T = XML.decodeFromString(response)

            val error = when (data) {
                is ModuleResult -> data.error
                is SearchListResult -> data.error
                is ArtistResult -> data.error
                else -> null
            }

            if (!error.isNullOrEmpty()) {
                Result.failure(Exception(error))
            } else {
                Result.success(data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Search modules by an Artist's ID
    suspend fun getArtistById(query: Int): Result<SearchListResult> =
        executeRequest(BY_ARTIST_ID, mapOf("query" to query))

    // Search by Artist's name
    suspend fun getArtistSearch(query: String): Result<ArtistResult> =
        executeRequest(BY_ARTIST, mapOf("query" to query))

    // View a module ID
    suspend fun getModuleById(query: Int): Result<ModuleResult> =
        executeRequest(BY_MODULE_ID, mapOf("query" to query))

    // Search a random module
    suspend fun getRandomModule(): Result<ModuleResult> = executeRequest(BY_RANDOM)

    // Search by Filename or by Song title
    suspend fun getSearchByFileNameOrTitle(query: String): Result<SearchListResult> =
        executeRequest(BY_SEARCH, mapOf("type" to TYPE_FILE_OR_TITLE, "query" to query))
}
