package org.helllabs.android.xmp.compose.ui.search.screen

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.annotatedLinkString
import org.helllabs.android.xmp.compose.theme.XmpTheme

@Stable
data class SearchSegmentedButton(val type: SearchType, @StringRes val string: Int)

@Composable
fun SearchScreen(
    modifier: Modifier,
    onBack: () -> Unit,
    onSearch: (String, SearchType) -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var searchText by rememberSaveable { mutableStateOf("") }
    var currentSelection by rememberSaveable { mutableStateOf(SearchType.TITLE) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val modifier = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier
            } else {
                Modifier.displayCutoutPadding()
            }
        }

        // (Not present anymore, but bug still opened) Weird bottom padding workaround:
        // https://issuetracker.google.com/issues/249727298
        Box(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    value = searchText,
                    onValueChange = { searchText = it },
                    isError = searchText.isEmpty(),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchText.isNotEmpty()) {
                                onSearch(searchText, currentSelection)
                            }

                            focusManager.clearFocus()
                        }
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    maxLines = 1,
                    label = { Text(text = stringResource(id = R.string.search)) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                SegmentedButtons(
                    onSearchType = { currentSelection = it },
                    searchType = currentSelection,
                )

                Spacer(modifier = Modifier.height(32.dp))

                SearchButtons(
                    searchText = searchText,
                    onSearch = { onSearch(it, currentSelection) },
                    onRandom = onRandom
                )

                Spacer(modifier = Modifier.height(64.dp))
            }

            DownloadsText(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedButtons(onSearchType: (SearchType) -> Unit, searchType: SearchType) {
    val buttonOptions = remember {
        listOf(
            SearchSegmentedButton(SearchType.TITLE, R.string.title_or_filename),
            SearchSegmentedButton(SearchType.ARTIST, R.string.artist)
        )
    }

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxWidth()
    ) {
        val activeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .75f)
        buttonOptions.forEachIndexed { idx, item ->
            SegmentedButton(
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = activeColor
                ),
                shape = SegmentedButtonDefaults.itemShape(
                    index = idx,
                    count = buttonOptions.size
                ),
                onClick = { onSearchType(item.type) },
                selected = searchType == item.type,
                label = { Text(text = stringResource(id = item.string)) }
            )
        }
    }
}

@Composable
private fun SearchButtons(
    searchText: String,
    onSearch: (String) -> Unit,
    onRandom: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            modifier = Modifier
                .weight(.75f),
            enabled = searchText.isNotEmpty(),
            onClick = { onSearch(searchText) }
        ) {
            Text(text = stringResource(id = R.string.search))
        }
        Spacer(modifier = Modifier.width(16.dp))
        OutlinedButton(
            modifier = Modifier
                .weight(.75f),
            onClick = onRandom
        ) {
            Text(text = stringResource(id = R.string.random))
        }
    }
}

@Composable
private fun DownloadsText(
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = annotatedLinkString(
            text = stringResource(id = R.string.search_provided_by),
            url = "modarchive.org"
        ),
        style = TextStyle(color = MaterialTheme.colorScheme.onBackground)
    )
}

@Preview
@Composable
private fun Preview_SearchScreen() {
    XmpTheme(useDarkTheme = true) {
        SearchScreen(
            modifier = Modifier,
            onBack = {},
            onSearch = { _, _ -> },
            onRandom = {},
            onHistory = {}
        )
    }
}
