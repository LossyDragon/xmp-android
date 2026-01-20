package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.seed
import org.helllabs.android.xmp.compose.ui.player.ChannelMuteState
import org.helllabs.android.xmp.compose.ui.player.PatternDataState
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

// Maybe keep the row numbers in view at all times, and move the channel columns instead?

// TODO
//  1. I broke preview
//  3. New song render issues (first few channels don't render if scrolled)

private val headerTextStyle = TextStyle(
    color = Color.White,
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold
)
private val rowTextStyle = TextStyle(
    fontSize = 11.sp,
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    platformStyle = PlatformTextStyle(includeFontPadding = true)
)
private val patternTextStyle = TextStyle(
    fontSize = 14.sp,
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold
)
private val noteColor = Color(140, 140, 160)
private val noteColorMuted = Color(60, 60, 60)
private val instrumentColor = Color(160, 80, 80)
private val instrumentColorMuted = Color(80, 40, 40)
private val effectColor = Color(34, 158, 60)
private val effectColorMuted = Color(16, 75, 28)
private val backgroundColor = Color(0x0D888888)

@Composable
internal fun ComposePatternViewer(
    onTap: () -> Unit,
    fi: FrameInfo,
    isMuted: ChannelMuteState,
    modType: String,
    modVars: ModVars,
    patternData: PatternDataState,
    onVisibleRowRangeChanged: (IntRange) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val infoTextMeasurer = rememberTextMeasurer(4096)
    val rowTextMeasurer = rememberTextMeasurer(256)
    val headerTextMeasurer = rememberTextMeasurer(64)

    val dimensions = remember(density) {
        PatternViewerDimensions(density)
    }

    val xAxisMultiplier = dimensions.xAxisMultiplier
    val yAxisMultiplier = dimensions.yAxisMultiplier

    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val offsetX = remember { Animatable(0f) }

    // Cache effect table based on module type
    var currentType by remember { mutableStateOf("") }
    val effectsTable = remember(modType) {
        val type = Effects.getEffectList(modType)
        currentType = type.name
        type.table
    }

    val rowText = remember(fi.numRows) {
        (0..fi.numRows).map {
            rowTextMeasurer.measure(
                text = AnnotatedString(it.toString()),
                style = rowTextStyle.copy(
                    background = if (view.isInEditMode) Color.Green else Color.Unspecified
                )
            )
        }
    }

    val headerText = remember(modVars.numChannels) {
        (0 until modVars.numChannels).map {
            headerTextMeasurer.measure(
                text = AnnotatedString("${it + 1}"),
                style = headerTextStyle
            )
        }
    }

    val textCache = remember(modVars.numChannels, fi.numRows) {
        Array(fi.numRows) {
            Array(modVars.numChannels) {
                PatternTextCacheEntry()
            }
        }
    }

    // Current state of muted channels
    val mutedState = remember(isMuted) { isMuted.isMuted }

    // Calculate key layout positions
    val barLineY = remember(canvasSize, yAxisMultiplier) {
        canvasSize.height.div(2).div(yAxisMultiplier).toInt().times(yAxisMultiplier)
    }

    val currentRow = fi.row.toFloat()
    val rowYOffset = remember(barLineY, currentRow, yAxisMultiplier) {
        barLineY - (currentRow * yAxisMultiplier)
    }

    val visibleRowRange = remember(canvasSize, rowYOffset, yAxisMultiplier) {
        val firstVisible = ((yAxisMultiplier - rowYOffset) / yAxisMultiplier).toInt()
            .coerceAtLeast(0)
        val lastVisible = ((canvasSize.height - rowYOffset) / yAxisMultiplier).toInt()
            .coerceAtMost(fi.numRows - 1)

        firstVisible..lastVisible
    }

    // Dynamically calculate visible channels based on scroll position
    val visibleChannelRange by remember(canvasSize, modVars.numChannels, xAxisMultiplier) {
        derivedStateOf {
            val padding = 2
            val first = ((-offsetX.value) / (xAxisMultiplier * 3)).toInt() - padding
            val last =
                ((canvasSize.width - offsetX.value) / (xAxisMultiplier * 3)).toInt() + padding

            first.coerceAtLeast(0)..last.coerceAtMost(modVars.numChannels - 1)
        }
    }

    val scrollState = rememberScrollableState { delta ->
        val totalContentWidth = (modVars.numChannels * 3 + 1) * xAxisMultiplier
        val minOffsetX = (canvasSize.width - totalContentWidth).coerceAtMost(0f)
        val current = offsetX.value
        val target = (current + delta).coerceIn(minOffsetX, 0f)

        if (current != target) {
            scope.launch { offsetX.snapTo(target) }
            target - current
        } else {
            0f
        }
    }

    // Reset scroll position
    LaunchedEffect(modVars.numChannels, canvasSize /*, fi.pattern */) {
        Timber.d("Resetting scroll position")
        scope.launch {
            offsetX.snapTo(0f)
        }
    }

    LaunchedEffect(visibleRowRange) {
        onVisibleRowRangeChanged(visibleRowRange)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(
                orientation = Orientation.Horizontal,
                state = scrollState,
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        // Column backgrounds
        for (i in 1 until modVars.numChannels) {
            if (i % 2 == 1) {
                // Only draw for odd columns
                drawRect(
                    color = backgroundColor,
                    topLeft = Offset(
                        x = (i * 3 + 1) * xAxisMultiplier + offsetX.value,
                        y = 0f
                    ),
                    size = Size(width = xAxisMultiplier * 3, height = canvasSize.height)
                )
            }
        }

        // Header background
        drawRect(
            color = seed,
            size = Size(canvasSize.width, yAxisMultiplier),
            topLeft = Offset(0f, 0f)
        )

        // Current row highlight bar
        val dynamicBarLineY = barLineY
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(0f, dynamicBarLineY),
            size = Size(canvasSize.width, yAxisMultiplier)
        )

        // Row numbers background
        drawRect(
            color = Color.DarkGray,
            alpha = 1f,
            topLeft = Offset(0f, yAxisMultiplier),
            size = Size(xAxisMultiplier, canvasSize.height)
        )

        // Stop drawing if we have no rows, during song change
        if (fi.numRows == 0) {
            return@Canvas
        }

        // Channel header numbers (only visible ones)
        for (i in visibleChannelRange) {
            val hdrDivision = xAxisMultiplier + (i * 3 * xAxisMultiplier) + (xAxisMultiplier * 1.5f)
            val hdrTxtCenterX = offsetX.value + (hdrDivision - (headerText[i].size.width / 2))
            val hdrTxtCenterY = (yAxisMultiplier / 2) - (headerText[i].size.height / 2)

            drawText(
                textLayoutResult = headerText[i],
                topLeft = Offset(hdrTxtCenterX, hdrTxtCenterY)
            )
        }

        if (PlayerService.isAlive.value && fi.numRows > 0 && modVars.numChannels > 0) {
            for (row in visibleRowRange) {
                val rowData = patternData.getRow(row) ?: continue

                for (chn in visibleChannelRange) {
                    val cacheEntry = textCache[row][chn]

                    val needsUpdate = !cacheEntry.isValid ||
                        cacheEntry.note != rowData.notes[chn] ||
                        cacheEntry.instrument != rowData.instruments[chn] ||
                        cacheEntry.effectType != rowData.fxType[chn] ||
                        cacheEntry.effectParam != rowData.fxParm[chn] ||
                        cacheEntry.isMuted != mutedState[chn]

                    if (needsUpdate) {
                        cacheEntry.note = rowData.notes[chn]
                        cacheEntry.instrument = rowData.instruments[chn]
                        cacheEntry.effectType = rowData.fxType[chn]
                        cacheEntry.effectParam = rowData.fxParm[chn]
                        cacheEntry.isMuted = mutedState[chn]

                        cacheEntry.textLayout = infoTextMeasurer.measure(
                            text = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = if (mutedState[chn]) noteColorMuted else noteColor
                                    )
                                ) {
                                    append(Util.note(rowData.notes[chn].toInt()))
                                }
                                withStyle(
                                    style = SpanStyle(
                                        color = if (mutedState[chn]) instrumentColorMuted else instrumentColor
                                    )
                                ) {
                                    append(Util.num(rowData.instruments[chn].toInt()))
                                }
                                withStyle(
                                    style = SpanStyle(
                                        color = if (mutedState[chn]) effectColorMuted else effectColor
                                    )
                                ) {
                                    val fxt = rowData.fxType[chn]
                                    val fx = if (fxt < 0) {
                                        "-"
                                    } else {
                                        effectsTable.getOrElse(fxt) { "?" }
                                    }
                                    append(fx)
                                }
                                withStyle(
                                    style = SpanStyle(
                                        color = if (mutedState[chn]) effectColorMuted else effectColor
                                    )
                                ) {
                                    append(Util.num(rowData.fxParm[chn].toInt()))
                                }
                            },
                            style = patternTextStyle
                        )

                        cacheEntry.isValid = true
                    }
                }
            }
        }

        // Draw pattern data for visible rows and channels
        for (row in visibleRowRange) {
            for (chn in visibleChannelRange) {
                val cacheEntry = textCache[row][chn]
                val textLayout = cacheEntry.textLayout ?: continue

                val noteRowCenterX = xAxisMultiplier * (chn * 3 + 2)
                val noteCenterX = noteRowCenterX - (textLayout.size.width / 3)
                val noteCenterY = rowYOffset +
                    (row * yAxisMultiplier) +
                    (yAxisMultiplier / 2 - textLayout.size.height / 2)

                // Pattern text
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(noteCenterX + offsetX.value, noteCenterY)
                )
            }

            // Row numbers
            val textCenterX = xAxisMultiplier / 2 - rowText[row].size.width / 2
            val textCenterY = rowYOffset +
                (row * yAxisMultiplier) +
                (yAxisMultiplier / 2 - rowText[row].size.height / 2)

            drawText(
                textLayoutResult = rowText[row],
                color = Color.White,
                topLeft = Offset(textCenterX, textCenterY)
            )
        }

        if (view.isInEditMode) {
            debugScreen(rowTextMeasurer, xAxisMultiplier, yAxisMultiplier)
        }
    }
}

/** Helpers **/
private class PatternTextCacheEntry(
    var textLayout: TextLayoutResult? = null,
    var note: Byte = 0,
    var instrument: Byte = 0,
    var effectType: Byte = 0,
    var effectParam: Byte = 0,
    var isMuted: Boolean = false,
    var isValid: Boolean = false
)

private class PatternViewerDimensions(density: Density) {
    val xAxisMultiplier: Float
    val yAxisMultiplier: Float

    init {
        with(density) {
            xAxisMultiplier = 24.dp.toPx()
            yAxisMultiplier = 24.dp.toPx()
        }
    }
}

/** Preview **/
@Preview
@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun Preview_PatternViewer() {
    val modVars = composeSampleModVars()
    XmpTheme(useDarkTheme = true) {
        ComposePatternViewer(
            onTap = { },
            modType = "FastTracker v2.00 XM 1.04",
            fi = composeSampleFrameInfo(),
            isMuted = ChannelMuteState(
                isMuted = List(modVars.numChannels) {
                    false
                }.toPersistentList()
            ),
            modVars = modVars,
            patternData = PatternDataState(),
            onVisibleRowRangeChanged = { },
        )
    }
}
