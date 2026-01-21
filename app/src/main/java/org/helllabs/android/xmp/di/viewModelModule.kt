@file:Suppress("ktlint:standard:filename")

package org.helllabs.android.xmp.di

import android.net.Uri
import org.helllabs.android.xmp.core.FileManager
import org.helllabs.android.xmp.ui.screens.explorer.ExplorerViewModel
import org.helllabs.android.xmp.ui.screens.player.PlayerViewModel
import org.helllabs.android.xmp.ui.screens.playlist.viewmodel.PlaylistsViewModel
import org.helllabs.android.xmp.ui.screens.playlist.viewmodel.SelectedPlaylistViewModel
import org.helllabs.android.xmp.ui.screens.search.viewmodel.ResultViewModel
import org.helllabs.android.xmp.ui.screens.search.viewmodel.SearchResultViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { FileManager(context = get()) }

    viewModel {
        PlaylistsViewModel(
            playlistManager = get(),
            prefManager = get()
        )
    }

    viewModel { (uri: Uri) ->
        SelectedPlaylistViewModel(
            playlistUri = uri,
            playlistManager = get(),
            prefManager = get()
        )
    }

    viewModel {
        ExplorerViewModel(
            playlistManager = get(),
            prefManager = get(),
            storageManager = get()
        )
    }

    viewModel { SearchResultViewModel(modArchive = get()) }

    viewModel {
        ResultViewModel(
            httpClient = get(),
            modArchive = get(),
            storageManager = get(),
            fileManager = get(),
            prefManager = get()
        )
    }

    viewModel { PlayerViewModel(prefManager = get()) }
}
