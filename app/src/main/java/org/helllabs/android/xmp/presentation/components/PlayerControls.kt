package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.presentation.theme.*

@Composable
fun PlayerInfo(
    speed: String = "000",
    bpm: String = "000",
    pos: String = "000",
    pat: String = "000",
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Speed
        Row {
            SingleLineText(text = stringResource(id = R.string.info_speed, speed))
        }
        Spacer(modifier = Modifier.width(5.dp))
        // BPM
        Row {
            SingleLineText(text = stringResource(id = R.string.info_bpm, bpm))
        }
        Spacer(modifier = Modifier.width(5.dp))
        // Pos
        Row {
            SingleLineText(text = stringResource(id = R.string.info_position, pos))
        }
        Spacer(modifier = Modifier.width(5.dp))
        // Pat
        Row {
            SingleLineText(text = stringResource(id = R.string.info_pattern, pat))
        }
    }
}

@Composable
fun PlayerTimeBar(
    currentTime: String,
    totalTime: String,
    position: Float,
    range: Float,
    onSeek: (value: Float) -> Unit,
) {
    // What a shit show to intercept touching
    var sliderPosition by remember { mutableStateOf(0f) }
    var sliderRange by remember { mutableStateOf(0f..range) }
    var wasPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.wrapContentWidth(Alignment.Start),
            text = currentTime,
            color = lightGray,
            fontWeight = FontWeight.Bold,
        )

        if (isPressed || isDragged) wasPressed = true

        if (!wasPressed && sliderPosition != position) sliderPosition = position

        if (sliderRange != 0f..range) sliderRange = 0f..range

        Slider(
            modifier = Modifier
                .fillMaxWidth(.8f)
                .height(20.dp),
            interactionSource = interactionSource,
            valueRange = sliderRange,
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
            },
            colors = SliderDefaults.colors(
                thumbColor = darkAccent,
                activeTrackColor = lightGray
            )
        )

        if (!isPressed && !isDragged && wasPressed) {
            onSeek(sliderPosition)
            wasPressed = false
        }

        Text(
            modifier = Modifier.wrapContentWidth(Alignment.End),
            text = totalTime,
            color = lightGray,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun PlayerButtons(
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    isPlaying: Boolean,
    isRepeating: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onStop() }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.Stop,
                contentDescription = stringResource(id = R.string.notif_stop),
                tint = Color.White
            )
        }
        IconButton(onClick = { onPrev() }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = stringResource(id = R.string.notif_prev),
                tint = Color.White
            )
        }
        // TODO: Animate Play Pause
        Surface(
            modifier = Modifier.size(50.dp),
            color = darkAccent,
            shape = CircleShape,
            elevation = 4.dp,
        ) {
            val paused = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
            IconButton(onClick = { onPlay() }) {
                Icon(
                    modifier = Modifier.scale(1.2f),
                    imageVector = paused,
                    contentDescription = stringResource(id = R.string.notif_play),
                    tint = Color.White
                )
            }
        }
        IconButton(onClick = { onNext() }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(id = R.string.notif_next),
                tint = Color.White
            )
        }
        IconButton(onClick = { onRepeat() }) {
            val repeat = if (isRepeating) Icons.Default.RepeatOneOn else Icons.Default.RepeatOn
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = repeat,
                contentDescription = stringResource(id = R.string.button_play_all),
                tint = if (isRepeating) Color.Green else Color.White
            )
        }
    }
}

@Composable
fun DetailsSheet(
    onMessage: () -> Unit,
    moduleInfo: List<Int>,
    playAllSeq: Boolean,
    onAllSeq: (bool: Boolean) -> Unit,
    sequences: List<Int>,
    currentSequence: Int,
    onSequence: (int: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gray)
            .padding(4.dp)
    ) {
        ModuleSection(
            text = stringResource(id = R.string.sheet_details)
        ) {
            IconButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .wrapContentWidth(Alignment.End),
                onClick = { onMessage() }
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(id = R.string.sheet_details),
                    tint = lightGray
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        ModuleInsDetails(stringResource(id = R.string.sheet_patterns), moduleInfo[0])
        ModuleInsDetails(stringResource(id = R.string.sheet_instruments), moduleInfo[1])
        ModuleInsDetails(stringResource(id = R.string.sheet_samples), moduleInfo[2])
        ModuleInsDetails(stringResource(id = R.string.sheet_channels), moduleInfo[3])
        Spacer(modifier = Modifier.height(10.dp))
        ModuleSection(
            text = stringResource(id = R.string.sheet_button_allseqs)
        ) {
            Switch(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .wrapContentWidth(Alignment.End),
                checked = playAllSeq,
                onCheckedChange = {
                    onAllSeq(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = darkAccent,
                    uncheckedThumbColor = lightGray
                ),
            )
        }

        val list = sequences.mapIndexed { index, item ->
            val main = stringResource(R.string.sheet_main_song)
            val sub = stringResource(R.string.sheet_sub_song, index)
            val text = if (index == 0) main else sub

            String.format("%2d:%02d (%s)", item / 60000, item / 1000 % 60, text)
        }
        LazyColumnWithTopScroll(
            modifier = Modifier.fillMaxSize(),
            fabModifier = Modifier.size(64.dp),
            showScrollAt = 0,
            shouldPadBottom = false,
            additionalBottomPad = 0.dp,
            boxContent = { }
        ) {
            itemsIndexed(items = list) { index, _ ->
                RadioButtonItem(
                    item = list[index],
                    index = index,
                    selectedOption = currentSequence,
                    onSelected = { onSequence(it) },
                    overrideTheme = true
                )
            }
        }
    }
}

/***********
 * Helpers
 ***********/

@Composable
fun SingleLineText(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        maxLines = 1,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = lightGray,
    )
}

@Composable
fun ModuleSection(
    text: String,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(AppShapes.small)
            .background(sectionBackground),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .wrapContentWidth(Alignment.Start),
            text = text,
            color = Color.White
        )

        content()
    }
}

@Composable
fun ModuleInsDetails(
    string: String,
    number: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .wrapContentWidth(Alignment.Start),
            text = string,
            fontSize = 14.sp,
            color = lightGray
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .wrapContentWidth(Alignment.End),
            text = number.toString(),
            fontSize = 14.sp,
            color = lightGray
        )
    }
}

/***********
 * Previews
 ***********/

@Preview
@Composable
fun PlayerInfoPreview() {
    AppTheme(false) {
        PlayerInfo()
    }
}

@Preview
@Composable
fun PlayerTimeBarPreview() {
    AppTheme(false) {
        PlayerTimeBar(
            currentTime = "00:00",
            totalTime = "00:00",
            position = 6f,
            range = 100f,
            onSeek = {}
        )
    }
}

@Preview
@Composable
fun PlayerButtonsPreview() {
    AppTheme(false) {
        PlayerButtons(
            onStop = {},
            onPrev = {},
            onPlay = {},
            onNext = {},
            onRepeat = {},
            isPlaying = false,
            isRepeating = true
        )
    }
}

@Preview
@Composable
fun DetailsSheetPreview() {
    val moduleDetails = List(50) { Random.nextInt(1, 1000) }
    val sequences = List(50) { Random.nextInt(1000, 100000) }
    AppTheme(false) {
        DetailsSheet(
            onMessage = {},
            moduleInfo = moduleDetails,
            playAllSeq = true,
            onAllSeq = {},
            sequences = sequences,
            currentSequence = 1,
            onSequence = {}
        )
    }
}
