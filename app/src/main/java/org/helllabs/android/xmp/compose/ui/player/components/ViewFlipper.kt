package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily

private const val ANIMATION_DURATION = 500

// I guess this acts like a ViewFlipper now
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewFlipper(
    navigationIcon: (@Composable () -> Unit) = { },
    actions: (@Composable RowScope.() -> Unit)? = null,
    skipToPrevious: Boolean,
    info: Pair<String, String>
) {
    val slideInFromLeft = remember {
        slideInHorizontally(
            animationSpec = tween(ANIMATION_DURATION)
        ) { width -> -width } + fadeIn()
    }
    val slideOutToRight = remember {
        slideOutHorizontally(
            animationSpec = tween(ANIMATION_DURATION)
        ) { width -> width } + fadeOut()
    }
    val slideInFromRight = remember {
        slideInHorizontally(
            animationSpec = tween(ANIMATION_DURATION)
        ) { width -> width } + fadeIn()
    }
    val slideOutToLeft = remember {
        slideOutHorizontally(
            animationSpec = tween(ANIMATION_DURATION)
        ) { width -> -width } + fadeOut()
    }
    val transitionSpec = if (skipToPrevious) {
        slideInFromLeft.togetherWith(slideOutToRight)
    } else {
        slideInFromRight.togetherWith(slideOutToLeft)
    }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = navigationIcon,
        actions = {
            if (actions != null) {
                actions()
            } else {
                Spacer(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                )
            }
        },
        title = {
            AnimatedContent(
                targetState = info,
                transitionSpec = { transitionSpec.using(SizeTransform(clip = false)) },
                label = "XMP ViewFlipper",
                content = {
                    ViewFlipperItem(
                        infoTitle = it.first,
                        infoType = it.second
                    )
                }
            )
        }
    )
}

@Composable
private fun ViewFlipperItem(
    infoTitle: String,
    infoType: String
) {
    val textStyle = remember {
        TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ProvideTextStyle(LocalTextStyle.current.merge(textStyle)) {
            Text(
                fontFamily = michromaFontFamily,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = infoTitle
            )
            Text(
                fontFamily = michromaFontFamily,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = infoType
            )
        }
    }
}

/**********
 * Preview
 **********/

@Preview
@Composable
private fun Preview_ViewFlipperItem() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            ViewFlipper(
                actions = {},
                navigationIcon = {},
                skipToPrevious = false,
                info = Pair(
                    "Some Super Very Long Name",
                    "Some Super Duper Very Long Type"
                )
            )
        }
    }
}

@Preview
@Composable
private fun Preview_ViewFlipperItem_2() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            ViewFlipper(
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                    }
                },
                skipToPrevious = false,
                info = Pair(
                    "Some Super Very Long Name",
                    "Some Super Duper Very Long Type"
                )
            )
        }
    }
}

// To be ran on a device or emulator to test
@Preview
@Composable
private fun Preview_Demo_ViewFlipper() {
    XmpTheme(useDarkTheme = true) {
        var infoName by remember { mutableIntStateOf(0) }
        var infoType by remember { mutableIntStateOf(0) }
        var skipToPrevious by remember { mutableStateOf(false) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ViewFlipper(
                actions = {
                    IconButton(onClick = { }) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                    }
                },
                skipToPrevious = skipToPrevious,
                info = Pair(
                    "Some Super Very Long Name: $infoName",
                    "Some Super Duper Very Long Type: $infoType"
                )
            )
            Spacer(Modifier.size(20.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround) {
                Button(
                    onClick = {
                        infoName -= 1
                        infoType -= 1
                        skipToPrevious = true
                    },
                    content = {
                        Text("Previous")
                    }
                )
                Spacer(Modifier.size(60.dp))
                Button(
                    onClick = {
                        infoName += 1
                        infoType += 1
                        skipToPrevious = false
                    },
                    content = {
                        Text("Forward")
                    }
                )
            }
        }
    }
}
