package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily
import org.helllabs.android.xmp.compose.theme.seed
import org.helllabs.android.xmp.compose.ui.player.ChannelMuteState
import org.helllabs.android.xmp.compose.ui.player.SampleDataState
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars

// TODO: 2 Column support on wider screens or in landscape.

val c = CharArray(2)

private val waveformStroke = Stroke(
    width = 0.75f,
    cap = StrokeCap.Butt,
    join = StrokeJoin.Bevel,
)
private val channelTextStyle = TextStyle(
    color = Color(200, 200, 200, 255),
    fontSize = 14.sp,
    fontFamily = FontFamily.Monospace
)
private val instrumentTextStyle = TextStyle(
    color = Color(200, 200, 200, 255),
    fontSize = 12.sp,
    fontFamily = FontFamily.Monospace
)
private val muteTextStyle = TextStyle(
    color = Color.White,
    fontSize = 8.sp,
    fontWeight = FontWeight.Bold,
    fontFamily = michromaFontFamily,
)
private val backgroundColor = Color(40, 40, 40, 255)
private val mutedBackgroundColor = Color(60, 0, 0, 255)
private val waveformColor = Color.Green

@Composable
fun ComposeChannelViewer(
    onTap: () -> Unit,
    channelInfo: ChannelInfo,
    frameInfo: FrameInfo,
    insName: ImmutableList<String>,
    isMuted: ChannelMuteState,
    modVars: ModVars,
    sampleData: SampleDataState
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current

    val dimensions = remember(density) {
        ChannelViewerDimensions(density)
    }
    var canvasSize by remember {
        mutableStateOf(Size.Zero)
    }
    val yOffset = remember {
        Animatable(0f)
    }
    val channelData = remember(modVars) {
        ChannelViewerData()
    }
    val channelNumber = remember(modVars.numChannels) {
        (0 until modVars.numChannels).map {
            "${it + 1}"
        }
    }
    val scrollState = rememberScrollableState { delta ->
        scope.launch {
            val totalContentHeight = dimensions.yMultiplier * modVars.numChannels
            val maxOffset = (totalContentHeight - canvasSize.height).coerceAtLeast(0f)
            val newOffset = (yOffset.value + delta).coerceIn(-maxOffset, 0f)
            yOffset.snapTo(newOffset)
        }
        delta
    }
    val waveformPaths = remember(modVars) {
        Array(modVars.numChannels) { Path() }
    }
    val isChnMuted by remember(isMuted) {
        derivedStateOf { isMuted.isMuted }
    }
    val visibleChannelRange by remember(modVars.numChannels, canvasSize, dimensions.yMultiplier) {
        derivedStateOf {
            val numChannels = modVars.numChannels
            if (canvasSize.height == 0f || numChannels == 0) {
                IntRange.EMPTY
            } else {
                val start = ((-yOffset.value) / dimensions.yMultiplier).toInt()
                    .coerceIn(0, numChannels - 1)

                val end = (((-yOffset.value) + canvasSize.height) / dimensions.yMultiplier).toInt()
                    .coerceIn(start, numChannels - 1)

                start..end
            }
        }
    }

    LaunchedEffect(modVars.numInstruments, modVars.numChannels) {
        scope.launch {
            yOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(
                orientation = Orientation.Vertical,
                state = scrollState
            )
            .pointerInput(isChnMuted) {
                detectTapGestures(
                    onTap = { offset ->
                        val adjustedOffset = offset.copy(y = offset.y - yOffset.value)
                        for (chn in 0 until modVars.numChannels) {
                            val scopeRect = getScopeRect(chn, dimensions, yOffset.value)
                            if (scopeRect.contains(adjustedOffset)) {
                                Xmp.mute(chn, 2)
                                return@detectTapGestures
                            }
                        }
                        onTap()
                    },
                    onLongPress = { offset ->
                        val adjustedOffset = offset.copy(y = offset.y - yOffset.value)
                        for (chn in 0 until modVars.numChannels) {
                            val scopeRect = getScopeRect(chn, dimensions, yOffset.value)
                            if (scopeRect.contains(adjustedOffset)) {
                                val unMuteCount = isChnMuted.count { !it }
                                if (unMuteCount == 1) {
                                    for (i in 0 until modVars.numChannels) {
                                        Xmp.mute(i, 0)
                                    }
                                } else {
                                    for (i in 0 until modVars.numChannels) {
                                        Xmp.mute(i, if (i == chn) 0 else 1)
                                    }
                                }
                            }
                        }
                    }
                )
            }
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        drawChannels(
            visibleChannelRange = visibleChannelRange,
            channelInfo = channelInfo,
            insName = insName,
            isChnMuted = isChnMuted,
            modVars = modVars,
            dimensions = dimensions,
            channelData = channelData,
            channelNumber = channelNumber,
            yOffset = yOffset.value,
            sampleData = sampleData,
            waveformPaths = waveformPaths,
            textMeasurer = textMeasurer
        )

        if (view.isInEditMode) {
            debugScreen(
                textMeasurer = textMeasurer,
                xValue = dimensions.xMultiplier,
                yValue = dimensions.yMultiplier
            )
        }
    }
}

private fun DrawScope.drawChannels(
    visibleChannelRange: IntRange,
    channelInfo: ChannelInfo,
    insName: ImmutableList<String>,
    isChnMuted: ImmutableList<Boolean>,
    modVars: ModVars,
    dimensions: ChannelViewerDimensions,
    channelData: ChannelViewerData,
    channelNumber: List<String>,
    yOffset: Float,
    sampleData: SampleDataState,
    waveformPaths: Array<Path>,
    textMeasurer: TextMeasurer
) {
    val xMult = dimensions.xMultiplier
    val yMult = dimensions.yMultiplier
    val scopeWidth = dimensions.scopeWidth
    val barWidth = dimensions.barWidth

    for (chn in visibleChannelRange) {
        if (chn >= isChnMuted.size) {
            continue
        }

        val ins = channelInfo.instruments[chn]
        val pan = channelInfo.pans[chn]
        val isMuted = isChnMuted[chn]

        // Calculate reused offsets for this channel once
        val channelY = yMult * chn
        val chnYWithOffset = channelY + yOffset
        val labelCenterY = chnYWithOffset + yMult / 2

        // Channel number
        val chnText = textMeasurer.measure(
            text = AnnotatedString(channelNumber[chn]),
            style = channelTextStyle
        )

        val textCenterX = xMult / 2 - chnText.size.width / 2
        val textCenterY = labelCenterY - chnText.size.height / 2

        drawText(
            textLayoutResult = chnText,
            color = Color.White,
            topLeft = Offset(textCenterX, textCenterY)
        )

        // Instrument name
        if (ins in 0..<modVars.numInstruments) {
            val chnNameText = textMeasurer.measure(
                text = AnnotatedString(if (isMuted) "---" else insName[ins]),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = instrumentTextStyle
            )

            drawText(
                textLayoutResult = chnNameText,
                topLeft = Offset(
                    x = xMult * 4,
                    y = chnYWithOffset + yMult / 4
                )
            )
        }

        val barY = chnYWithOffset + yMult - yMult / 3
        val barX = xMult * 4

        // Volume bar background
        drawRect(
            color = backgroundColor,
            topLeft = Offset(x = barX, y = barY),
            size = Size(barWidth, 16f)
        )

        // Volume bars
        if (!isMuted) {
            val vol = channelInfo.volumes[chn]
            if (vol > 0) {
                val volWidth = barWidth * (vol.toFloat() / 64)
                drawRect(
                    color = seed.copy(alpha = .35f),
                    topLeft = Offset(x = barX, y = barY),
                    size = Size(volWidth, 16f)
                )
            }

            val fVol = channelInfo.finalVols[chn]
            if (fVol > 0) {
                val fVolWidth = barWidth * (fVol.toFloat() / 64)
                drawRect(
                    color = seed,
                    topLeft = Offset(x = barX, y = barY),
                    size = Size(fVolWidth, 16f)
                )
            }
        }

        // Pan bar background
        val panBarX = xMult * 10
        drawRect(
            color = backgroundColor,
            topLeft = Offset(x = panBarX, y = barY),
            size = Size(barWidth, 16f)
        )

        // Pan bar
        if (!isMuted) {
            val panRectWidth = 8.dp.toPx()
            val panMaxOffset = barWidth - panRectWidth
            val panOffset = panMaxOffset * (pan.toFloat() / 255)

            drawRect(
                color = seed,
                topLeft = Offset(x = panBarX + panOffset, y = barY),
                size = Size(panRectWidth, 16f)
            )
        }

        // Scope area
        val scopeXOffset = xMult + xMult / 4
        val scopeYOffset = chnYWithOffset + yMult / 6
        val scopeHeight = yMult - yMult / 3

        if (isMuted) {
            drawRect(
                color = mutedBackgroundColor,
                size = Size(width = scopeWidth, height = scopeHeight),
                topLeft = Offset(x = scopeXOffset, y = scopeYOffset)
            )

            val muteText = channelData.muteLabelCache ?: textMeasurer.measure(
                text = AnnotatedString("MUTE"),
                style = muteTextStyle
            ).also { channelData.muteLabelCache = it }

            drawText(
                textLayoutResult = muteText,
                topLeft = Offset(
                    x = scopeXOffset + (scopeWidth - muteText.size.width) / 2,
                    y = scopeYOffset + (scopeHeight - muteText.size.height) / 2
                )
            )
        } else {
            // Channel scope background
            drawRect(
                color = backgroundColor,
                size = Size(width = scopeWidth, height = scopeHeight),
                topLeft = Offset(x = scopeXOffset, y = scopeYOffset)
            )

            // Get the buffer for this channel from sampleData
            val buffer = if (chn < sampleData.buffers.size) {
                sampleData.buffers[chn]
            } else {
                null
            }

            // Draw waveform
            val centerY = scopeYOffset + (scopeHeight / 2)
            val halfHeight = scopeHeight / 2
            val volumeScale = channelInfo.finalVols[chn].coerceIn(0, 64) / 64f

            if (buffer != null && buffer.isNotEmpty() && volumeScale > 0.01f) {
                val widthScale = scopeWidth / buffer.size

                var hasNonZeroValue = false
                var minVal = 127f
                var maxVal = -128f

                for (i in buffer.indices) {
                    val value = buffer[i].toInt()
                    if (value != 0) {
                        hasNonZeroValue = true
                        if (value < minVal) minVal = value.toFloat()
                        if (value > maxVal) maxVal = value.toFloat()
                    }
                }

                if (hasNonZeroValue && maxVal > minVal) {
                    val range = maxVal - minVal

                    val currentPath = waveformPaths[chn]
                    currentPath.reset()

                    var index = 0
                    while (index < buffer.size && buffer[index].toInt() == 0) {
                        index++
                    }

                    if (index < buffer.size) {
                        val byteValue = buffer[index].toInt().toFloat()
                        val normalizedValue = ((byteValue - minVal) / range - 0.5f) * 2f
                        val x = scopeXOffset + widthScale * index
                        val y = centerY - (normalizedValue * halfHeight * volumeScale)
                        currentPath.moveTo(x, y)

                        for (i in index + 1 until buffer.size) {
                            val nextValue = buffer[i].toInt().toFloat()
                            if (nextValue != 0f || (i > 0 && buffer[i - 1].toInt() != 0)) {
                                val nextNormalized = ((nextValue - minVal) / range - 0.5f) * 2f
                                val nextX = scopeXOffset + widthScale * i
                                val nextY = centerY - (nextNormalized * halfHeight * volumeScale)
                                currentPath.lineTo(nextX, nextY)
                            }
                        }

                        drawPath(
                            path = currentPath,
                            color = waveformColor,
                            style = waveformStroke
                        )
                    }
                } else {
                    drawFlatLine(scopeXOffset, centerY, scopeWidth)
                }
            } else {
                drawFlatLine(scopeXOffset, centerY, scopeWidth)
            }
        }
    }
}

private fun getScopeRect(chn: Int, dimensions: ChannelViewerDimensions, yOffset: Float): Rect {
    val scopeXOffset = dimensions.xMultiplier + dimensions.xMultiplier.div(4)
    val scopeYOffset = dimensions.yMultiplier.times(chn) + dimensions.yMultiplier.div(6)
    val scopeHeight = dimensions.yMultiplier - dimensions.yMultiplier.div(3)

    return Rect(
        left = scopeXOffset,
        top = scopeYOffset,
        right = scopeXOffset + dimensions.scopeWidth,
        bottom = scopeYOffset + scopeHeight
    )
}

private fun DrawScope.drawFlatLine(scopeXOffset: Float, centerY: Float, scopeWidth: Float) {
    drawLine(
        color = waveformColor,
        start = Offset(scopeXOffset, centerY),
        end = Offset(scopeXOffset + scopeWidth, centerY),
        strokeWidth = 0.75f
    )
}

private class ChannelViewerDimensions(density: Density) {
    val xMultiplier: Float
    val yMultiplier: Float
    val barWidth: Float
    val scopeWidth: Float

    init {
        with(density) {
            xMultiplier = 24.dp.toPx()
            yMultiplier = 56.dp.toPx()

            barWidth = if (density.density <= 3.0) {
                xMultiplier.times(4)
            } else {
                xMultiplier.times(5)
            }

            scopeWidth = xMultiplier.times(3) - xMultiplier.div(2)
        }
    }
}

private class ChannelViewerData {
    var muteLabelCache: TextLayoutResult? = null
}

/** Preview **/

@Preview(device = "id:pixel_6_pro")
@Preview(name = "Huawei P20 lite", device = "spec:width=1080px,height=2280px,dpi=480")
@Composable
private fun Preview_ChannelViewer() {
    val modVars = composeSampleModVars()
    XmpTheme(useDarkTheme = true) {
        ComposeChannelViewer(
            onTap = {},
            channelInfo = composeSampleChannelInfo(),
            frameInfo = composeSampleFrameInfo(),
            isMuted = ChannelMuteState(
                isMuted = List(modVars.numChannels) {
                    it % 2 == 0
                }.toPersistentList()
            ),
            modVars = modVars,
            insName = List(modVars.numInstruments) {
                String.format("%02X %s", it + 1, "Instrument Name")
            }.toPersistentList(),
            sampleData = SampleDataState()
        )
    }
}
