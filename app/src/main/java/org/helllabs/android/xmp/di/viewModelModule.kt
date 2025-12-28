@file:Suppress("ktlint:standard:filename")

package org.helllabs.android.xmp.di

import org.helllabs.android.xmp.compose.ui.home.PlaylistMenuViewModel
import org.helllabs.android.xmp.compose.ui.search.viewmodel.ResultViewModel
import org.helllabs.android.xmp.compose.ui.search.viewmodel.SearchResultViewModel
import org.helllabs.android.xmp.core.FileManager
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        ResultViewModel(
            httpClient = get(),
            repository = get(),
            storageManager = get(),
            fileManager = get(),
            prefManager = get()
        )
    }
    viewModel {
        PlaylistMenuViewModel(
            storageManager = get(),
            prefManager = get(),
            playlistManager = get()
        )
    }
    viewModel { SearchResultViewModel(get()) }

    single { FileManager(context = get()) }
}
