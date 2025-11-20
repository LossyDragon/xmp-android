package org.helllabs.android.xmp.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import nl.adaptivity.xmlutil.serialization.*
import org.helllabs.android.xmp.core.Resource
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.SearchListResult

class ApiService(private val client: HttpClient, private val apiKey: String) {

    private inline fun <reified T> executeRequest(
        request: String,
        additionalParams: Map<String, Any?> = emptyMap()
    ): Flow<Resource<T>> = flow {
        emit(Resource.Loading())

        try {
            val response = client.get("/xml-tools.php") {
                parameter("key", apiKey)
                parameter("request", request)

                // Add any additional parameters
                additionalParams.forEach { (key, value) ->
                    if (value != null) {
                        parameter(key, value)
                    }
                }
            }.bodyAsText()

            // Check API error response
            when (val data: T = XML.decodeFromString(response)) {
                is ModuleResult if !data.error.isNullOrEmpty() ->
                    emit(Resource.Error(data.error))

                is SearchListResult if !data.error.isNullOrEmpty() ->
                    emit(Resource.Error(data.error))

                is ArtistResult if !data.error.isNullOrEmpty() ->
                    emit(Resource.Error(data.error))

                else -> emit(Resource.Success(data))
            }
        } catch (e: ClientRequestException) {
            emit(Resource.Error("Network error: ${e.response.status.description}"))
        } catch (e: ServerResponseException) {
            emit(Resource.Error("Server error: ${e.response.status.description}"))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    // Search modules by an Artist's ID
    fun getArtistById(
        request: String,
        query: Int
    ): Flow<Resource<SearchListResult>> = executeRequest(request, mapOf("query" to query))

    // Search by Artist's name
    fun getArtistSearch(
        request: String,
        query: String
    ): Flow<Resource<ArtistResult>> = executeRequest(request, mapOf("query" to query))

    // View a module ID
    fun getModuleById(
        request: String,
        query: Int
    ): Flow<Resource<ModuleResult>> = executeRequest(request, mapOf("query" to query))

    // Search a random module
    fun getRandomModule(
        request: String
    ): Flow<Resource<ModuleResult>> = executeRequest(request)

    // Search by Filename or by Song title
    fun getSearchByFileNameOrTitle(
        request: String,
        type: String,
        query: String
    ): Flow<Resource<SearchListResult>> = executeRequest(
        request,
        mapOf("type" to type, "query" to query)
    )
}
