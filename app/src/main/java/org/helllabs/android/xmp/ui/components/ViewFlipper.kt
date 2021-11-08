package org.helllabs.android.xmp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.ui.theme.darkAccent
import org.helllabs.android.xmp.ui.theme.michromaFontFamily

// A shitty attempt for a ViewFlipper... You can see the text change before transition :/
// https://developer.android.com/jetpack/compose/animation#animatedcontent
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ViewFlipper(
    modifier: Modifier = Modifier,
    count: Int,
    modTitle: String,
    format: String,
) {
    AnimatedContent(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { it } + fadeIn() with
                    slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() with
                    slideOutHorizontally { it } + fadeOut()
            }.using(SizeTransform(clip = false))
        }
    ) {
        ViewFlipperItem(modTitle, format)
    }
}

// Font Padding: https://issuetracker.google.com/issues/171394808
@Composable
private fun ViewFlipperItem(
    modTitle: String,
    format: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.height(28.dp),
            color = Color.White,
            fontFamily = michromaFontFamily,
            fontSize = 20.sp, // 22.sp
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = modTitle,
            style = TextStyle(baselineShift = BaselineShift(1f)),
        )
        Text(
            modifier = Modifier.height(28.dp),
            color = Color.LightGray,
            fontFamily = michromaFontFamily,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = format,
            style = TextStyle(baselineShift = BaselineShift(-0.2f)),
        )
    }
}

@Preview
@Composable
private fun ViewFlipperItemPreview() {
    ViewFlipperItem("Some Text", "Some Format")
}

// To be ran on a device or emulator to test
@Preview
@Composable
private fun ViewFlipperPreview() {
    var count by remember { mutableStateOf(0) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ViewFlipper(
            modifier = Modifier.background(darkAccent),
            count = count,
            modTitle = "Name: $count",
            format = "Format: $count"
        )
        Spacer(Modifier.size(20.dp))
        Row(horizontalArrangement = Arrangement.SpaceAround) {
            Button(onClick = { count-- }) { Text("Previous") }
            Spacer(Modifier.size(60.dp))
            Button(onClick = { count++ }) { Text("Forward") }
        }
    }
}
