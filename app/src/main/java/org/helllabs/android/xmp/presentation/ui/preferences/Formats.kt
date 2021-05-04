package org.helllabs.android.xmp.presentation.ui.preferences

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.presentation.components.AppBar
import org.helllabs.android.xmp.presentation.components.ErrorLayout
import org.helllabs.android.xmp.presentation.components.ItemSingle
import org.helllabs.android.xmp.presentation.components.LazyColumnWithTopScroll
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.systemDarkTheme
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.toast

@AndroidEntryPoint
class Formats : ComponentActivity() {

    @Inject
    lateinit var clipboard: ClipboardManager

    private val formats = Xmp.getFormats()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Sort alphabetically
        formats.sort()

        logD("onCreate")
        setContent {
            FormatsLayout(
                onBack = { onBackPressed() },
                formatsList = formats.toList(),
                onLongClick = {
                    val clip = ClipData.newPlainText("Xmp Clipboard", it)
                    clipboard.setPrimaryClip(clip)
                    toast(R.string.clipboard_copied)
                },
            )
        }
    }
}

@Composable
private fun FormatsLayout(
    isDarkTheme: Boolean = systemDarkTheme(),
    onBack: () -> Unit,
    formatsList: List<String>,
    onLongClick: (text: String) -> Unit,
) {
    AppTheme(
        isDarkTheme = isDarkTheme
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(id = R.string.pref_list_formats_title),
                    navIconClick = { onBack() },
                )
            }
        ) {
            LazyColumnWithTopScroll(
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

@Preview
@Composable
private fun ListFormatsLayoutPreview() {
    val list = listOf("String 1", "String 2", "String 3", "String 4", "String 5")
    FormatsLayout(
        isDarkTheme = false,
        onBack = {},
        formatsList = list,
        onLongClick = {},
    )
}

@Preview
@Composable
private fun ListFormatsLayoutPreviewDark() {
    val list = listOf("String 1", "String 2", "String 3", "String 4", "String 5")
    FormatsLayout(
        isDarkTheme = true,
        onBack = {},
        formatsList = list,
        onLongClick = {},
    )
}
