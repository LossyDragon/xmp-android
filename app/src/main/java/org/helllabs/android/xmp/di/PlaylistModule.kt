package org.helllabs.android.xmp.di

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.File
import org.helllabs.android.xmp.model.Playlist

@Module
@InstallIn(ViewModelComponent::class)
object PlaylistModule {

    @ViewModelScoped
    @Provides
    fun providePlaylistDir(@ApplicationContext context: Context): File? {
        return context.getExternalFilesDir("playlists")
    }

    @ViewModelScoped
    @Provides
    fun provideMoshiAdapter(): JsonAdapter<Playlist> {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        return moshi.adapter(Playlist::class.java)
    }
}
