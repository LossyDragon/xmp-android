package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.persistentListOf
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.model.SequenceVars

@Suppress("unused")
internal fun DrawScope.debugPatternViewColumns(
    xValue: Float = 24.dp.toPx()
) {
    if (!BuildConfig.DEBUG) {
        throw Exception("This DrawScope shouldn't be used in non debug builds.")
    }
    for (i in 0 until (size.width / xValue).toInt()) {
        val xPosition = (i * 3 + 1) * 22.dp.toPx()
        drawRect(
            color = (if (i % 2 == 0) Color.Magenta else Color.Cyan).copy(alpha = .05f),
            topLeft = Offset(xPosition, 0f),
            size = Size(66.dp.toPx(), size.height)
        )
    }
}

internal fun DrawScope.debugScreen(
    textMeasurer: TextMeasurer,
    xValue: Float = 24.dp.toPx(),
    yValue: Float = 24.dp.toPx()
) {
    if (!BuildConfig.DEBUG) {
        throw Exception("This DrawScope shouldn't be used in non debug builds.")
    }
    for (i in 0 until (size.width / xValue).toInt()) {
        val xPosition = i * xValue
        drawRect(
            color = Color.Green.copy(alpha = .12f),
            topLeft = Offset(xPosition, 0f),
            size = Size(1f, size.height)
        )
        val text = textMeasurer.measure(
            text = AnnotatedString(i.toString()),
            style = TextStyle(
                color = Color.White,
                fontSize = 5.sp,
                fontFamily = FontFamily.Monospace
            )

        )
        drawText(textLayoutResult = text, topLeft = Offset(xPosition, 0f))
    }
    for (i in 0 until (size.height / yValue).toInt()) {
        val yPosition = i * yValue
        val text = textMeasurer.measure(
            text = AnnotatedString(i.toString()),
            style = TextStyle(
                color = Color.White,
                fontSize = 5.sp,
                fontFamily = FontFamily.Monospace
            )

        )
        drawText(textLayoutResult = text, topLeft = Offset(0f, yPosition))
        drawRect(
            color = Color.Yellow.copy(alpha = .12f),
            topLeft = Offset(0f, yPosition),
            size = Size(size.width, 1f)
        )
    }
}

@Composable
internal fun composeSampleChannelInfo(): ChannelInfo {
    if (!BuildConfig.DEBUG) {
        throw Exception("This function shouldn't be used in non debug builds.")
    }
    return ChannelInfo(
        volumes = intArrayOf(
            64, 33, 44, 64, 33, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
        ),
        finalVols = intArrayOf(
            64, 32, 44, 64, 32, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
        ),
        pans = intArrayOf(
            128, 256, 128, 128, 128, 128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        instruments = intArrayOf(
            2, 2, 6, 8, 8, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0
        ),
        keys = intArrayOf(
            60, 72, 60, 60, 79, 71, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
        ),
        periods = intArrayOf(
            5670, 2835, 2733, 2572, 858, 3543, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        holdVols = intArrayOf(
            21, 33, 31, 5, 33, 23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
        ),
    )
}

@Composable
internal fun composeSampleModVars(): ModVars {
    if (!BuildConfig.DEBUG) {
        throw Exception("This function shouldn't be used in non debug builds.")
    }
    return ModVars(
        seqDuration = 158677,
        lengthInPatterns = 42,
        numPatterns = 37,
        numChannels = 6,
        numInstruments = 18,
        numSamples = 18,
        numSequence = 1,
        currentSequence = 0
    )
}

@Composable
internal fun composeSampleFrameInfo(): FrameInfo {
    if (!BuildConfig.DEBUG) {
        throw Exception("This function shouldn't be used in non debug builds.")
    }
    return FrameInfo(pos = 11, pattern = 5, row = 10, numRows = 64, frame = 0, speed = 3, bpm = 121)
}

@Composable
internal fun composeSampleSeqVars(): SequenceVars {
    if (!BuildConfig.DEBUG) {
        throw Exception("This function shouldn't be used in non debug builds.")
    }
    return SequenceVars(sequence = persistentListOf(1111, 2222, 3333, 4444, 5555, 6666))
}

internal val composePreviewRowFxParm = intArrayOf(
    -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0
)
internal val composePreviewRowFxType = intArrayOf(
    -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0
)
internal val composePreviewRowInsts = intArrayOf(
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0
)
internal val composePreviewRowNotes = intArrayOf(
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0
)
