package com.lossydragon.media3.ui.screens.playlists.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.ui.theme.XmpTheme
import sh.calvin.reorderable.DragGestureDetector
import sh.calvin.reorderable.ReorderableCollectionItemScope

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReorderableCollectionItemScope.PlaylistEntryItem(
    index: Int,
    file: ModuleFile,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    ListItem(
        onClick = onPlay,
        shapes = ListItemDefaults.shapes(
            shape = MaterialTheme.shapes.small,
            focusedShape = MaterialTheme.shapes.small,
            pressedShape = MaterialTheme.shapes.small,
        ),
        content = {
            Text(
                text = file.resolvedName.ifBlank { file.name },
                maxLines = 1,
            )
        },
        supportingContent = {
            Text(text = file.resolvedType.ifBlank { file.extension.uppercase() })
        },
        leadingContent = {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
            )
        },
        trailingContent = {
            Row {
                IconButton(
                    onClick = onRemove,
                    content = {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                        )
                    }
                )
                IconButton(
                    modifier = Modifier.draggableHandle(
                        onDragStarted = {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.GestureThresholdActivate
                            )
                        },
                        onDragStopped = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        },
                    ),
                    onClick = {},
                    content = {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = null,
                        )
                    }
                )
            }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    val fakeScope = remember {
        object : ReorderableCollectionItemScope {
            override fun Modifier.draggableHandle(
                enabled: Boolean,
                interactionSource: MutableInteractionSource?,
                onDragStarted: (startedPosition: Offset) -> Unit,
                onDragStopped: () -> Unit,
                dragGestureDetector: DragGestureDetector
            ): Modifier = this

            override fun Modifier.longPressDraggableHandle(
                enabled: Boolean,
                interactionSource: MutableInteractionSource?,
                onDragStarted: (startedPosition: Offset) -> Unit,
                onDragStopped: () -> Unit
            ): Modifier = this
        }
    }

    XmpTheme {
        with(fakeScope) {
            PlaylistEntryItem(
                index = 1,
                file = ModuleFile(
                    uri = "content://preview/1".toUri(),
                    name = "aegis_-_beneath_the_fallen_stars.it",
                    sizeBytes = 1_820_792L,
                    extension = "it",
                    resolvedName = "Beneath the Fallen Stars",
                    resolvedType = "Impulse Tracker",
                ),
                onPlay = {},
                onRemove = {},
            )
        }
    }
}
