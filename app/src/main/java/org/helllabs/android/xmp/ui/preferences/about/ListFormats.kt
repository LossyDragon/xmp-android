package org.helllabs.android.xmp.ui.preferences.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.ui.components.AppBar
import org.helllabs.android.xmp.ui.components.ErrorLayout
import org.helllabs.android.xmp.ui.components.ItemSingle
import org.helllabs.android.xmp.ui.components.LazyList
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.ui.util.toast
import org.helllabs.android.xmp.util.logD

@AndroidEntryPoint
class ListFormats : ComponentActivity() {

    @Inject
    lateinit var clipboard: ClipboardManager

    private val formats = Xmp.getFormats()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Sort alphabetically
        formats.sort()

        logD("onCreate")
        setContent {
            val haptic = LocalHapticFeedback.current
            FormatsLayout(
                onBack = { onBackPressed() },
                formatsList = formats.toList(),
                onLongClick = {
                    val clip = ClipData.newPlainText("Xmp Clipboard", it)
                    clipboard.setPrimaryClip(clip)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    toast(R.string.clipboard_copied)
                },
            )
        }
    }
}

@Composable
private fun FormatsLayout(
    onBack: () -> Unit,
    formatsList: List<String>,
    onLongClick: (text: String) -> Unit,
) {
    XmpTheme {
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(id = R.string.pref_list_formats_title),
                    navIconClick = { onBack() },
                )
            }
        ) {
            LazyList(
                modifier = Modifier.fillMaxSize(),
                showScrollAt = 15,
                boxContent = {
                    if (formatsList.isEmpty())
                        ErrorLayout(message = stringResource(id = R.string.msg_no_formats))
                },
                lazyContent = {
                    itemsIndexed(items = formatsList) { _, item ->
                        ItemSingle(
                            text = item,
                            onLongClick = { onLongClick(item) }
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
        onLongClick = {},
    )
}
