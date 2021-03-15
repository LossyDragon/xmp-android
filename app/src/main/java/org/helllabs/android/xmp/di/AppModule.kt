package org.helllabs.android.xmp.di

import android.content.ClipboardManager
import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import org.greenrobot.eventbus.EventBus
import org.helllabs.android.xmp.model.Module as XmpModule

@Module
@InstallIn(ActivityComponent::class)
object AppModule {

    @ActivityScoped
    @Provides
    fun provideEventBus(): EventBus = EventBus.getDefault()

    @ActivityScoped
    @Provides
    fun provideClipService(@ApplicationContext context: Context): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @ActivityScoped
    @Provides
    fun provideMoshiAdapter(): JsonAdapter<List<XmpModule>> {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val listData = Types.newParameterizedType(
            MutableList::class.java,
            XmpModule::class.java
        )

        return moshi.adapter(listData)
    }
}
