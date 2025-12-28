@file:Suppress("ktlint:standard:filename")

package org.helllabs.android.xmp.di

import org.helllabs.android.xmp.core.PlaylistManager
import org.koin.dsl.module

val playlistModule = module {
    factory { PlaylistManager(context = get(), json = get(), storageManager = get()) }
}
