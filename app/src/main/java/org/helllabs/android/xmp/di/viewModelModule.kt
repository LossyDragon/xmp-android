@file:Suppress("ktlint:standard:filename")

package org.helllabs.android.xmp.di

import android.net.Uri
import org.helllabs.android.xmp.compose.ui.explorer.ExplorerViewModel
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.PlaylistsViewModel
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.SelectedPlaylistViewModel
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
        PlaylistsViewModel(
            storageManager = get(),
            prefManager = get(),
            playlistManager = get()
        )
    }

    viewModel { ExplorerViewModel(get(), get(), get()) }

    viewModel { SearchResultViewModel(get()) }

    viewModel { (uri: Uri) ->
        SelectedPlaylistViewModel(
            playlistUri = uri,
            playlistManager = get(),
            prefManager = get()
        )
    }

    single { FileManager(context = get()) }
}
