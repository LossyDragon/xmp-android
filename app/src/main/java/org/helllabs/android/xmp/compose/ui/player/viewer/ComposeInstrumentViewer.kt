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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.seed
import org.helllabs.android.xmp.compose.ui.player.ChannelMuteState
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.ModVars

private const val VOLUME_STEPS = 32
private val barShape = CornerRadius(8f, 8f)
private val textColor = buildList {
    for (i in 0..VOLUME_STEPS) {
        val fraction = i.coerceIn(0, VOLUME_STEPS) / VOLUME_STEPS.toFloat()
        add(lerp(Color.Gray, Color.White, fraction))
    }
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

    val dimensions = remember(density) {
        InstrumentDimensions(density)
    }
    val measuredText = remember(modVars.numInstruments, insName) {
        (0 until modVars.numInstruments).map {
            textMeasurer.measure(
                text = AnnotatedString(insName[it]),
                style = instrumentTextStyle
            )
        }
    }
    val yOffset = remember {
        Animatable(0f)
    }
    var canvasSize by remember {
        mutableStateOf(Size.Zero)
    }
    val channelMuteState = remember(isMuted) {
        isMuted.isMuted
    }
    val scrollState = rememberScrollableState { delta ->
        scope.launch {
            val totalContentHeight = dimensions.rowHeight * modVars.numInstruments
            val maxOffset = (totalContentHeight - canvasSize.height).coerceAtLeast(0f)
            val newOffset = (yOffset.value + delta).coerceIn(-maxOffset, 0f)
            yOffset.snapTo(newOffset)
        }
        delta
    }

    LaunchedEffect(modVars.numInstruments, insName) {
        // Scroll to the top on song change
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
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        // Pre-calculate fixed layout values
        val totalPadding = (modVars.numChannels - 1) * dimensions.padding
        val availableWidth = size.width - totalPadding
        val boxWidth = availableWidth / modVars.numChannels

        // Get visible range for culling
        val firstVisibleRow = (-yOffset.value / dimensions.rowHeight).toInt().coerceAtLeast(0)
        val lastVisibleRow = ((-yOffset.value + size.height) / dimensions.rowHeight).toInt()
            .coerceAtMost(modVars.numInstruments - 1)

        // Only draw visible instruments
        for (i in firstVisibleRow..lastVisibleRow) {
            var maxVol = 0
            val yPos = yOffset.value + (dimensions.rowHeight * i)

            // Active channel volume boxes
            for (j in 0 until modVars.numChannels) {
                if (channelMuteState[j] || i != channelInfo.instruments[j]) {
                    continue
                }

                val vol = (channelInfo.volumes[j] / 2).coerceAtMost(VOLUME_STEPS)
                if (vol > 0) {
                    val start = j * (boxWidth + dimensions.padding)

                    if (vol > maxVol) {
                        maxVol = vol
                    }

                    drawRoundRect(
                        color = seed,
                        cornerRadius = barShape,
                        alpha = vol / VOLUME_STEPS.toFloat(),
                        topLeft = Offset(start, yPos),
                        size = Size(boxWidth, dimensions.rowHeight)
                    )
                }
            }

            // Instrument name with appropriate color
            drawText(
                color = textColor[maxVol],
                textLayoutResult = measuredText[i],
                topLeft = Offset(0f, yPos)
            )
        }

        if (view.isInEditMode) {
            debugScreen(textMeasurer = textMeasurer)
        }
    }
}

/** Helpers **/
private class InstrumentDimensions(val density: Density) {
    val rowHeight: Float
    val padding: Float

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
            isMuted = ChannelMuteState(isMuted = BooleanArray(modVars.numChannels) { false }),
            modVars = modVars,
            insName = List(modVars.numInstruments) {
                String.format("%02X %s", it + 1, "Instrument Name")
            }.toPersistentList()
        )
    }
}
