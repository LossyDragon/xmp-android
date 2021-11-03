package org.helllabs.android.xmp.ui.modarchive

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.ui.modarchive.result.ModuleResult
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.util.yesNoDialog

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
            SearchHistoryScreen(
                onBack = { onBackPressed() },
                historyList = historyList
            )
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

@Composable
private fun SearchHistoryLayout(
    onBack: () -> Unit,
    historyList: List<Module>,
    onCleared: () -> Unit,
) {
    XmpTheme {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val onClear = {
            context.yesNoDialog(
                lifecycleOwner = lifecycleOwner,
                title = context.getString(R.string.dialog_clear_history_title),
                message = context.getString(R.string.dialog_clear_history_msg),
                positiveButton = R.string.delete,
                negativeButton = R.string.cancel,
                onPositiveButton = { onCleared() }
            )
        }
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(id = R.string.search_history),
                    navIconClick = { onBack() },
                    menuActions = {
                        if (historyList.isNotEmpty())
                            DeleteMenu(
                                deleteClick = { onClear() },
                                image = Icons.Default.ClearAll
                            )
                    },
                )
            }
        ) {
            LazyList(
                modifier = Modifier.fillMaxSize(),
                showScrollAt = 5,
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
                                context.startActivity(intent)
                                (context as Activity).overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
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

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun SearchHistoryPreviewDark() {
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
        onBack = {},
        historyList = items,
        onCleared = {},
    )
}
