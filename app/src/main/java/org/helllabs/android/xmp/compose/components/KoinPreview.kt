package org.helllabs.android.xmp.compose.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.*
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinConfiguration

@Composable
fun KoinPreview(
    modules: List<Module> = listOf(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current.applicationContext
    CompositionLocalProvider(LocalInspectionMode provides true) {
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
