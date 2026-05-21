package com.lossydragon.media3.ui.screens.downloads

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.lossydragon.media3.model.SearchType
import com.lossydragon.media3.ui.theme.XmpTheme
import com.lossydragon.media3.ui.util.annotatedLinkString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DownloadSearchScreen(
    modifier: Modifier = Modifier,
    hasApiKey: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSearch: (String, SearchType) -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(SearchType.TITLE) }
    var hasInteracted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500L) // Chill
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Search") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        content = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        if (!hasApiKey) {
                            snackbarHostState.showSnackbar(
                                message = "No API key used to search."
                            )
                        } else if (query.length < 3) {
                            snackbarHostState.showSnackbar(
                                message = "At least 3 characters required to search"
                            )
                        } else {
                            onSearch(query, type)
                            focusManager.clearFocus()
                        }
                    }
                },
                text = { Text(text = "Search") },
                icon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) }
            )
        },
        bottomBar = {
            ShortNavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                content = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                        content = {
                            Text(
                                text = annotatedLinkString(
                                    text = "Powered by The Mod Archive",
                                    url = "https://modarchive.org/"
                                ),
                            )
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            content = {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    enabled = hasApiKey,
                    value = query,
                    onValueChange = {
                        query = it
                        hasInteracted = true
                    },
                    isError = hasInteracted && query.length < 3,
                    singleLine = true,
                    label = { Text(text = "Search") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.length >= 3) {
                                onSearch(query, type)
                                focusManager.clearFocus()
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "At least 3 characters required to search"
                                    )
                                }
                            }
                        }
                    ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                ButtonGroup(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    expandedRatio = .20f,
                    overflowIndicator = {},
                    content = {
                        toggleableItem(
                            checked = type == SearchType.TITLE,
                            label = "Title or Filename",
                            onCheckedChange = { type = SearchType.TITLE },
                            weight = 1f,
                        )
                        toggleableItem(
                            checked = type == SearchType.ARTIST,
                            label = "Artist",
                            onCheckedChange = { type = SearchType.ARTIST },
                            weight = 1f,
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                ButtonGroup(
                    overflowIndicator = {},
                    content = {
                        customItem(
                            buttonGroupContent = {
                                OutlinedButton(
                                    onClick = onRandom,
                                    enabled = hasApiKey,
                                    modifier = Modifier.weight(1f),
                                    content = {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Random")
                                    }
                                )
                            },
                            menuContent = {}
                        )
                        customItem(
                            buttonGroupContent = {
                                OutlinedButton(
                                    onClick = onHistory,
                                    content = {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "History")
                                    }
                                )
                            },
                            menuContent = {}
                        )
                    }
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        DownloadSearchScreen(
            hasApiKey = true,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onSearch = { _, _ -> },
            onRandom = {},
            onHistory = {},
        )
    }
}
