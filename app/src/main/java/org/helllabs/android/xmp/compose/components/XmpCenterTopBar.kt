package org.helllabs.android.xmp.compose.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.seconds
import me.saket.extendedspans.ExtendedSpans
import me.saket.extendedspans.SquigglyUnderlineSpanPainter
import me.saket.extendedspans.drawBehind
import me.saket.extendedspans.rememberSquigglyUnderlineAnimator
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.theme.michromaFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XmpCenterTopBar(
    onSettings: () -> Unit,
    onTitle: () -> Unit,
    isAlive: Boolean = false,
    isPlaying: Boolean = false
) {
    val underlineAnimator = rememberSquigglyUnderlineAnimator(2.seconds)
    val extendedSpans = remember {
        ExtendedSpans(
            SquigglyUnderlineSpanPainter(
                wavelength = 32.sp,
                amplitude = 2.sp,
                bottomOffset = 4.sp,
                animator = underlineAnimator
            )
        )
    }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(),
        actions = {
            IconButton(
                onClick = onSettings,
                content = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                }
            )
        },
        title = {
            TextButton(
                onClick = onTitle,
                content = {
                    ProvideTextStyle(
                        LocalTextStyle.current.merge(
                            TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                    ) {
                        val text = themedText(
                            text = stringResource(id = R.string.app_name),
                            isAlive = isAlive,
                            isPlaying = isPlaying

                        )

                        Text(
                            modifier = Modifier.drawBehind(
                                extendedSpans
                            ),
                            text = remember(text) {
                                extendedSpans.extend(text)
                            },
                            onTextLayout = { result ->
                                extendedSpans.onTextLayout(
                                    result
                                )
                            },
                            fontFamily = michromaFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    )
}
