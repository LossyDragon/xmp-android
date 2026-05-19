package com.lossydragon.media3.ui.downloads

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lossydragon.media3.BuildConfig
import com.lossydragon.media3.data.XmpPreferences
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.player.XmpPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun NavDownloads(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    onNavigateToPlayer: () -> Unit
) {
    val viewModel: XmpPlayerViewModel = koinViewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
    val prefs: XmpPreferences = koinInject()

    val backStack = rememberNavBackStack(NavKeyDownload.Search)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val history = remember { mutableStateListOf<Module>() }
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
                    history = history,
                    onBack = { backStack.removeLastOrNull() },
                    onClear = { history.clear() },
                    onModuleClick = { backStack.add(NavKeyDownload.Module(it)) },
                )
            }

            entry<NavKeyDownload.SearchResult> { it ->
                val viewModel = koinViewModel<DownloadViewModel>()
                DownloadResultScreen(
                    modifier = modifier,
                    viewModel = viewModel,
                    searchType = it.type,
                    query = it.query,
                    onBack = backStack::removeLastOrNull,
                    onModuleClick = { backStack.add(NavKeyDownload.Module(it)) },
                    onArtistClick = viewModel::getArtistById,
                )
            }

            entry<NavKeyDownload.Module> {
                val resultViewModel = koinViewModel<ModuleResultViewModel>()
                DownloadModuleScreen(
                    modifier = modifier,
                    viewModel = resultViewModel,
                    moduleId = it.moduleId,
                    onBack = { backStack.removeLastOrNull() },
                    onPlay = { module ->
                        scope.launch(Dispatchers.IO) {
                            val rootUriStr = prefs.getLastDirectoryUri() ?: return@launch
                            val rootUri = rootUriStr.toUri()
                            val filename = module.url.substringAfterLast('#')

                            // Walk the download dir to find the file URI
                            val fileUri = findDownloadedModule(
                                context = context,
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

private fun findDownloadedModule(
    context: Context,
    rootUri: Uri,
    artist: String,
    filename: String
): Uri? {
    return try {
        val rootDocId = DocumentsContract.getTreeDocumentId(rootUri)
        val cr = context.contentResolver

        // TheModArchive dir
        val tmaDocId = findChildDir(cr, rootUri, rootDocId, "TheModArchive") ?: return null
        // Artist dir
        val artistDocId = findChildDir(cr, rootUri, tmaDocId, artist) ?: tmaDocId
        // Find file by name
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, artistDocId)

        cr.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == filename) {
                    return DocumentsContract.buildDocumentUriUsingTree(rootUri, cursor.getString(0))
                }
            }
        }
        null
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}

private fun findChildDir(
    cr: ContentResolver,
    treeUri: Uri,
    parentDocId: String,
    name: String
): String? {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
    cr.query(
        childrenUri,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        ),
        null,
        null,
        null
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            if (cursor.getString(1) == name &&
                cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
            ) {
                return cursor.getString(0)
            }
        }
    }
    return null
}
