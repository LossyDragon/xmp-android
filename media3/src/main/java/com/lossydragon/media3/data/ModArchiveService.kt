package com.lossydragon.media3.data

import com.lossydragon.media3.model.ArtistResult
import com.lossydragon.media3.model.ModuleResult
import com.lossydragon.media3.model.SearchListResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import nl.adaptivity.xmlutil.serialization.XML

class ModArchiveService(
    private val client: HttpClient,
    private val apiKey: String
) {

    private suspend inline fun <reified T> executeRequest(
        request: String,
        additionalParams: Map<String, Any?> = emptyMap()
    ): Result<T> = runCatching {
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

        if (!error.isNullOrEmpty()) throw Exception(error)

        data
    }

    suspend fun getArtistById(id: Int): Result<SearchListResult> =
        executeRequest(
            request = "view_modules_by_artistid",
            additionalParams = mapOf("query" to id)
        )

    suspend fun getArtistSearch(query: String): Result<ArtistResult> =
        executeRequest(
            request = "search_artist",
            additionalParams = mapOf("query" to query)
        )

    suspend fun getModuleById(id: Int): Result<ModuleResult> =
        executeRequest(
            request = "view_by_moduleid",
            additionalParams = mapOf("query" to id)
        )

    suspend fun getRandomModule(): Result<ModuleResult> =
        executeRequest(
            request = "random"
        )

    suspend fun searchByFileNameOrTitle(query: String): Result<SearchListResult> =
        executeRequest(
            request = "search",
            additionalParams = mapOf("type" to "filename_or_songtitle", "query" to query)
        )
}
