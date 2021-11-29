package org.helllabs.android.xmp.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.helllabs.android.xmp.util.PrefTheme
import org.helllabs.android.xmp.util.PrefThemeImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindPrefTheme(
        userSettingsImpl: PrefThemeImpl
    ): PrefTheme
}
