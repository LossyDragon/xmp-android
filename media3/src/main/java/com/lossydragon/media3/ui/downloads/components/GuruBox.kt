package com.lossydragon.media3.ui.downloads.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lossydragon.media3.ui.theme.XmpTheme
import com.lossydragon.media3.ui.theme.topazFontFamily
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun GuruBox(
    modifier: Modifier = Modifier,
    message: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var frameState by remember { mutableStateOf(true) }

    val guruCode by retain(message) {
        val random = if (message.isNotEmpty()) {
            Random(message.hashCode().toLong())
        } else {
            Random.Default
        }

        val x = random.nextInt(16)
        val y = random.nextLong(0x100000000L)
        mutableStateOf(String.format("0000000%X.%08X", x, y))
    }

    LaunchedEffect(frameState) {
        // Guru Meditation Frame
        scope.launch {
            delay(1337L)
            frameState = !frameState
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(5.dp, if (frameState) Color.Red else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.padding(12.dp),
                text = "$message\n\nGuru Meditation #$guruCode",
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                fontFamily = topazFontFamily,
                fontSize = 16.sp,
                color = Color.Red
            )
        }

        TextButton(
            onClick = onBack,
            content = {
                Text(
                    color = Color.Red,
                    fontFamily = topazFontFamily,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp,
                    text = "Go Back",
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        GuruBox(message = "Guru Box", onBack = {})
    }
}
