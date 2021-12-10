package org.helllabs.android.xmp.ui.search.result

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.ArtistResult as _ArtistResult
import org.helllabs.android.xmp.ui.NavScreens
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.theme.XmpTheme3

@Composable
fun ArtistResultScreen(
    navController: NavController,
    artistQuery: String,
    viewModel: ArtistResultViewModel = hiltViewModel(),
) {
    var isLoading by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(true) {
        viewModel.onEvent(ArtistEvent.FetchArtist(artistQuery))

        viewModel.uiState.collectLatest { event ->
            when (event) {
                is ArtistUiState.Error -> {
                    navController.navigate(
                        NavScreens.SearchError.route + "?errorMsg=${event.error}"
                    )
                }
                is ArtistUiState.Loading ->
                    isLoading = event.isLoading
            }
        }
    }

    ArtistLayout(
        onBack = { navController.popBackStack() },
        onClick = { artistId ->
            navController.navigate(
                NavScreens.SearchListResult.route + "?queryArtist=$artistId"
            )
        },
        state = viewModel.state.value,
        isLoading = isLoading,
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ArtistLayout(
    onBack: () -> Unit,
    onClick: (artistId: Int) -> Unit,
    state: ArtistState,
    isLoading: Boolean,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    Scaffold(
        topBar = {
            XmpAppBar3(
                scrollBehavior = scrollBehavior,
                onNavIconPressed = onBack,
                titleText = stringResource(id = R.string.search_artist_title)
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
                itemsIndexed(items = state.result?.listItems.orEmpty()) { _, item ->
                    ListItem(
                        modifier = Modifier.clickable {
                            onClick(item.id)
                        },
                        text = { Text(item.alias) }
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
private fun ArtistResultPreview() {
    val result = fakeDataArtistResult()

    XmpTheme3 {
        ArtistLayout(
            onBack = {},
            onClick = {},
            state = ArtistState(result = _ArtistResult(items = result), softError = "Test Error"),
            isLoading = true,
        )
    }
}
