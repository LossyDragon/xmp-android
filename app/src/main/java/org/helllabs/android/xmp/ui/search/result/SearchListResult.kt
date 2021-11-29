package org.helllabs.android.xmp.ui.search.result

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.NavScreens
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.theme.XmpTheme3

@Composable
fun SearchListResult(
    navController: NavController,
    querySearch: String?,
    queryArtist: String?,
    viewModel: SearchListViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    var appTitle by remember { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(true) {
        when {
            querySearch != null -> {
                appTitle = context.getString(R.string.search_title_title)
                viewModel.onEvent(SearchListEvent.FileOrTitle(querySearch))
            }
            queryArtist != null -> { // Convert to Int to bypass nullable limit
                appTitle = context.getString(R.string.search_artist_modules_title)
                viewModel.onEvent(SearchListEvent.ArtistById(queryArtist.toInt()))
            }
        }

        viewModel.uiState.collectLatest { event ->
            when (event) {
                is SearchListUiState.Error -> {
                    navController.navigate(
                        NavScreens.SearchError.route + "?errorMsg=${event.error}"
                    )
                }
                is SearchListUiState.Loading ->
                    isLoading = event.isLoading
            }
        }
    }

    SearchLayout(
        onBack = { navController.popBackStack() },
        onClick = { id ->
            navController.navigate(
                NavScreens.SearchModuleResult.route + "?moduleId=$id"
            )
        },
        appTitle = appTitle,
        isLoading = isLoading,
        state = viewModel.state.value
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchLayout(
    onBack: () -> Unit,
    onClick: (id: Int) -> Unit,
    appTitle: String,
    isLoading: Boolean,
    state: SearchListState
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    Scaffold(
        topBar = {
            XmpAppBar3(
                scrollBehavior = scrollBehavior,
                titleText = appTitle,
                onNavIconPressed = onBack,
            )
        }
    ) {
        LazyList(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            boxContent = {
                state.softError?.let {
                    ErrorLayout(message = it)
                }

                ProgressbarIndicator(isLoading)
            },
            lazyContent = {
                itemsIndexed(items = state.result?.module.orEmpty()) { _, item ->
                    ItemModule(
                        item = item,
                        onClick = { onClick(item.id!!) }
                    )
                }
            }
        )
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun SearchLayoutPreview() {
    val state = SearchListState(result = fakeDataSearchListResult(), softError = "Test Error")

    XmpTheme3 {
        SearchLayout(
            onBack = {},
            onClick = {},
            appTitle = stringResource(R.string.search_title_title),
            isLoading = true,
            state = state,
        )
    }
}
