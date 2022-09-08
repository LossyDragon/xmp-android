@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package org.helllabs.android.xmp.ui.playlists.selected

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import org.helllabs.android.xmp.ui.components.ProgressbarIndicator
import org.helllabs.android.xmp.ui.components.TopAppBar
import org.helllabs.android.xmp.ui.destinations.PlaylistScreenDestination
import timber.log.Timber

data class PlaylistNavArgs(
    val title: String,
    val comment: String
)

@Destination(navArgsDelegate = PlaylistNavArgs::class)
@Composable
fun SelectedScreen(
    args: PlaylistNavArgs,
    navigator: DestinationsNavigator,
    viewModel: SelectedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState

    val topBarState = rememberTopAppBarState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topBarState) }
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
        viewModel.setDragDropState(
            // TODO: find a way to save the list once done dragging.
            uiState.data.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
        )
    }

    /* Item Edit */
    val itemEdit = rememberMaterialDialogState()
    ItemEditDialog(
        dialogState = itemEdit,
        onClick = { index ->
            // TODO handle overflow menu items
            Timber.d("Item Edit: $index")
        }
    )

    // Feed the ViewModel playlist data.
    LaunchedEffect(Unit) {
        viewModel.loadPlaylist(args.title)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(.75f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = args.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = args.comment.ifEmpty { "** No Comment **" },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                onNavPressed = {
                    navigator.popBackStack(route = PlaylistScreenDestination, inclusive = false)
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .dragContainer(dragDropState),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = rememberInsetsPaddingValues(
                    LocalWindowInsets.current.navigationBars,
                    applyBottom = true,
                    additionalTop = 10.dp,
                    additionalBottom = 80.dp
                ),
            ) {
                itemsIndexed(uiState.data, key = { _, item -> item }) { index, item ->
                    DraggableItem(dragDropState, index) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 16.dp else 1.dp)

                        SelectedItemCard(
                            elevation = elevation,
                            primaryText = item.name,
                            secondaryText = item.type,
                            onClick = {
                                // TODO
                            },
                            onOverFlow = {
                                itemEdit.show()
                            },
                        )
                    }
                }
            }

            if (!uiState.isLoading && uiState.data.isEmpty()) {
                OutlinedCard {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        fontSize = 18.sp,
                        text = "No playlists found!"
                    )
                }
            }

            ProgressbarIndicator(uiState.isLoading)
        }
    }
}
