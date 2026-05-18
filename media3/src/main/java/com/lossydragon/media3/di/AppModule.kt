package com.lossydragon.media3.di

import com.lossydragon.media3.BuildConfig
import com.lossydragon.media3.data.ModArchiveService
import com.lossydragon.media3.player.XmpEngine
import com.lossydragon.media3.player.XmpPlayer
import com.lossydragon.media3.player.XmpPlayerViewModel
import com.lossydragon.media3.ui.browser.FileBrowserViewModel
import com.lossydragon.media3.ui.downloads.DownloadViewModel
import com.lossydragon.media3.ui.downloads.ModuleResultViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.xml.xml
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { DownloadViewModel(get()) }
    viewModel { FileBrowserViewModel(androidContext()) }
    viewModel { ModuleResultViewModel(androidContext(), get(), get()) }
    viewModel { XmpPlayerViewModel(androidContext(), get()) }

    single { ModArchiveService(get(), BuildConfig.API_KEY) }
    single { XmpEngine(androidContext()) }
    single { XmpPlayer(androidContext(), get()) }
    single {
        HttpClient(engineFactory = Android) {
            install(ContentNegotiation) { xml() }
            defaultRequest {
                url("https://api.modarchive.org")
                accept(ContentType.Application.Xml)
            }
        }
    }
}
