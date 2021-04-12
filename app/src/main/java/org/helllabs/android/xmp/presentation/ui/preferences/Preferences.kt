package org.helllabs.android.xmp.presentation.ui.preferences

import android.os.Bundle
import android.os.Environment
import android.os.Environment.MEDIA_MOUNTED
import android.os.Environment.MEDIA_MOUNTED_READ_ONLY
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.presentation.components.AppBar
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.util.logE

class Preferences : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme(
                isDarkTheme = isSystemInDarkTheme(),
            ) {
                Scaffold(
                    topBar = {
                        AppBar(
                            title = stringResource(id = R.string.pref_category_preferences),
                            navIconClick = { onBackPressed() },
                            isCompatView = true,
                        )
                    }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            FrameLayout(context).apply {
                                id = R.id.composeFrameLayout
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }.also {
                                supportFragmentManager
                                    .beginTransaction()
                                    .replace(R.id.composeFrameLayout, PreferencesFragment())
                                    .commit()
                            }
                        },
                    )
                }
            }
        }
    }

    companion object {
        @Suppress("DEPRECATION") // Not using SAF yet
        private val SD_DIR: File = Environment.getExternalStorageDirectory()

        val DATA_DIR = File(SD_DIR, "Xmp for Android")
        val CACHE_DIR = File(SD_DIR, "Android/data/org.helllabs.android.xmp/cache/")
        val DEFAULT_MEDIA_PATH = "$SD_DIR/mod"

        fun checkStorage(): Boolean {
            val state = Environment.getExternalStorageState()
            return if (MEDIA_MOUNTED == state || MEDIA_MOUNTED_READ_ONLY == state) {
                true
            } else {
                logE("External storage state error: $state")
                false
            }
        }
    }
}
