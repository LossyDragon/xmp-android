package com.lossydragon.media3.ui.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lossydragon.media3.model.ChannelSnapshot

@Composable
fun ChannelMeterGrid(channels: List<ChannelSnapshot>) {
    Column {
        Text(
            "Channels (${channels.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(channels) { idx, ch ->
                ChannelMeter(index = idx, channel = ch)
            }
        }
    }
}

@Composable
private fun ChannelMeter(
    index: Int,
    channel: ChannelSnapshot,
    width: Dp = 24.dp,
    height: Dp = 120.dp
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

    val barColor = when {
        animatedVol > 0.85f -> MaterialTheme.colorScheme.error
        animatedVol > 0.65f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(width),
        content = {
            Box(
                modifier = Modifier
                    .width(width - 4.dp)
                    .height(height)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.BottomCenter,
                content = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height * animatedFinalVol)
                            .background(barColor.copy(alpha = 0.25f)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height * animatedVol)
                            .background(barColor),
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
