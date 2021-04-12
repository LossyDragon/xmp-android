package org.helllabs.android.xmp.presentation.ui.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.presentation.components.*
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.systemDarkTheme
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.presentation.ui.search.searchResult.ModuleResult
import org.helllabs.android.xmp.util.yesNoDialog

@AndroidEntryPoint
class SearchHistory : ComponentActivity() {

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
            val context = LocalContext.current
            var history by remember { mutableStateOf(historyList) }

            SearchHistoryLayout(
                appTitle = stringResource(id = R.string.search_history),
                onBack = { onBackPressed() },
                onClear = {
                    context.yesNoDialog(
                        this,
                        title = R.string.dialog_clear_history_title,
                        message = getString(R.string.dialog_clear_history_msg),
                        confirmText = R.string.delete,
                        dismissText = R.string.cancel,
                        onConfirm = {
                            PrefManager.clearSearchHistory()
                            history = historyList
                        }
                    )
                },
                historyList = history,
                onclick = { id ->
                    Intent(this, ModuleResult::class.java).apply {
                        putExtra(MODULE_ID, id)
                    }.also {
                        startActivity(it)
                    }
                }
            )
        }
    }

    companion object {
        const val HISTORY_LENGTH = 50
    }
}

@Composable
fun SearchHistoryLayout(
    isDarkTheme: Boolean = systemDarkTheme(),
    appTitle: String,
    onBack: () -> Unit,
    onClear: () -> Unit,
    historyList: List<Module>,
    onclick: (id: Int) -> Unit,
) {
    AppTheme(
        isDarkTheme = isDarkTheme
    ) {
        val isListEmpty = historyList.isEmpty()
        Scaffold(
            topBar = {
                AppBar(
                    title = appTitle,
                    navIconClick = { onBack() },
                    menuActions = { if (!isListEmpty) DeleteMenu { onClear() } },
                )
            }
        ) {
            if (isListEmpty) {
                ErrorLayout(message = stringResource(id = R.string.history_no_items))
            } else {
                LazyColumnWithTopScroll(
                    modifier = Modifier.fillMaxSize(),
                    showScrollAt = 5,
                    boxContent = { },
                    lazyContent = {
                        itemsIndexed(items = historyList.reversed()) { _, item ->
                            ItemModule(
                                item = item,
                                onClick = { onclick(item.id!!) }
                            )
                        }
                    }
                )
            }
        }
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
fun SearchHistoryPreview() {
    val items = mutableListOf<Module>()
    for (i in 0..5) {
        items.add(
            Module(
                format = "MOD",
                songtitle = "Some Title $i",
                artistInfo = ArtistInfo(artist = Artist(alias = "Some Artist $i")),
                bytes = 669
            )
        )
    }

    SearchHistoryLayout(
        isDarkTheme = false,
        appTitle = stringResource(id = R.string.search_history),
        onBack = {},
        onClear = {},
        historyList = items,
        onclick = {},
    )
}

@Preview
@Composable
fun SearchHistoryPreviewDark() {
    val items = mutableListOf<Module>()
    for (i in 0..5) {
        items.add(
            Module(
                format = "MOD",
                songtitle = "Some Title $i",
                artistInfo = ArtistInfo(artist = Artist(alias = "Some Artist $i")),
                bytes = 669
            )
        )
    }

    SearchHistoryLayout(
        isDarkTheme = true,
        appTitle = stringResource(id = R.string.search_history),
        onBack = {},
        onClear = {},
        historyList = items,
        onclick = {}
    )
}
