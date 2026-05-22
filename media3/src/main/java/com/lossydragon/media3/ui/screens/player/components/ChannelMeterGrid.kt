package com.lossydragon.media3.ui.screens.player.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.lossydragon.media3.model.ChannelSnapshot
import com.lossydragon.media3.ui.theme.XmpTheme
import com.materialkolor.ktx.darken
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Suppress("ParamsComparedByRef")
@Composable
fun ChannelMeterGrid(
    modifier: Modifier = Modifier,
    channels: ImmutableList<ChannelSnapshot>
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = {
                    val rows = when {
                        channels.size <= 16 -> 1
                        channels.size <= 32 -> 2
                        channels.size <= 48 -> 3
                        else -> 4
                    }
                    val columns = (channels.size + rows - 1) / rows
                    val labelHeight = 16.dp
                    val barHeight =
                        (maxHeight - 24.dp - if (rows > 1) 4.dp else 0.dp) / rows - labelHeight

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(count = columns),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.Center, // .spacedBy(4.dp),
                        userScrollEnabled = false,
                        content = {
                            itemsIndexed(
                                items = channels,
                                itemContent = { idx, ch ->
                                    ChannelMeter(
                                        index = idx,
                                        channel = ch,
                                        height = barHeight,
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
    )
}

@Composable
private fun ChannelMeter(
    index: Int,
    channel: ChannelSnapshot,
    width: Dp = 24.dp,
    height: Dp
) {
    val volFraction = (channel.volume / 64f).coerceIn(0f, 1f)
    val finalVolFraction = (channel.finalVol / 64f).coerceIn(0f, 1f)

    val animatedVol by animateFloatAsState(
        targetValue = volFraction,
        animationSpec = tween(durationMillis = 40),
        label = "vol_ch$index",
    )
    val animatedFinalVol by animateFloatAsState(
        targetValue = finalVolFraction,
        animationSpec = tween(durationMillis = 80),
        label = "finalVol_ch$index",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(width),
        content = {
            Box(
                modifier = Modifier
                    .width(width - 4.dp)
                    .height(height)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color.DarkGray.darken(1.25f)),
                contentAlignment = Alignment.BottomCenter,
                content = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height * animatedFinalVol)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height * animatedVol)
                            .background(MaterialTheme.colorScheme.inversePrimary),
                    )
                }
            )
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        Surface {
            ChannelMeterGrid(
                channels = Array(64) {
                    ChannelSnapshot(
                        volume = (it + 1) * 5,
                        finalVol = (it + 2) * 5,
                        pan = 0,
                        instrument = 0,
                        note = 0,
                        period = 0,
                    )
                }.toImmutableList()
            )
        }
    }
}
