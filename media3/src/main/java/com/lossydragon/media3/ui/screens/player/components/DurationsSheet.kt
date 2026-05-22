package com.lossydragon.media3.ui.screens.player.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DurationsSheet(
    sheetState: SheetState,
    sequenceDurations: ImmutableList<Int>,
    currentSequence: Int,
    onDismiss: () -> Unit,
    onItemClick: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        content = {
            Text(
                text = "Sub Songs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            DurationsList(
                modifier = Modifier.fillMaxWidth(),
                sequenceDurations = sequenceDurations,
                currentSequence = currentSequence,
                onItemClick = onItemClick
            )
        }
    )
}

@Composable
internal fun DurationsList(
    modifier: Modifier = Modifier,
    sequenceDurations: ImmutableList<Int>,
    currentSequence: Int,
    onItemClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to current item
    LaunchedEffect(currentSequence) {
        if (currentSequence in 0..<sequenceDurations.size) {
            listState.animateScrollToItem(currentSequence)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = {
            itemsIndexed(
                items = sequenceDurations,
                key = { idx, _ -> idx },
            ) { idx, duration ->
                DurationItem(
                    isCurrentItem = idx == currentSequence,
                    index = idx,
                    duration = duration,
                    onItemClick = onItemClick,
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DurationItem(
    isCurrentItem: Boolean,
    index: Int,
    duration: Int,
    onItemClick: (Int) -> Unit
) {
    val minutes = duration / 60000
    val seconds = (duration / 1000) % 60
    val label = if (index == 0) "Main Song" else "Sub Song $index"
    val timeText = "%d:%02d — %s".format(minutes, seconds, label)

    val background = if (isCurrentItem) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    ListItem(
        onClick = { onItemClick(index) },
        modifier = Modifier
            .fillMaxWidth()
            .background(background),
        content = {
            Text(
                text = timeText,
                style = if (isCurrentItem) {
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { RadioButton(selected = isCurrentItem, onClick = null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    val sheetState = rememberBottomSheetState(
        initialValue = Expanded,
        enabledValues = setOf(Hidden, Expanded),
    )
    XmpTheme {
        Scaffold { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )

            DurationsSheet(
                sheetState = sheetState,
                sequenceDurations = listOf(
                    183_000, // 3:03 — Main Song
                    94_000, // 1:34
                    211_000, // 3:31
                    57_000, // 0:57
                    142_000, // 2:22
                    78_000, // 1:18
                    305_000, // 5:05
                    33_000, // 0:33
                    167_000, // 2:47
                    120_000, // 2:00
                ).toPersistentList(),
                currentSequence = 5,
                onItemClick = {},
                onDismiss = {},
            )
        }
    }
}
