package org.helllabs.android.xmp.ui.search.result

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.ArtistResult as _ArtistResult
import org.helllabs.android.xmp.model.Item
import org.helllabs.android.xmp.ui.components.AppBar
import org.helllabs.android.xmp.ui.components.ErrorLayout
import org.helllabs.android.xmp.ui.components.LazyList
import org.helllabs.android.xmp.ui.components.ProgressbarIndicator
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.ARTIST_ID
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.ui.search.SearchError
import org.helllabs.android.xmp.ui.search.result.ArtistResultViewModel.ArtistState
import org.helllabs.android.xmp.ui.theme.XmpTheme
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
            ArtistResultScreen(
                viewModel = viewModel,
                onBack = { onBackPressed() }
            )
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
            context.startActivity(intent)
            (context as Activity).overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        }
    ) { error ->
        val intent = Intent(context, SearchError::class.java)
        intent.putExtra(ERROR, error)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
        (context as Activity).overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ArtistLayout(
    onBack: () -> Unit,
    resultState: ArtistState,
    onClick: (id: Int) -> Unit,
    onHardError: (message: String) -> Unit,
) {
    XmpTheme {
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(id = R.string.search_artist_title),
                    navIconClick = { onBack() },
                )
            }
        ) {
            var items by remember { mutableStateOf(listOf<Item>()) }
            LazyList(
                modifier = Modifier.fillMaxSize(),
                boxContent = {
                    when (resultState) {
                        ArtistState.None -> Unit
                        ArtistState.Load -> {
                            ProgressbarIndicator()
                        }
                        is ArtistState.Error -> {
                            onHardError(
                                resultState.error
                                    ?: stringResource(id = R.string.search_unknown_error)
                            )
                        }
                        is ArtistState.SoftError -> {
                            ErrorLayout(
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp)
                                    .fillMaxSize(),
                                resultState.softError
                            )
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
    val list = mutableListOf<Item>()
    repeat(8) {
        list.add(Item(alias = "Item $it"))
    }
    val result = _ArtistResult(items = list.toList())

    ArtistLayout(
        onBack = {},
        resultState = ArtistState.SearchResult(result),
        onClick = {},
        onHardError = {},
    )
}
