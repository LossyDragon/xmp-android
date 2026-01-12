package org.helllabs.android.xmp.compose.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.*
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.di.appModule
import org.helllabs.android.xmp.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinConfiguration

@Suppress("ParamsComparedByRef")
@Composable
fun KoinPreview(
    modules: List<Module> = listOf(appModule, viewModelModule),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    require(view.isInEditMode)

    val context = LocalContext.current.applicationContext
    CompositionLocalProvider(
        LocalInspectionMode provides true,
        LocalNavigationEventDispatcherOwner provides object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = NavigationEventDispatcher()
        }
    ) {
        KoinApplication(
            configuration = koinConfiguration(
                declaration = {
                    androidContext(context)
                    modules(modules)
                }
            ),
            content = {
                XmpTheme(
                    content = {
                        content()
                    }
                )
            }
        )
    }
}
