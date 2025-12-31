package org.helllabs.android.xmp.compose.ui.search.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.topazFontFamily

@Composable
fun GuruTextButton(
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        content = {
            Text(
                color = Color.Red,
                fontFamily = topazFontFamily,
                fontSize = 16.sp,
                letterSpacing = 1.sp,
                text = text,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline,
            )
        }
    )
}

@Composable
fun GuruFrame(
    modifier: Modifier = Modifier,
    message: String,
    action: (@Composable () -> Unit)? = null
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
                text = message + stringResource(R.string.guru_meditation, guruCode),
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                fontFamily = topazFontFamily,
                fontSize = 16.sp,
                color = Color.Red
            )
        }

        action?.invoke()
    }
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        GuruFrame(
            message = stringResource(R.string.search_unknown_error),
            action = {
                TextButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    content = { GuruTextButton(text = "Go Back", onClick = { }) }
                )
            }
        )
    }
}
