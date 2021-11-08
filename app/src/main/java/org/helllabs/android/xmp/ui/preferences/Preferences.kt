package org.helllabs.android.xmp.ui.preferences

import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.os.Environment.MEDIA_MOUNTED
import android.os.Environment.MEDIA_MOUNTED_READ_ONLY
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.insets.systemBarsPadding
import java.io.File
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.util.logE

class Preferences : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProvideWindowInsets {
                val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

                XmpTheme3 {
                    Scaffold(
                        topBar = {
                            val rotation = LocalConfiguration.current.orientation
                            val appBarModifier =
                                if (rotation == Configuration.ORIENTATION_PORTRAIT)
                                    Modifier.statusBarsPadding()
                                else Modifier.systemBarsPadding()

                            XmpAppBar3(
                                modifier = appBarModifier,
                                scrollBehavior = scrollBehavior,
                                onNavIconPressed = { onBackPressed() },
                                titleText = stringResource(id = R.string.pref_category_preferences),
                            )
                        }
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
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
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        @Suppress("DEPRECATION") // Not using SAF yet
        private val SD_DIR: File = Environment.getExternalStorageDirectory()

        val DATA_DIR = File(SD_DIR, "Xmp for Android")
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
