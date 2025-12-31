package org.helllabs.android.xmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.helllabs.android.xmp.compose.RootNavigation
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.core.setEdgeToEdgeConfig
import org.helllabs.android.xmp.di.appModule
import org.helllabs.android.xmp.di.modArchiveModule
import org.helllabs.android.xmp.di.viewModelModule
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)

        setContent {
            XmpTheme {
                KoinApplication(
                    configuration = koinConfiguration {
                        val list = listOf(
                            viewModelModule,
                            modArchiveModule,
                            appModule
                        )
                        modules(list)
                    },
                    content = { RootNavigation() }
                )
            }
        }
    }
}
