package org.helllabs.android.xmp.ui.search

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.insets.systemBarsPadding
import com.squareup.moshi.JsonAdapter
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.ui.search.result.ModuleResult
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.util.DialogMessage
import org.helllabs.android.xmp.util.launchActivity

@AndroidEntryPoint
class SearchHistory : AppCompatActivity() {

    @Inject
    lateinit var moshiAdapter: JsonAdapter<List<Module>>

    private val historyList: List<Module>
        get() = PrefManager.searchHistory?.let {
            moshiAdapter.fromJson(it)
        }.orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ProvideWindowInsets {
                SearchHistoryScreen(
                    onBack = { onBackPressed() },
                    historyList = historyList
                )
            }
        }
    }

    companion object {
        const val HISTORY_LENGTH = 50
    }
}

@Composable
private fun SearchHistoryScreen(
    onBack: () -> Unit,
    historyList: List<Module>
) {
    val list = remember { mutableStateOf(historyList) } // Only here to force recompositions

    SearchHistoryLayout(
        onBack = { onBack() },
        historyList = list.value,
        onCleared = {
            PrefManager.clearSearchHistory()
            list.value = listOf()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchHistoryLayout(
    onBack: () -> Unit,
    historyList: List<Module>,
    onCleared: () -> Unit,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    val onClearState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = onClearState,
        title = R.string.dialog_clear_history_title,
        message = R.string.dialog_clear_history_msg,
        positiveButtonText = R.string.delete,
        negativeButtonText = R.string.cancel,
        onPositiveButton = onCleared,
        onDismiss = { onClearState.hide() }
    )

    XmpTheme3 {
        Scaffold(
            topBar = {
                // Top App Bar
                val rotation = LocalConfiguration.current.orientation
                val appBarModifier =
                    if (rotation == Configuration.ORIENTATION_PORTRAIT) Modifier.statusBarsPadding()
                    else Modifier.systemBarsPadding()

                XmpAppBar3(
                    modifier = appBarModifier,
                    scrollBehavior = scrollBehavior,
                    onNavIconPressed = onBack,
                    titleText = stringResource(id = R.string.search_history),
                    actions = {
                        if (historyList.isNotEmpty()) {
                            DeleteMenu(
                                deleteClick = { onClearState.show() },
                                image = Icons.Default.ClearAll
                            )
                        }
                    }
                )
            }
        ) {
            val context = LocalContext.current

            LazyList(
                modifier = Modifier.fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                boxContent = {
                    if (historyList.isEmpty()) {
                        ErrorLayout(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp)
                                .fillMaxSize(),
                            message = stringResource(id = R.string.history_no_items)
                        )
                    }
                },
                lazyContent = {
                    itemsIndexed(items = historyList.reversed()) { _, item ->
                        ItemModule(
                            item = item,
                            onClick = {
                                val intent = Intent(context, ModuleResult::class.java)
                                intent.putExtra(MODULE_ID, item.id!!)
                                context.launchActivity(intent)
                            }
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

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun SearchHistoryPreviewDark() {
    SearchHistoryLayout(
        onBack = {},
        historyList = fakeDataSearchListResult(),
        onCleared = {},
    )
}
