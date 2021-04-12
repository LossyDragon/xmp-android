package org.helllabs.android.xmp.presentation.ui.search.searchResult

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
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
import org.helllabs.android.xmp.model.Item
import org.helllabs.android.xmp.presentation.components.AppBar
import org.helllabs.android.xmp.presentation.components.ErrorLayout
import org.helllabs.android.xmp.presentation.components.ItemSingle
import org.helllabs.android.xmp.presentation.components.LazyColumnWithTopScroll
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.systemDarkTheme
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.ARTIST_ID
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.presentation.ui.search.SearchError
import org.helllabs.android.xmp.util.logD

@AndroidEntryPoint
class ArtistResult : ComponentActivity() {

    private val viewModel: ArtistResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        viewModel.fetchArtists(intent.getStringExtra(SEARCH_TEXT)!!)

        logD("onCreate")
        setContent {
            val state = viewModel.artistState.collectAsState()

            ArtistLayout(
                appTitle = stringResource(id = R.string.search_artist_title),
                onBack = { onBackPressed() },
                resultState = state,
                onClick = { id ->
                    Intent(this, SearchListResult::class.java).apply {
                        putExtra(ARTIST_ID, id)
                    }.also {
                        startActivity(it)
                    }
                },
                onHardError = { error ->
                    Intent(this, SearchError::class.java).apply {
                        putExtra(ERROR, error)
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
fun ArtistLayout(
    isDarkTheme: Boolean = systemDarkTheme(),
    appTitle: String,
    onBack: () -> Unit,
    resultState: State<ArtistResultViewModel.ArtistState>,
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
            var items by remember { mutableStateOf(listOf<Item>()) }
            LazyColumnWithTopScroll(
                modifier = Modifier.fillMaxSize(),
                showScrollAt = 5,
                boxContent = {
                    when (val state = resultState.value) {
                        ArtistResultViewModel.ArtistState.None -> Unit
                        ArtistResultViewModel.ArtistState.Load -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is ArtistResultViewModel.ArtistState.Error -> {
                            onHardError(
                                state.error ?: stringResource(id = R.string.search_unknown_error)
                            )
                        }
                        is ArtistResultViewModel.ArtistState.SoftError -> {
                            ErrorLayout(state.softError)
                        }
                        is ArtistResultViewModel.ArtistState.SearchResult -> {
                            items = state.result.items.orEmpty()
                        }
                    }
                },
                lazyContent = {
                    itemsIndexed(items = items) { _, item ->
                        ItemSingle(
                            text = item.alias!!,
                            onClick = { onClick(item.id!!) },
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
