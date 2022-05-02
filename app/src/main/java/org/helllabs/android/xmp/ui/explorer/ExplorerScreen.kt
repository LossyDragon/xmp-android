package org.helllabs.android.xmp.ui.explorer

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.ui.components.ProgressbarIndicator
import org.helllabs.android.xmp.ui.components.TopAppBar
import org.helllabs.android.xmp.ui.destinations.PlaylistScreenDestination
import org.helllabs.android.xmp.ui.explorer.util.CachingDocumentFile
import org.helllabs.android.xmp.util.preferences.Manager.dataStoreManager
import org.helllabs.android.xmp.util.preferences.requestMediaPath
import timber.log.Timber

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Destination
@Composable
fun ExplorerScreen(
    navigator: DestinationsNavigator,
    viewModel: ExplorerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    val onClick: (document: CachingDocumentFile) -> Unit = { document ->
        viewModel.documentClicked(document)
    }
    val onOverflowClick: () -> Unit = {
        // TODO
    }

    // New URI handler
    val contentResolver = LocalContext.current.contentResolver
    val contract = ActivityResultContracts.StartActivityForResult()
    val startForResult = rememberLauncherForActivityResult(contract) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Timber.w("Failed getting directory access")
        }
        if (result.resultCode == Activity.RESULT_OK) {
            val directoryUri = result.data?.data ?: return@rememberLauncherForActivityResult

            contentResolver.takePersistableUriPermission(
                directoryUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Save our starting point
            scope.launch {
                // Remove previous value
                val oldValue = dataStoreManager.getPreference(requestMediaPath)
                if (oldValue != "") {
                    contentResolver.releasePersistableUriPermission(
                        oldValue.toUri(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                // Save
                dataStoreManager.editPreference(requestMediaPath.key, directoryUri.toString())
            }

            // Navigate to directory
            viewModel.loadDirectory(directoryUri, true)
        }
    }

    // Back Handler
    var handleBack by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.backStack.size) {
        handleBack = uiState.backStack.size > 1
    }
    BackHandler(handleBack) {
        viewModel.popBackStack()
    }

    // Observers
    LaunchedEffect(true) {
        viewModel.openDirectory.collectLatest { event ->
            Timber.d("openDirectory")
            event.getContentIfNotHandled()?.let { directory ->
                viewModel.loadDirectory(directory.uri, true)
            }
        }
    }
    LaunchedEffect(true) {
        viewModel.openDocument.collectLatest { event ->
            Timber.d("openDocument")
            event.getContentIfNotHandled()?.let { document ->
                viewModel.onMusicItemPressed()
                Toast.makeText(
                    context,
                    "${document.name},\n${document.uri}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    LaunchedEffect(true) {
        viewModel.noValidUri.collectLatest { value ->
            if (value) {
                // TODO make a dialog for this, or a button
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startForResult.launch(intent)
            }
        }
    }

    // Content
    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text("Explorer")
                },
                onNavPressed = {
                    navigator.popBackStack(route = PlaylistScreenDestination, inclusive = false)
                },
                actions = {
                    IconButton(
                        onClick = {
                            // TODO add directory reset option
                        }
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, null)
                    }

                }
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = scrollState,
                contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    val isDirName = if (item.isDirectory) "Directory" else item.type.toString()
                    val isDirIcon = if (item.isDirectory) Icons.Default.Folder
                    else Icons.Default.InsertDriveFile

                    ExplorerItemCard(
                        modifier = Modifier.animateItemPlacement(),
                        cardIcon = isDirIcon,
                        primaryText = item.name.toString(),
                        secondaryText = isDirName,
                        onClick = { onClick(item) },
                        onOverFlow = { onOverflowClick() }
                    )
                }
            }

            if (!uiState.isLoading && uiState.items.isEmpty()) {
                OutlinedCard {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        fontSize = 18.sp,
                        text = "No items in list!"
                    )
                }
            }

            ProgressbarIndicator(uiState.isLoading)
        }
    }
}
