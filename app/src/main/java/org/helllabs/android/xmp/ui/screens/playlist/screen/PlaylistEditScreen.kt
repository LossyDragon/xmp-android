package org.helllabs.android.xmp.ui.screens.playlist.screen

import android.net.Uri
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
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.ui.components.KoinPreview
import org.helllabs.android.xmp.ui.components.MessageDialog
import org.helllabs.android.xmp.ui.components.ProgressbarIndicator
import org.koin.compose.koinInject
import timber.log.Timber

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistEditScreen(
    uri: Uri?,
    onBack: (Boolean) -> Unit,
    onDeleted: (Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val playlistManager = koinInject<PlaylistManager>()
    val scope = rememberCoroutineScope()

    var playlist by remember(uri) { mutableStateOf(Playlist()) }
    var title by rememberSaveable(playlist) { mutableStateOf(playlist.name) }
    var description by rememberSaveable(playlist) { mutableStateOf(playlist.comment) }

    LaunchedEffect(Unit) {
        delay(250.milliseconds) // Slight delay to request focus
        focusRequester.requestFocus()
    }

    LaunchedEffect(uri) {
        if (uri != null) {
            playlist = playlistManager.loadPlaylist(uri).getOrThrow()
        }
    }

    var isPendingDeleteLoading by remember { mutableStateOf(false) }
    var isPendingDelete by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .imePadding()
            .fillMaxSize()
    ) {
        MessageDialog(
            isShowing = isPendingDelete,
            icon = Icons.Default.DeleteForever,
            title = "Delete Playlist",
            text = "Are you sure you want to delete ${playlist.name}?",
            confirmText = "Delete",
            onConfirm = {
                isPendingDelete = false
                isPendingDeleteLoading = true
                scope.launch {
                    playlistManager.deletePlaylist(uri!!).fold(
                        onSuccess = { result ->
                            isPendingDeleteLoading = false
                            onDeleted(result)
                        },
                        onFailure = {
                            Timber.e(it, "Failed to delete playlist")
                            isPendingDeleteLoading = false
                            onDeleted(false)
                        }
                    )
                }
            },
            onDismiss = { isPendingDelete = false }
        )

        AlertDialog(
            // properties = DialogProperties(decorFitsSystemWindows = false),
            onDismissRequest = { onBack(false) },
            icon = {
                val icon = if (uri == null) {
                    Icons.AutoMirrored.Filled.PlaylistAdd
                } else {
                    Icons.AutoMirrored.Filled.PlaylistAddCheck
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            },
            title = {
                val title = if (uri == null) {
                    "New Playlist"
                } else {
                    "EditPlaylist"
                }
                Text(text = title)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = {
                        if (isPendingDeleteLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                                content = {
                                    ProgressbarIndicator()
                                }
                            )
                        } else {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                value = title,
                                onValueChange = { title = it },
                                isError = title.isEmpty(),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Search,
                                    keyboardType = KeyboardType.Text
                                ),
                                maxLines = 1,
                                label = {
                                    Text(
                                        text = "Title",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                shape = MaterialTheme.shapes.largeIncreased,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    errorBorderColor = MaterialTheme.colorScheme.error,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = description,
                                onValueChange = { description = it },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Search,
                                    keyboardType = KeyboardType.Text
                                ),
                                maxLines = 2,
                                label = {
                                    Text(
                                        text = "Description (Optional)",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                shape = MaterialTheme.shapes.largeIncreased,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    errorBorderColor = MaterialTheme.colorScheme.error,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isPendingDeleteLoading,
                    onClick = {
                        scope.launch {
                            if (uri == null) {
                                /* New */
                                playlist = playlistManager
                                    .createPlaylist(title, description)
                                    .getOrThrow()
                            } else {
                                /* Update */
                                var updatedUri: Uri = uri

                                playlist = playlistManager.setComment(playlist, description)

                                playlistManager.renamePlaylist(uri, playlist, title)
                                    .onSuccess { newUri ->
                                        updatedUri = newUri
                                        playlist = playlist.copy(name = title)
                                    }
                                    .getOrThrow()

                                playlistManager.savePlaylist(updatedUri, playlist)
                                    .getOrThrow()
                            }

                            onBack(true)
                        }
                    },
                    content = {
                        val text = if (uri == null) "Create" else "Update"
                        Text(text = text)
                    }
                )
            },
            dismissButton = {
                TextButton(
                    enabled = !isPendingDeleteLoading,
                    onClick = { onBack(false) },
                    content = {
                        Text(text = "Cancel")
                    }
                )

                if (uri != null) {
                    TextButton(
                        enabled = !isPendingDeleteLoading,
                        onClick = { isPendingDelete = true },
                        content = { Text(text = "Delete") }
                    )
                }
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    KoinPreview {
        Surface {
            PlaylistEditScreen(
                uri = null,
                onBack = { },
                onDeleted = { },
            )
        }
    }
}

@Preview
@Composable
private fun Preview2() {
    KoinPreview {
        Surface {
            PlaylistEditScreen(
                uri = Uri.EMPTY,
                onBack = { },
                onDeleted = { },
            )
        }
    }
}
