package org.helllabs.android.xmp.compose.ui.search.screen

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.components.DownloadsText
import org.helllabs.android.xmp.compose.ui.search.components.SearchButtons
import org.helllabs.android.xmp.compose.ui.search.components.SegmentedButtons

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    modifier: Modifier,
    onSearch: (String, SearchType) -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchType by rememberSaveable { mutableStateOf(SearchType.TITLE) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val orientationModifier = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Modifier.imePadding()
    } else {
        Modifier
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .then(orientationModifier),
        bottomBar = {
            NavigationBar {
                Box(modifier = Modifier.fillMaxWidth()) {
                    DownloadsText(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        },
    ) { paddingValues ->

        val modifier = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier
            } else {
                Modifier.displayCutoutPadding()
            }
        }

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
                                onSearch(searchText, searchType)
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
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth(),
                    searchType = searchType,
                    onSearchType = { searchType = it },
                )

                Spacer(modifier = Modifier.height(32.dp))

//                SearchButtons(
//                    searchText = searchText,
//                    onSearch = { onSearch(it, searchType) },
//                    onRandom = onRandom
//                )

                ButtonGroup(
                    modifier = Modifier,
                    overflowIndicator = { menuState ->
                        ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                    },
                    content = {
                        clickableItem(
                            onClick = {
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            label = "Search"
                        )
                        clickableItem(
                            onClick = {
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null
                                )
                            },
                            label = "Random"
                        )
                        clickableItem(
                            onClick = {
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null
                                )
                            },
                            label = "History"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Preview
@Composable
private fun Preview_SearchScreen() {
    XmpTheme(useDarkTheme = true) {
        SearchScreen(
            modifier = Modifier,
            onSearch = { _, _ -> },
            onRandom = {},
            onHistory = {}
        )
    }
}
