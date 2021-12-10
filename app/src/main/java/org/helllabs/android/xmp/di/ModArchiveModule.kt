package org.helllabs.android.xmp.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.serialization.ExperimentalSerializationApi
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.helllabs.android.xmp.api.ApiHelper
import org.helllabs.android.xmp.api.ApiHelperImpl
import org.helllabs.android.xmp.api.ApiService
import org.helllabs.android.xmp.ui.search.ModArchiveConstants
import retrofit2.Retrofit

@Module
@InstallIn(ViewModelComponent::class)
object ModArchiveModule {

    @ViewModelScoped
    @Provides
    fun provideOkHttpClient() = OkHttpClient.Builder().build()

    @ViewModelScoped
    @Provides
    fun provideFetch(@ApplicationContext context: Context, okHttpClient: OkHttpClient): Fetch {
        val fetchConfiguration = FetchConfiguration.Builder(context)
            .enableRetryOnNetworkGain(true)
            .setDownloadConcurrentLimit(1)
            .setHttpDownloader(OkHttpDownloader(okHttpClient))
            .build()

        return Fetch.Impl.getInstance(fetchConfiguration)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @ViewModelScoped
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/xml;".toMediaType()
        return Retrofit.Builder()
            .baseUrl(ModArchiveConstants.BASE_URL)
            .addConverterFactory(
                XML {
                    autoPolymorphic = true
                }.asConverterFactory(contentType)
            )
            .client(okHttpClient)
            .build()
    }

    @ViewModelScoped
    @Provides
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @ViewModelScoped
    @Provides
    fun provideApiHelper(apiHelper: ApiHelperImpl): ApiHelper = apiHelper

    @ViewModelScoped
    @Provides
    fun provideMoshiAdapter(): JsonAdapter<List<org.helllabs.android.xmp.model.Module>> {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val listData = Types.newParameterizedType(
            MutableList::class.java,
            org.helllabs.android.xmp.model.Module::class.java
        )

        return moshi.adapter(listData)
    }
}
