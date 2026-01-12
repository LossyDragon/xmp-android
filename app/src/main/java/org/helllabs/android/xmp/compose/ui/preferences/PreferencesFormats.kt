package org.helllabs.android.xmp.compose.ui.preferences

import android.content.ClipData
import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpRoundedCorner
import org.helllabs.android.xmp.compose.theme.XmpTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormatsScreen(
    snackBarHostState: SnackbarHostState,
    formatsList: ImmutableList<String>,
    onBack: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            XmpTopBar(
                title = stringResource(id = R.string.screen_title_formats),
                isScrolled = isScrolled.value,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        val clip = LocalClipboard.current
        val resources = LocalResources.current
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val configuration = LocalConfiguration.current
        val modifier = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier
            } else {
                Modifier.displayCutoutPadding()
            }
        }

        LazyColumn(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = scrollState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(formatsList) { item ->
                ListItem(
                    modifier = Modifier
                        .clip(XmpRoundedCorner)
                        .combinedClickable(
                            onClick = { /* Nothing */ },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    snackBarHostState.showSnackbar(
                                        message = resources.getString(R.string.copied)
                                    )

                                    val entry = ClipData.newPlainText(item, item)
                                    clip.setClipEntry(entry.toClipEntry())
                                }
                            }
                        ),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    headlineContent = {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_FormatsScreen() {
    XmpTheme(useDarkTheme = true) {
        FormatsScreen(
            snackBarHostState = SnackbarHostState(),
            formatsList = List(14) { "Format $it" }.toPersistentList(),
            onBack = { }
        )
    }
}
