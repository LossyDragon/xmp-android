@file:Suppress("ktlint:standard:filename")

package org.helllabs.android.xmp.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.xml.xml
import org.helllabs.android.xmp.api.ModArchiveService
import org.helllabs.android.xmp.core.Constants
import org.koin.dsl.module

val modArchiveModule = module {
    single {
        HttpClient(engineFactory = Android) {
            install(plugin = ContentNegotiation) { xml() }
            defaultRequest {
                url(urlString = Constants.BASE_URL)
                accept(contentType = ContentType.Application.Xml)
            }
        }
    }

    single { ModArchiveService(client = get(), apiKey = Constants.APIKEY) }
}
