package org.helllabs.android.xmp.ui.search.result

import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import dagger.hilt.android.AndroidEntryPoint
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.SearchListResult as Result
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.search.ModArchiveConstants
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.ARTIST_ID
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.ui.search.SearchError
import org.helllabs.android.xmp.ui.search.result.SearchListViewModel.SearchResultState
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.util.launchActivity
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

            ProvideWindowInsets {
                SearchLayout(
                    appTitle = appTitle,
                    onBack = { onBackPressed() },
                    resultState = state.value,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchLayout(
    appTitle: String,
    onBack: () -> Unit,
    resultState: SearchResultState,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    XmpTheme3 {
        Scaffold(
            topBar = {
                XmpAppBar3(
                    scrollBehavior = scrollBehavior,
                    titleText = appTitle,
                    onNavIconPressed = onBack,
                )
            }
        ) {
            val context = LocalContext.current
            var result by remember { mutableStateOf(listOf<Module>()) }

            LazyList(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
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
                            context.launchActivity(intent)
                        }
                        is SearchResultState.SoftError -> {
                            ErrorLayout(message = resultState.softError)
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

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun SearchLayoutPreview() {
    val state = SearchResultState.SearchResult(Result(module = fakeDataSearchListResult()))

    SearchLayout(
        appTitle = stringResource(R.string.search_title_title),
        onBack = {},
        resultState = state,
    )
}
