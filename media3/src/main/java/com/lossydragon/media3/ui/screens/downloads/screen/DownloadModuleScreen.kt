package com.lossydragon.media3.ui.screens.downloads.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lossydragon.media3.model.DownloadStatus
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.model.ModuleResult
import com.lossydragon.media3.model.ModuleResultState
import com.lossydragon.media3.ui.components.GuruBox
import com.lossydragon.media3.ui.screens.downloads.components.ModuleDetailLayout
import com.lossydragon.media3.ui.screens.downloads.viewmodel.ModuleResultViewModel
import com.lossydragon.media3.ui.theme.XmpTheme
import com.lossydragon.media3.util.shareLink
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DownloadModuleScreen(
    modifier: Modifier = Modifier,
    moduleId: Int,
    onBack: () -> Unit,
    onPlay: (Module) -> Unit
) {
    val viewModel = koinViewModel<ModuleResultViewModel>()

    val state by viewModel.state.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Only fetch if we don't already have a module loaded
        // or if the requested ID differs from what's currently loaded
        if (state.result == null || (moduleId >= 0 && state.result?.module?.id != moduleId)) {
            viewModel.getModuleById(moduleId)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Delete Module") },
            text = { Text("Delete ${state.result?.module?.filename}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.result?.module?.let { viewModel.deleteModule(it) }
                        showDeleteDialog = false
                    },
                    content = { Text("Delete") }
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    content = { Text("Cancel") }
                )
            }
        )
    }

    DownloadModuleContent(
        modifier = modifier,
        state = state,
        onBack = onBack,
        onShowDialog = { showDeleteDialog = it },
        onPlay = onPlay,
        onDownloadModule = viewModel::downloadModule,
        onRandomModule = viewModel::getRandomModule,
    )
}

@Composable
private fun DownloadModuleContent(
    modifier: Modifier = Modifier,
    state: ModuleResultState,
    onBack: () -> Unit,
    onShowDialog: (Boolean) -> Unit,
    onDownloadModule: (Module) -> Unit,
    onRandomModule: () -> Unit,
    onPlay: (Module) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isRandom) "Random Module" else "Module Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
                },
                actions = {
                    if (state.moduleExists) {
                        IconButton(
                            onClick = { onShowDialog(true) },
                            content = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                    state.result?.module?.infopage?.let { url ->
                        val context = LocalContext.current
                        val module = state.result.module
                        if (url.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val title = module.songtitle.ifEmpty { module.filename }
                                    val artist = module.artist
                                    val infoPage = module.infopage
                                    val message = """
                                        $title (by $artist) from The Mod Archive:
                                        $infoPage
                                    """.trimIndent()
                                    context.shareLink(message = message)
                                },
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Progress bar — only visible while downloading
                    when (val status = state.downloadStatus) {
                        is DownloadStatus.Progress -> LinearProgressIndicator(
                            progress = { status.percent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        is DownloadStatus.Loading -> LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )

                        else -> Spacer(modifier = Modifier.height(4.dp)) // keep layout stable
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        content = {
                            val module = state.result?.module
                            val isDownloading = state.downloadStatus is DownloadStatus.Loading ||
                                state.downloadStatus is DownloadStatus.Progress
                            val buttonLabel = when {
                                state.isLoading -> "Loading..."

                                isDownloading -> when (val s = state.downloadStatus) {
                                    is DownloadStatus.Progress -> "%.0f%%".format(s.percent)
                                    else -> "Downloading..."
                                }

                                state.moduleExists -> "Play"

                                module?.isSupported == false -> "Unsupported"

                                else -> "Download"
                            }

                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = !state.isLoading && !isDownloading &&
                                    module?.isSupported != false,
                                onClick = {
                                    if (state.moduleExists) {
                                        module?.let { onPlay(it) }
                                    } else {
                                        module?.let { onDownloadModule(it) }
                                    }
                                },
                                content = { Text(text = buttonLabel) }
                            )

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = !state.isLoading && !isDownloading,
                                onClick = onRandomModule,
                                content = { Text(text = "Random") }
                            )
                        }
                    )
                }
            }
        },
        content = { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = {
                    if (state.isLoading) CircularProgressIndicator()

                    state.softError?.let {
                        GuruBox(message = it, onBack = onBack)
                    }

                    state.result?.let { result ->
                        ModuleDetailLayout(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            moduleResult = result,
                        )
                    }
                }
            )
        }
    )
}

private class DownloadModulePreviewParameter : PreviewParameterProvider<ModuleResultState> {
    private val sampleModule = ModuleResult(
        module = Module(
            filename = "alpharapii.mod",
            songtitle = "alpharapii",
            format = "MOD",
            bytes = 45678,
            infopage = "website"
        )
    )

    override val values = sequenceOf(
        ModuleResultState(isLoading = true),
        ModuleResultState(isRandom = true, result = sampleModule),
        ModuleResultState(result = sampleModule),
        ModuleResultState(result = sampleModule, moduleExists = true),
        ModuleResultState(result = sampleModule, downloadStatus = DownloadStatus.Loading),
        ModuleResultState(result = sampleModule, downloadStatus = DownloadStatus.Progress(66f)),
        ModuleResultState(result = sampleModule, downloadStatus = DownloadStatus.Success),
        ModuleResultState(softError = "Could not fetch module."),
    )
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(DownloadModulePreviewParameter::class) state: ModuleResultState
) {
    XmpTheme {
        DownloadModuleContent(
            state = state,
            onBack = {},
            onShowDialog = {},
            onDownloadModule = {},
            onRandomModule = {},
            onPlay = {},
        )
    }
}
