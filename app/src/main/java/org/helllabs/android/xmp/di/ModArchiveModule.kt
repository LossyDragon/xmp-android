package org.helllabs.android.xmp.di

import android.content.Context
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import org.helllabs.android.xmp.api.ApiHelper
import org.helllabs.android.xmp.api.ApiHelperImpl
import org.helllabs.android.xmp.api.ApiService
import org.helllabs.android.xmp.modarchive.ModArchiveConstants
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object ModArchiveModule {

    @Singleton
    @Provides
    fun provideOkHttpClient() = OkHttpClient.Builder().build()

    @Singleton
    @Provides
    fun provideFetch(@ApplicationContext context: Context, okHttpClient: OkHttpClient): Fetch {
        val fetchConfiguration = FetchConfiguration.Builder(context)
            .enableRetryOnNetworkGain(true)
            .setDownloadConcurrentLimit(1)
            .setHttpDownloader(OkHttpDownloader(okHttpClient))
            .build()

        return Fetch.Impl.getInstance(fetchConfiguration)
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(ModArchiveConstants.BASE_URL)
            .addConverterFactory(TikXmlConverterFactory.create())
            .client(okHttpClient)
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideApiHelper(apiHelper: ApiHelperImpl): ApiHelper = apiHelper
}
