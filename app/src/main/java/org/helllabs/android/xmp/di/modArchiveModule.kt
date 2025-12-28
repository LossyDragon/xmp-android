@file:Suppress("ktlint:standard:filename")

package org.helllabs.android.xmp.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.xml.xml
import org.helllabs.android.xmp.api.ApiService
import org.helllabs.android.xmp.api.Repository
import org.helllabs.android.xmp.core.Constants
import org.koin.dsl.module

val modArchiveModule = module {
    single {
        HttpClient(Android) {
            install(ContentNegotiation) { xml() }
            defaultRequest {
                url(Constants.BASE_URL)
                accept(ContentType.Application.Xml)
            }
        }
    }

    single { ApiService(get(), Constants.APIKEY) }

    single { Repository(get()) }
}
