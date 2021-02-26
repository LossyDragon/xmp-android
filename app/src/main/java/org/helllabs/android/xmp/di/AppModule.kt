package org.helllabs.android.xmp.di

import android.content.ClipboardManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.greenrobot.eventbus.EventBus

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Singleton
    @Provides
    fun provideEventBus(): EventBus = EventBus.getDefault()

    @Singleton
    @Provides
    fun provideClipService(@ApplicationContext context: Context): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
}
