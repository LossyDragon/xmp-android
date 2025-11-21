package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import java.util.Locale
import kotlin.random.Random
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.RadioButtonItem
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerSheetState

@Stable
sealed class PlayerSheetEvent {
    data class OnSequence(val seq: Int) : PlayerSheetEvent()
    data object OnAllSeq : PlayerSheetEvent()
    data object OnMessage : PlayerSheetEvent()
    data object OnAddToPlaylist : PlayerSheetEvent()
}

@Stable
data class SubSongItem(val index: Int, val string: String)

@Composable
fun PlayerSheet(
    modifier: Modifier = Modifier,
    state: PlayerSheetState,
    onEvent: (PlayerSheetEvent) -> Unit
) {
    val context = LocalContext.current
    val lazyState = rememberLazyListState()

    val subSongSequences = remember(state.numOfSequences) {
        val main = context.getString(R.string.sidebar_main_song)
        state.numOfSequences.mapIndexed { index, item ->
            val sub = context.getString(R.string.sidebar_sub_song, index)
            val text = if (index == 0) main else sub
            val string = String.format(
                Locale.getDefault(),
                "%2d:%02d (%s)",
                item / 60000,
                item / 1000 % 60,
                text
            )
            SubSongItem(index, string)
        }
    }

    Surface {
        Column(
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            ModuleSection(text = stringResource(id = R.string.sidebar_add_playlist)) {
                IconButton(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.End),
                    onClick = { onEvent(PlayerSheetEvent.OnAddToPlaylist) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ModuleSection(text = stringResource(id = R.string.sidebar_details)) {
                IconButton(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.End),
                    onClick = { onEvent(PlayerSheetEvent.OnMessage) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            ModuleInsDetails(stringResource(id = R.string.sidebar_channels), state.moduleInfo[3])

            ModuleInsDetails(stringResource(id = R.string.sidebar_instruments), state.moduleInfo[1])

            ModuleInsDetails(stringResource(id = R.string.sidebar_patterns), state.moduleInfo[0])

            ModuleInsDetails(stringResource(id = R.string.sidebar_samples), state.moduleInfo[2])

            ModuleInsDetails(stringResource(id = R.string.sidebar_length), state.moduleInfo[4])

            Spacer(modifier = Modifier.height(12.dp))

            ModuleSection(text = stringResource(id = R.string.sidebar_subsongs)) {
                Switch(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.End),
                    checked = state.isPlayAllSequences,
                    onCheckedChange = { onEvent(PlayerSheetEvent.OnAllSeq) }
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(state = lazyState) {
                items(subSongSequences) { item ->
                    val onClick = remember(item) {
                        {
                            onEvent(PlayerSheetEvent.OnSequence(item.index))
                        }
                    }

                    RadioButtonItem(
                        index = item.index,
                        selection = state.currentSequence,
                        text = item.string,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleSection(
    text: String,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier.height(48.dp),
        shape = shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.Start),
                text = text,
            )

            content()
        }
    }
}

@Composable
private fun ModuleInsDetails(
    string: String,
    number: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 28.dp)
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.Start),
            text = string,
            fontSize = 14.sp,
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.End),
            text = number.toString(),
            fontSize = 14.sp,
        )
    }
}

@Preview
@Composable
private fun Preview_PlayerSheet() {
    XmpTheme(useDarkTheme = true) {
        PlayerSheet(
            state = PlayerSheetState(
                moduleInfo = listOf(111, 222, 333, 444, 555),
                isPlayAllSequences = true,
                currentSequence = 1,
                numOfSequences = List(12) {
                    Random.nextInt(100, 10_000)
                }
            ),
            onEvent = { },
        )
    }
}
