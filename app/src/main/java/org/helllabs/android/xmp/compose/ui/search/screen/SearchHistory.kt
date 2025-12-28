package org.helllabs.android.xmp.compose.ui.search.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.components.ItemModule
import org.helllabs.android.xmp.model.Module

@Composable
fun SearchHistoryScreen(
    historyList: ImmutableList<Module>,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onClicked: (Int) -> Unit
) {
    val scrollState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    /**
     * Clear history dialog
     */
    var clearDialog by remember { mutableStateOf(false) }
    MessageDialog(
        isShowing = clearDialog,
        title = "Clear History",
        text = "Are you sure you want to clear your Module search history?",
        confirmText = "Clear",
        onConfirm = {
            onClear()
            clearDialog = false
        },
        onDismiss = { clearDialog = false }
    )

    Scaffold(
        topBar = {
            XmpTopBar(
                isScrolled = isScrolled.value,
                onBack = onBack,
                title = stringResource(id = R.string.screen_title_history),
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val modifier = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier
            } else {
                Modifier.displayCutoutPadding()
            }
        }

        Box(
            modifier = modifier.padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(items = historyList.reversed()) { _, item ->
                    ItemModule(
                        item = item,
                        onClick = { onClicked(item.id) }
                    )
                }
            }

            if (historyList.isEmpty()) {
                ErrorScreen(text = "Empty History")
            }
        }
    }
}

@Preview
@Composable
private fun Preview_HistoryScreen() {
    XmpTheme(useDarkTheme = true) {
        SearchHistoryScreen(
            historyList = List(12) {
                Module(songtitle = "Module $it")
            }.toPersistentList(),
            onBack = { },
            onClear = { },
            onClicked = { }
        )
    }
}
