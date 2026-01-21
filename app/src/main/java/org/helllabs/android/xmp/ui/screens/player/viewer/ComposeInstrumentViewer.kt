package org.helllabs.android.xmp.ui.screens.player.viewer

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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.ui.screens.player.ChannelMuteState
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.ui.theme.seed

private const val VOLUME_STEPS = 32
private val barShape = CornerRadius(8f, 8f)

// O(1) lookup based on volume
private val textColor = Array(VOLUME_STEPS + 1) { i ->
    val fraction = i.coerceIn(0, VOLUME_STEPS) / VOLUME_STEPS.toFloat()
    lerp(Color.Gray, Color.White, fraction)
}

private val instrumentTextStyle = TextStyle(
    fontSize = 18.sp,
    fontFamily = FontFamily.Monospace,
    platformStyle = PlatformTextStyle(includeFontPadding = true)
)

@Composable
internal fun InstrumentViewer(
    onTap: () -> Unit,
    channelInfo: ChannelInfo,
    insName: ImmutableList<String>,
    isMuted: ChannelMuteState,
    modVars: ModVars
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current

    // Layout dimensions cached per density
    val dimensions = remember(density) {
        InstrumentDimensions(density)
    }

    // Pre-measured text for all instruments - only recalculates on instrument list change
    val measuredText = remember(insName) {
        insName.map {
            textMeasurer.measure(
                text = AnnotatedString(it),
                style = instrumentTextStyle
            )
        }
    }

    // Vertical scroll offset (negative values = scrolled down)
    val yOffset = remember {
        Animatable(0f)
    }

    // Canvas dimensions for scroll bounds calculation
    var canvasSize by remember {
        mutableStateOf(Size.Zero)
    }

    val scrollState = rememberScrollableState { delta ->
        scope.launch {
            // Total height of all instrument rows
            val totalContentHeight = dimensions.rowHeight * modVars.numInstruments
            // Maximum negative offset (how far we can scroll down)
            val maxOffset = (totalContentHeight - canvasSize.height).coerceAtLeast(0f)
            val newOffset = (yOffset.value + delta).coerceIn(-maxOffset, 0f)
            yOffset.snapTo(newOffset)
        }
        delta
    }

    // Reset scroll position when song changes
    LaunchedEffect(modVars.numInstruments, insName) {
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
            .graphicsLayer {
                translationY = yOffset.value
            }
            .scrollable(
                orientation = Orientation.Vertical,
                state = scrollState
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        // Calculate channel box layout (distributed evenly across width)
        val totalPadding = (modVars.numChannels - 1) * dimensions.padding
        val availableWidth = size.width - totalPadding
        val boxWidth = availableWidth / modVars.numChannels

        // Reusable size object for all volume bars
        val barSize = Size(boxWidth, dimensions.rowHeight)

        // Pre-calculate horizontal positions for each channel box
        val boxPositions = FloatArray(modVars.numChannels) { j ->
            j * (boxWidth + dimensions.padding)
        }

        // Calculate visible row range for culling
        val rowHeightInv = 1f / dimensions.rowHeight
        val firstVisibleRow = (-yOffset.value * rowHeightInv).toInt().coerceAtLeast(0)
        val lastVisibleRow = ((-yOffset.value + size.height) * rowHeightInv).toInt()
            .coerceAtMost(measuredText.size - 1)

        // Draw only visible instruments
        for (i in firstVisibleRow..lastVisibleRow) {
            var maxVol = 0 // Track loudest channel for text brightness
            val yPos = dimensions.rowHeight * i // Y position of this instrument row

            // Draw active channel volume boxes for this instrument
            for (j in 0 until modVars.numChannels) {
                if (j >= isMuted.size) continue

                // Skip muted channels or channels not playing this instrument
                if (isMuted.isMuted[j] || i != channelInfo.instruments[j]) {
                    continue
                }

                // Scale volume from 0-64 to 0-32 range
                val vol = (channelInfo.volumes[j] / 2).coerceAtMost(VOLUME_STEPS)

                if (vol > 0) {
                    // Track the loudest channel to determine text brightness
                    if (vol > maxVol) {
                        maxVol = vol
                    }

                    // Alpha calculation
                    val alpha = vol / VOLUME_STEPS.toFloat()

                    drawRoundRect(
                        color = seed,
                        cornerRadius = barShape,
                        alpha = alpha,
                        topLeft = Offset(boxPositions[j], yPos),
                        size = barSize
                    )
                }
            }

            // Draw instrument name with brightness based on max volume
            drawText(
                color = textColor[maxVol],
                textLayoutResult = measuredText[i],
                topLeft = Offset(0f, yPos)
            )
        }

        // Debug overlay
        if (view.isInEditMode) {
            debugScreen(textMeasurer = textMeasurer)
        }
    }
}

/** Helpers **/
private class InstrumentDimensions(density: Density) {
    val rowHeight: Float // Height of each instrument row
    val padding: Float // Horizontal padding between channel boxes

    init {
        with(density) {
            rowHeight = 24.dp.toPx()
            padding = 2.dp.toPx()
        }
    }
}

/** Preview **/
@Preview
@Composable
private fun Preview_InstrumentViewer() {
    val modVars = composeSampleModVars()
    XmpTheme(useDarkTheme = true) {
        InstrumentViewer(
            onTap = {},
            channelInfo = composeSampleChannelInfo(),
            isMuted = ChannelMuteState(
                isMuted = List(modVars.numChannels) {
                    false
                }.toPersistentList()
            ),
            modVars = modVars,
            insName = List(modVars.numInstruments) {
                String.format("%02X %s", it + 1, "Instrument Name")
            }.toPersistentList()
        )
    }
}
