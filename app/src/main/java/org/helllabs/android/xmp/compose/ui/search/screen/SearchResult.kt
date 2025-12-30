package org.helllabs.android.xmp.compose.ui.search.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.components.ItemModule
import org.helllabs.android.xmp.compose.ui.search.viewmodel.SearchResultState
import org.helllabs.android.xmp.compose.ui.search.viewmodel.SearchResultViewModel
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.SearchListResult
import org.helllabs.android.xmp.model.Sponsor
import org.helllabs.android.xmp.model.SponsorDetails
import timber.log.Timber

enum class SearchType {
    ARTIST,
    TITLE
}

@Composable
fun SearchResultScreen(
    viewModel: SearchResultViewModel,
    searchType: SearchType,
    searchQuery: String,
    onBack: () -> Unit,
    onClick: (Int) -> Unit,
    onError: (String?) -> Unit
) {
    val resources = LocalResources.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (searchType == SearchType.ARTIST) {
            val title = resources.getString(R.string.screen_title_artist)
            viewModel.getArtists(title, searchQuery)
        } else {
            val title = resources.getString(R.string.screen_title_search)
            viewModel.getFileOrTitle(title, searchQuery)
        }
    }

    LaunchedEffect(state.hardError) {
        if (state.hardError != null) {
            Timber.w("Hard error has occurred")
            onError(state.hardError)
        }
    }

    TitleResultScreen(
        state = state,
        onBack = onBack,
        onItemId = onClick,
        onArtistId = viewModel::getArtistById,
    )
}

@Composable
private fun TitleResultScreen(
    state: SearchResultState,
    onBack: () -> Unit,
    onItemId: (id: Int) -> Unit,
    onArtistId: (id: Int) -> Unit
) {
    val listState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        topBar = {
            XmpTopBar(
                title = state.title,
                onBack = onBack,
                isScrolled = isScrolled.value
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ProgressbarIndicator(isLoading = state.isLoading)

            if (!state.softError.isNullOrEmpty()) {
                ErrorScreen(text = state.softError)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    content = {
                        when (val items = state.result) {
                            is SearchListResult -> {
                                items(items.module) { item ->
                                    ItemModule(
                                        item = item,
                                        onClick = { onItemId(item.id) }
                                    )
                                }
                            }

                            is ArtistResult -> {
                                items(items.listItems) { item ->
                                    ListItem(
                                        modifier = Modifier.clickable {
                                            onArtistId(item.id)
                                        },
                                        headlineContent = {
                                            Text(text = item.alias)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_TitleResult() {
    XmpTheme(useDarkTheme = true) {
        TitleResultScreen(
            state = SearchResultState(
                isLoading = true,
                title = stringResource(id = R.string.screen_title_artist),
                result = null,
                softError = "Soft Error"
            ),
            onBack = {},
            onItemId = {},
            onArtistId = {}
        )
    }
}

@Preview
@Composable
private fun Preview_TitleResult2() {
    XmpTheme(useDarkTheme = true) {
        TitleResultScreen(
            state = SearchResultState(
                isLoading = false,
                title = stringResource(id = R.string.screen_title_artist),
                result = SearchListResult(
                    sponsor = Sponsor(
                        details = SponsorDetails(
                            link = "",
                            image = "",
                            text = "",
                            imagehtml = ""
                        )
                    ),
                    error = "",
                    results = 1,
                    totalpages = 1,
                    module = List(15) {
                        Module(
                            format = "XM",
                            songtitle = "Some Song Title $it",
                            artistInfo = ArtistInfo(
                                artist = listOf(Artist(alias = "Some Artist"))
                            ),
                            bytes = 669669
                        )
                    }
                )
            ),
            onBack = {},
            onItemId = {},
            onArtistId = {}
        )
    }
}
