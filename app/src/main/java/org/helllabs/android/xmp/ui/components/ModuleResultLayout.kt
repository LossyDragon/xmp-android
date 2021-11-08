package org.helllabs.android.xmp.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.*
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.darkAccent
import org.helllabs.android.xmp.ui.theme.darkGray
import org.helllabs.android.xmp.ui.theme.sectionBackground
import org.helllabs.android.xmp.util.upperCase

@Composable
fun ButtonBar(
    modifier: Modifier = Modifier,
    playButtonText: String,
    isLoading: Boolean,
    isSupported: Boolean,
    onPlay: () -> Unit,
    onRandom: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(sectionBackground)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(.85f),
            enabled = !isLoading && isSupported,
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = darkGray,
                containerColor = darkAccent
            ),
            onClick = { onPlay() },
        ) {
            Text(
                color = Color.White,
                text = playButtonText.upperCase()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            modifier = Modifier.fillMaxWidth(.85f),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = darkGray,
                containerColor = darkAccent
            ),
            onClick = { onRandom() },
        ) {
            Text(
                color = Color.White,
                text = stringResource(id = R.string.random).upperCase()
            )
        }
    }
}

@Composable
fun ModuleLayout(
    modifier: Modifier = Modifier,
    moduleResult: ModuleResult?,
) {
    if (moduleResult == null)
        return

    val module = moduleResult.module!!
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var moduleFile by rememberSaveable { mutableStateOf(module.filename) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Scroll to the top on a new module.
        if (module.filename != moduleFile) {
            moduleFile = module.filename
            LaunchedEffect(scrollState) {
                scope.launch {
                    scrollState.scrollTo(0)
                }
            }
        }

        val uriHandler = LocalUriHandler.current
        val size = (module.bytes?.div(1024)) ?: 0
        val info = stringResource(
            R.string.search_result_by,
            module.getFormat(),
            module.getArtist(),
            size
        )

        Spacer(modifier = Modifier.height(10.dp))
        // Title
        Text(text = module.getSongTitle().toString())
        Spacer(modifier = Modifier.height(5.dp))
        // Filename
        Text(text = module.getFilename())
        Spacer(modifier = Modifier.height(10.dp))
        // Info
        val infoLink = annotatedLink(info, module.infopage.orEmpty())
        ClickableText(
            text = infoLink,
            onClick = {
                infoLink
                    .getStringAnnotations("URL", it, it)
                    .firstOrNull()?.let { stringAnnotation ->
                        uriHandler.openUri(stringAnnotation.item)
                    }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        // License
        HeaderText(stringResource(id = R.string.text_license))
        Spacer(modifier = Modifier.height(5.dp))
        // Licence Link
        val licenseLink =
            annotatedLink(module.license!!.getLegalTitle(), module.license!!.getLegalUrl())
        ClickableText(
            text = licenseLink,
            style = TextStyle(fontSize = 16.sp),
            onClick = {
                licenseLink
                    .getStringAnnotations("URL", it, it)
                    .firstOrNull()?.let { stringAnnotation ->
                        uriHandler.openUri(stringAnnotation.item)
                    }
            }
        )
        Spacer(modifier = Modifier.height(5.dp))
        // Licence Statement
        Text(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
            text = module.license?.description ?: "...",
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (!module.comment.isNullOrEmpty()) {
            // Song Message
            HeaderText(stringResource(id = R.string.text_song_message))
            Spacer(modifier = Modifier.height(10.dp))
            // Song Message Content
            MonoSpaceText(text = module.parseComment())
            Spacer(modifier = Modifier.height(10.dp))
        }
        // Instruments
        HeaderText(stringResource(id = R.string.text_instruments))
        Spacer(modifier = Modifier.height(10.dp))
        // Instruments Content
        MonoSpaceText(text = module.parseInstruments())
        Spacer(modifier = Modifier.height(10.dp))
        // Sponsor
        if (moduleResult.hasSponsor()) {
            val sponsor = moduleResult.sponsor!!.details!!
            val sponsorLink = annotatedLink(sponsor.text!!, sponsor.link!!)
            HeaderText(stringResource(id = R.string.text_sponsor))
            Spacer(modifier = Modifier.height(10.dp))
            // Sponsor Content
            ClickableText(
                text = sponsorLink,
                style = TextStyle(fontSize = 16.sp),
                onClick = {
                    sponsorLink
                        .getStringAnnotations("URL", it, it)
                        .firstOrNull()?.let { stringAnnotation ->
                            uriHandler.openUri(stringAnnotation.item)
                        }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun HeaderText(text: String) {
    Text(
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        text = text
    )
}

@Composable
private fun MonoSpaceText(text: String) {
    Text(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        text = text
    )
}

/************
 * Previews *
 ************/

@Preview(uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ModuleLayoutPreview() {
    XmpTheme3 {
        Surface {
            ModuleLayout(modifier = Modifier, moduleResult = fakeModuleResult())
        }
    }
}

@Preview
@Composable
private fun ButtonBarPreview() {
    XmpTheme3 {
        ButtonBar(
            modifier = Modifier,
            playButtonText = "Play",
            onPlay = {},
            onRandom = {},
            isLoading = false,
            isSupported = true,
        )
    }
}
