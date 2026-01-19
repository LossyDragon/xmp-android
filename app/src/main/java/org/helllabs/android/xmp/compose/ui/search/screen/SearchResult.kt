package org.helllabs.android.xmp.compose.ui.search.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.components.GuruFrame
import org.helllabs.android.xmp.compose.ui.search.components.GuruTextButton
import org.helllabs.android.xmp.compose.ui.search.components.ItemArtist
import org.helllabs.android.xmp.compose.ui.search.components.ItemModule
import org.helllabs.android.xmp.compose.ui.search.viewmodel.SearchResultState
import org.helllabs.android.xmp.compose.ui.search.viewmodel.SearchResultViewModel
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.Item
import org.helllabs.android.xmp.model.Items
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.SearchListResult

enum class SearchType {
    ARTIST,
    TITLE
}

@Composable
fun SearchResultScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchResultViewModel,
    searchType: SearchType,
    searchQuery: String,
    onBack: () -> Unit,
    onClick: (Int) -> Unit
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

    TitleResultScreen(
        modifier = modifier,
        state = state,
        onBack = onBack,
        onItemId = onClick,
        onArtistId = viewModel::getArtistById,
    )
}

@Composable
private fun TitleResultScreen(
    modifier: Modifier = Modifier,
    state: SearchResultState,
    onBack: () -> Unit,
    onItemId: (id: Int) -> Unit,
    onArtistId: (id: Int) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            XmpTopBar(
                title = state.title,
                onBack = onBack,
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ProgressbarIndicator(isLoading = state.isLoading && state.softError == null)

            if (!state.softError.isNullOrEmpty()) {
                GuruFrame(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    message = state.softError,
                    action = { GuruTextButton(text = "Go Back", onClick = onBack) },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                                ItemArtist(
                                    alias = item.alias,
                                    onClick = { onArtistId(item.id) }
                                )
                            }
                        }
                    }
                }
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
                title = stringResource(id = R.string.screen_title_artist),
                result = ArtistResult(
                    items = Items(
                        item = List(15) {
                            Item(
                                id = it,
                                alias = "Artist $it",
                            )
                        }
                    )
                ),
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
                title = stringResource(id = R.string.screen_title_result),
                result = SearchListResult(
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
