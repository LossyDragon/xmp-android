package org.helllabs.android.xmp.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.xml.xml
import org.helllabs.android.xmp.api.ApiHelper
import org.helllabs.android.xmp.api.ApiHelperImpl
import org.helllabs.android.xmp.api.ApiService
import org.helllabs.android.xmp.core.Constants

interface ModArchiveModule {
    val httpClient: HttpClient
    val apiService: ApiService
    val apiHelper: ApiHelper
}

class ModArchiveModuleImpl : ModArchiveModule {
    override val httpClient: HttpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                xml()
            }
            defaultRequest {
                url(Constants.BASE_URL)
                accept(ContentType.Application.Xml)
            }
        }
    }

    override val apiService: ApiService by lazy { ApiService(httpClient) }

    override val apiHelper: ApiHelper by lazy { ApiHelperImpl(apiService) }
}
