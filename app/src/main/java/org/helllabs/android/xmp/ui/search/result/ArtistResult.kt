package org.helllabs.android.xmp.ui.search.result

import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import dagger.hilt.android.AndroidEntryPoint
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.ArtistResult as _ArtistResult
import org.helllabs.android.xmp.model.Item
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.ARTIST_ID
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.ui.search.SearchError
import org.helllabs.android.xmp.ui.search.result.ArtistResultViewModel.ArtistState
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.util.launchActivity
import org.helllabs.android.xmp.util.logD

@AndroidEntryPoint
class ArtistResult : AppCompatActivity() {

    private val viewModel: ArtistResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        viewModel.fetchArtists(intent.getStringExtra(SEARCH_TEXT)!!)

        logD("onCreate")
        setContent {
            ProvideWindowInsets {
                ArtistResultScreen(
                    viewModel = viewModel,
                    onBack = { onBackPressed() }
                )
            }
        }
    }
}

@Composable
private fun ArtistResultScreen(
    viewModel: ArtistResultViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state = viewModel.artistState.collectAsState()

    ArtistLayout(
        onBack = { onBack() },
        resultState = state.value,
        onClick = { id ->
            val intent = Intent(context, SearchListResult::class.java)
            intent.putExtra(ARTIST_ID, id)
            context.launchActivity(intent)
        }
    ) { error ->
        val intent = Intent(context, SearchError::class.java)
        intent.putExtra(ERROR, error)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.launchActivity(intent)
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ArtistLayout(
    onBack: () -> Unit,
    resultState: ArtistState,
    onClick: (id: Int) -> Unit,
    onHardError: (message: String) -> Unit,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    XmpTheme3 {
        Scaffold(
            topBar = {
                XmpAppBar3(
                    scrollBehavior = scrollBehavior,
                    onNavIconPressed = onBack,
                    titleText = stringResource(id = R.string.search_artist_title)
                )
            }
        ) {
            var items by remember { mutableStateOf(listOf<Item>()) }

            LazyList(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                boxContent = {
                    when (resultState) {
                        ArtistState.None -> Unit
                        ArtistState.Load -> {
                            ProgressbarIndicator()
                        }
                        is ArtistState.Error -> {
                            val error = resultState.error
                                ?: stringResource(id = R.string.search_unknown_error)

                            onHardError(error)
                        }
                        is ArtistState.SoftError -> {
                            ErrorLayout(message = resultState.softError)
                        }
                        is ArtistState.SearchResult -> {
                            items = resultState.result.items.orEmpty()
                        }
                    }
                },
                lazyContent = {
                    itemsIndexed(items = items) { _, item ->
                        ListItem(
                            modifier = Modifier.clickable { onClick(item.id!!) },
                            text = { Text(item.alias!!) }
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
private fun ArtistResultPreview() {
    val result = fakeDataArtistResult()

    ArtistLayout(
        onBack = {},
        resultState = ArtistState.SearchResult(_ArtistResult(items = result)),
        onClick = {},
        onHardError = {},
    )
}
