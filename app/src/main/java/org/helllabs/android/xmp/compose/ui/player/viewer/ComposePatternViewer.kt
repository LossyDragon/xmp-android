package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.seed
import org.helllabs.android.xmp.compose.ui.player.ChannelMuteState
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

// Maybe keep the row numbers in view at all times, and move the channel columns instead?

// TODO
//  1. I broke preview
//  2. Overscrolling issues
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
    modVars: ModVars
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

    val rowFxParm = remember { ByteArray(64) }
    val rowFxType = remember { ByteArray(64) }
    val rowInsts = remember { ByteArray(64) }
    val rowNotes = remember { ByteArray(64) }

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
        val firstVisible = ((yAxisMultiplier - rowYOffset) / yAxisMultiplier).toInt().coerceAtLeast(
            0
        )
        val lastVisible = ((canvasSize.height - rowYOffset) / yAxisMultiplier).toInt().coerceAtMost(
            fi.numRows - 1
        )
        firstVisible..lastVisible
    }

    val visibleChannelRange = remember(canvasSize, offsetX, modVars.numChannels, xAxisMultiplier) {
        val firstVisible = ((-offsetX.value - xAxisMultiplier) / (3 * xAxisMultiplier)).toInt()
            .coerceAtLeast(0)
        val lastVisible = (
            (canvasSize.width - offsetX.value + xAxisMultiplier) / (3 * xAxisMultiplier)
            ).toInt()
            .coerceAtMost(modVars.numChannels - 1)
            .coerceAtLeast(0)
        firstVisible..lastVisible
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
    LaunchedEffect(modVars.numChannels, canvasSize, fi.pattern) {
        Timber.d("Resetting scroll position")
        scope.launch {
            offsetX.snapTo(0f)
        }
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
                // Be very careful here!
                // Our variables are latency-compensated but pattern data is current
                // so caution is needed to avoid retrieving data using old variables
                // from a module with pattern data from a newly loaded one.
                Xmp.getPatternRow(
                    pat = fi.pattern,
                    row = row,
                    rowNotes = rowNotes,
                    rowInstruments = rowInsts,
                    rowFxType = rowFxType,
                    rowFxParm = rowFxParm
                )

                // Update cache for visible channels
                for (chn in visibleChannelRange) {
                    val cacheEntry = textCache[row][chn]

                    val needsUpdate = !cacheEntry.isValid ||
                        cacheEntry.note != rowNotes[chn] ||
                        cacheEntry.instrument != rowInsts[chn] ||
                        cacheEntry.effectType != rowFxType[chn] ||
                        cacheEntry.effectParam != rowFxParm[chn] ||
                        cacheEntry.isMuted != mutedState[chn]

                    if (needsUpdate) {
                        cacheEntry.note = rowNotes[chn]
                        cacheEntry.instrument = rowInsts[chn]
                        cacheEntry.effectType = rowFxType[chn]
                        cacheEntry.effectParam = rowFxParm[chn]
                        cacheEntry.isMuted = mutedState[chn]

                        cacheEntry.textLayout = infoTextMeasurer.measure(
                            text = buildAnnotatedString {
                                // Notes
                                withStyle(
                                    style = SpanStyle(
                                        color = if (mutedState[chn]) noteColorMuted else noteColor
                                    ),
                                    block = {
                                        append(Util.note(rowNotes[chn].toInt()))
                                    }
                                )
                                // Instruments
                                withStyle(
                                    style = SpanStyle(
                                        color = if (mutedState[chn]) {
                                            instrumentColorMuted
                                        } else {
                                            instrumentColor
                                        }
                                    ),
                                    block = {
                                        append(Util.num(rowInsts[chn].toInt()))
                                    }
                                )
                                // Effects
                                withStyle(
                                    style = SpanStyle(
                                        color = if (mutedState[chn]) {
                                            effectColorMuted
                                        } else {
                                            effectColor
                                        }
                                    ),
                                    block = {
                                        val fxt = rowFxType[chn]
                                        val fx = if (fxt < 0) {
                                            "-"
                                        } else {
                                            effectsTable.getOrElse(fxt) {
                                                Timber.w(
                                                    "Unknown FX: $fxt in chn ${chn + 1}, " +
                                                        "row $row, using $currentType. Type:$modType"
                                                )
                                                "?"
                                            }
                                        }
                                        append(fx)
                                    }
                                )
                                // Effects Params
                                withStyle(
                                    style = SpanStyle(
                                        color = if (mutedState[chn]) {
                                            effectColorMuted
                                        } else {
                                            effectColor
                                        }
                                    ),
                                    block = {
                                        append(Util.num(rowFxParm[chn].toInt()))
                                    }
                                )
                            },
                            style = patternTextStyle.copy(
                                background = if (view.isInEditMode) {
                                    Color.Green
                                } else {
                                    Color.Unspecified
                                }
                            )
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
            isMuted = ChannelMuteState(isMuted = BooleanArray(modVars.numChannels) { false }),
            modVars = modVars,
        )
    }
}
