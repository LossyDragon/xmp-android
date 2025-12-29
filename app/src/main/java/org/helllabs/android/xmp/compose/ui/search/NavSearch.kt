package org.helllabs.android.xmp.compose.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.helllabs.android.xmp.compose.navkey.NavKeySearch
import org.helllabs.android.xmp.compose.ui.search.screen.ModuleResultScreenImpl
import org.helllabs.android.xmp.compose.ui.search.screen.SearchErrorScreen
import org.helllabs.android.xmp.compose.ui.search.screen.SearchScreen
import org.helllabs.android.xmp.compose.ui.search.screen.TitleResultScreenImpl
import org.helllabs.android.xmp.compose.ui.search.viewmodel.ResultViewModel
import org.helllabs.android.xmp.compose.ui.search.viewmodel.SearchResultViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun NavSearch(
    modifier: Modifier,
    snackbarHostState: SnackbarHostState
) {
    val searchBackStack = rememberNavBackStack(NavKeySearch.Search)

    BackHandler(enabled = searchBackStack.size > 1) {
        searchBackStack.removeLastOrNull()
    }

    NavDisplay(
        backStack = searchBackStack,
        onBack = { searchBackStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<NavKeySearch.Search> {
                SearchScreen(
                    modifier = modifier,
                    onSearch = { query, type ->
                        val screen = NavKeySearch.SearchResult(query, type)
                        searchBackStack.add(screen)
                    },
                    onRandom = {
                        val screen = NavKeySearch.Result(-1)
                        searchBackStack.add(screen)
                    },
                    onHistory = {
                        val screen = NavKeySearch.SearchHistory
                        searchBackStack.add(screen)
                    },
                )
            }
            entry<NavKeySearch.SearchError> {
                SearchErrorScreen(
                    message = it.message,
                    onBack = { searchBackStack.removeLastOrNull() }
                )
            }
            entry<NavKeySearch.SearchHistory> {
                val screen = NavKeySearch.SearchHistory
                searchBackStack.add(screen)
            }
            entry<NavKeySearch.SearchResult> {
                val viewModel = koinViewModel<SearchResultViewModel>()
                TitleResultScreenImpl(
                    viewModel = viewModel,
                    searchType = it.type,
                    searchQuery = it.query,
                    onBack = { searchBackStack.removeLastOrNull() },
                    onClick = { moduleID ->
                        val screen = NavKeySearch.Result(moduleID = moduleID)
                        searchBackStack.add(screen)
                    },
                    onError = { error ->
                        val screen = NavKeySearch.SearchError(error)
                        searchBackStack.add(screen)
                    },
                )
            }
            entry<NavKeySearch.Result> {
                val viewModel = koinViewModel<ResultViewModel>()
                ModuleResultScreenImpl(
                    viewModel = viewModel,
                    snackBarHostState = snackbarHostState,
                    moduleID = it.moduleID,
                    onShare = {
                    },
                    onError = {
                    },
                    onBack = { searchBackStack.removeLastOrNull() },
                )
            }
        }
    )
}
