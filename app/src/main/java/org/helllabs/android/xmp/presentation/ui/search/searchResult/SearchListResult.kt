package org.helllabs.android.xmp.presentation.ui.search.searchResult

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.*
import org.helllabs.android.xmp.presentation.components.*
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.systemDarkTheme
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.ARTIST_ID
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.presentation.ui.search.SearchError
import org.helllabs.android.xmp.util.logD

@AndroidEntryPoint
class SearchListResult : ComponentActivity() {

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
                resultState = state,
                onClick = { id ->
                    Intent(this, ModuleResult::class.java).apply {
                        putExtra(MODULE_ID, id)
                    }.also {
                        startActivity(it)
                    }
                },
                onHardError = { error ->
                    Intent(this, SearchError::class.java).apply {
                        putExtra(ModArchiveConstants.ERROR, error)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }.also {
                        startActivity(it)
                    }
                }
            )
        }
    }
}

@Composable
private fun SearchLayout(
    isDarkTheme: Boolean = systemDarkTheme(),
    appTitle: String,
    onBack: () -> Unit,
    resultState: State<SearchListViewModel.SearchResultState>,
    onClick: (id: Int) -> Unit,
    onHardError: (message: String) -> Unit,
) {
    AppTheme(
        isDarkTheme = isDarkTheme
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    title = appTitle,
                    navIconClick = { onBack() },
                )
            }
        ) {
            var result by remember { mutableStateOf(listOf<Module>()) }
            LazyColumnWithTopScroll(
                modifier = Modifier.fillMaxSize(),
                showScrollAt = 5,
                boxContent = {
                    when (val state = resultState.value) {
                        SearchListViewModel.SearchResultState.None -> Unit
                        SearchListViewModel.SearchResultState.Load -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is SearchListViewModel.SearchResultState.Error -> {
                            onHardError(
                                state.error ?: stringResource(id = R.string.search_unknown_error)
                            )
                        }
                        is SearchListViewModel.SearchResultState.SoftError -> {
                            ErrorLayout(state.softError)
                        }
                        is SearchListViewModel.SearchResultState.SearchResult -> {
                            result = state.result.module.orEmpty()
                        }
                    }
                },
                lazyContent = {
                    itemsIndexed(items = result) { _, item ->
                        ItemModule(
                            item = item,
                            onClick = { onClick(item.id!!) }
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
