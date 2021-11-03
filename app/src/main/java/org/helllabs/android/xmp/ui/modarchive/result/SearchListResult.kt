package org.helllabs.android.xmp.ui.modarchive.result

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.SearchListResult as _SearchListResult
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.ARTIST_ID
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.ui.modarchive.SearchError
import org.helllabs.android.xmp.ui.modarchive.result.SearchListViewModel.SearchResultState
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.util.logD

@AndroidEntryPoint
class SearchListResult : AppCompatActivity() {

    private val viewModel: SearchListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        var appTitle = ""

        intent.getStringExtra(SEARCH_TEXT)?.let {
            appTitle = getString(R.string.search_title_title)
            viewModel.getFileOrTitle(it)
        }

        intent.getIntExtra(ARTIST_ID, -1).let {
            if (it < 0) return@let
            appTitle = getString(R.string.search_artist_modules_title)
            viewModel.getArtistById(it)
        }

        logD("onCreate")
        setContent {
            val state = viewModel.searchResultState.collectAsState()
            SearchLayout(
                appTitle = appTitle,
                onBack = { onBackPressed() },
                resultState = state.value,
            )
        }
    }
}

@Composable
private fun SearchLayout(
    appTitle: String,
    onBack: () -> Unit,
    resultState: SearchResultState,
) {
    XmpTheme {
        Scaffold(
            topBar = {
                AppBar(
                    title = appTitle,
                    navIconClick = { onBack() },
                )
            }
        ) {
            val context = LocalContext.current
            var result by remember { mutableStateOf(listOf<Module>()) }
            LazyList(
                modifier = Modifier.fillMaxSize(),
                showScrollAt = 5,
                boxContent = {
                    when (resultState) {
                        SearchResultState.None -> Unit
                        SearchResultState.Load -> {
                            ProgressbarIndicator()
                        }
                        is SearchResultState.Error -> {
                            val msg = resultState.error
                                ?: stringResource(id = R.string.search_unknown_error)

                            val intent = Intent(context, SearchError::class.java)
                            intent.putExtra(ModArchiveConstants.ERROR, msg)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            context.startActivity(intent)
                            (context as Activity).overridePendingTransition(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )
                        }
                        is SearchResultState.SoftError -> {
                            ErrorLayout(
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp)
                                    .fillMaxSize(),
                                resultState.softError
                            )
                        }
                        is SearchResultState.SearchResult -> {
                            result = resultState.result.module.orEmpty()
                        }
                    }
                },
                lazyContent = {
                    itemsIndexed(items = result) { _, item ->
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
private fun SearchLayoutPreview() {
    val result = mutableListOf<Module>()
    repeat(8) {
        result.add(
            Module(
                format = "XM",
                songtitle = "Some Song Title $it",
                artistInfo = ArtistInfo(artist = Artist(alias = "Some Artist")),
                bytes = 669669
            )
        )
    }
    val state = SearchResultState.SearchResult(_SearchListResult(module = result))

    SearchLayout(
        appTitle = stringResource(R.string.search_title_title),
        onBack = {},
        resultState = state,
    )
}
