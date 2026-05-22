package com.lossydragon.media3.ui.screens.downloads

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lossydragon.media3.BuildConfig
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.player.XmpPlayerViewModel
import com.lossydragon.media3.ui.NavKeyDownload
import com.lossydragon.media3.ui.screens.downloads.screen.DownloadHistoryScreen
import com.lossydragon.media3.ui.screens.downloads.screen.DownloadModuleScreen
import com.lossydragon.media3.ui.screens.downloads.screen.DownloadResultScreen
import com.lossydragon.media3.util.findDownloadedModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun NavDownloads(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val viewModel: XmpPlayerViewModel = koinViewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )

    val backStack = rememberNavBackStack(NavKeyDownload.Search)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hasApiKey = remember { BuildConfig.API_KEY.isNotBlank() }

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<NavKeyDownload.Search> {
                DownloadSearchScreen(
                    modifier = modifier,
                    hasApiKey = hasApiKey,
                    snackbarHostState = snackbarHostState,
                    onBack = onBack,
                    onSearch = { query, type ->
                        backStack.add(NavKeyDownload.SearchResult(query, type))
                    },
                    onRandom = { backStack.add(NavKeyDownload.Module(-1)) },
                    onHistory = { backStack.add(NavKeyDownload.History) },
                )
            }

            entry<NavKeyDownload.History> {
                DownloadHistoryScreen(
                    modifier = modifier,
                    onBack = { backStack.removeLastOrNull() },
                    onModuleClick = { backStack.add(NavKeyDownload.Module(it)) },
                )
            }

            entry<NavKeyDownload.SearchResult> { it ->
                DownloadResultScreen(
                    modifier = modifier,
                    searchType = it.type,
                    query = it.query,
                    onBack = backStack::removeLastOrNull,
                    onModuleClick = { backStack.add(NavKeyDownload.Module(it)) },
                )
            }

            entry<NavKeyDownload.Module> {
                DownloadModuleScreen(
                    modifier = modifier,
                    moduleId = it.moduleId,
                    onBack = { backStack.removeLastOrNull() },
                    onPlay = { module ->
                        scope.launch(Dispatchers.IO) {
                            val rootUriStr = viewModel.getLastDirectoryUri() ?: return@launch
                            val rootUri = rootUriStr.toUri()
                            val filename = module.url.substringAfterLast('#')

                            // Walk the download dir to find the file URI
                            val fileUri = context.findDownloadedModule(
                                rootUri = rootUri,
                                artist = module.artist,
                                filename = filename
                            ) ?: return@launch

                            val moduleFile = ModuleFile(
                                uri = fileUri,
                                name = module.songtitle.ifBlank { filename },
                                sizeBytes = module.bytes.toLong(),
                                extension = filename.substringAfterLast('.', ""),
                            )

                            withContext(Dispatchers.Main) {
                                viewModel.play(moduleFile)
                                onNavigateToPlayer()
                            }
                        }
                    },
                )
            }
        }
    )
}
