package org.helllabs.android.xmp.di

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import java.io.File
import org.helllabs.android.xmp.model.Playlist

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    @ServiceScoped
    @Provides
    fun providePlaylistDir(@ApplicationContext context: Context): File {
        return context.getExternalFilesDir("playlists")!!
    }

    @ServiceScoped
    @Provides
    fun provideMoshiAdapter(): JsonAdapter<Playlist> {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        return moshi.adapter(Playlist::class.java)
    }
}
