package org.helllabs.android.xmp.ui.preferences.about

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.ui.components.ErrorLayout
import org.helllabs.android.xmp.ui.components.LazyList
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.toast

class ListFormats : ComponentActivity() {

    private val formats = Xmp.getFormats()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Sort alphabetically
        formats.sort()

        logD("onCreate")
        setContent {
            ProvideWindowInsets {
                FormatsLayout(
                    onBack = { onBackPressed() },
                    formatsList = formats.toList(),
                )
            }
        }
    }
}

// TODO: Get dynamic day-night theme
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun FormatsLayout(
    onBack: () -> Unit,
    formatsList: List<String>,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    XmpTheme3 {
        Scaffold(
            topBar = {
                XmpAppBar3(
                    scrollBehavior = scrollBehavior,
                    titleText = stringResource(id = R.string.pref_list_formats_title),
                    onNavIconPressed = onBack
                )
            }
        ) {
            val context = LocalContext.current
            val haptic = LocalHapticFeedback.current
            val clip = LocalClipboardManager.current

            LazyList(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                boxContent = {
                    if (formatsList.isEmpty())
                        ErrorLayout(message = stringResource(id = R.string.msg_no_formats))
                },
                lazyContent = {
                    itemsIndexed(items = formatsList) { _, item ->
                        ListItem(
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    context.toast(R.string.clipboard_copied)
                                    clip.setText(buildAnnotatedString { append(item) })
                                }
                            ),
                            text = { Text(text = item) }
                        )
                    }
                }
            )
        }
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ListFormatsLayoutPreview() {
    val list = listOf("String 1", "String 2", "String 3", "String 4", "String 5")
    FormatsLayout(
        onBack = {},
        formatsList = list,
    )
}
