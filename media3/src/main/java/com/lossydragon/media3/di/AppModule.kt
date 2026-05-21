package com.lossydragon.media3.di

import androidx.room.Room
import com.lossydragon.media3.core.Constants
import com.lossydragon.media3.data.DownloadHistoryRepository
import com.lossydragon.media3.data.ModArchiveService
import com.lossydragon.media3.data.ModuleMetadataRepository
import com.lossydragon.media3.db.XmpDatabase
import com.lossydragon.media3.db.XmpPreferences
import com.lossydragon.media3.player.XmpEngine
import com.lossydragon.media3.player.XmpPlayer
import com.lossydragon.media3.player.XmpPlayerViewModel
import com.lossydragon.media3.ui.screens.browser.FileBrowserViewModel
import com.lossydragon.media3.ui.screens.downloads.viewmodel.DownloadHistoryViewModel
import com.lossydragon.media3.ui.screens.downloads.viewmodel.DownloadViewModel
import com.lossydragon.media3.ui.screens.downloads.viewmodel.ModuleResultViewModel
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
    // ViewModels
    viewModel { DownloadViewModel(get()) }
    viewModel { FileBrowserViewModel(androidContext(), get(), get()) }
    viewModel { ModuleResultViewModel(androidContext(), get(), get(), get(), get()) }
    viewModel { XmpPlayerViewModel(androidContext(), get()) }
    viewModel { DownloadHistoryViewModel(get()) }

    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            XmpDatabase::class.java,
            Constants.ROOM_DATABASE_NAME,
        ).fallbackToDestructiveMigration(true).build()
    }
    single { get<XmpDatabase>().moduleMetadataDao() }
    single { get<XmpDatabase>().downloadHistoryDao() }
    single { ModuleMetadataRepository(androidContext(), get()) }
    single { DownloadHistoryRepository(get()) }

    // Downloads
    single {
        HttpClient(engineFactory = Android) {
            install(ContentNegotiation) { xml() }
            defaultRequest {
                url(Constants.HTTP_BASE_URL)
                accept(ContentType.Application.Xml)
            }
        }
    }
    single { ModArchiveService(get()) }

    // Media Player
    single { XmpEngine(androidContext()) }
    single { XmpPlayer(androidContext(), get(), get()) }
    single { XmpPreferences(androidContext()) }
}
