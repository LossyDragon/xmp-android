package org.helllabs.android.xmp.compose.ui.search.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.annotatedLinkStringCombined
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.License
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.Sponsor
import org.helllabs.android.xmp.model.SponsorDetails

@Composable
fun ModuleLayout(
    modifier: Modifier = Modifier,
    moduleResult: ModuleResult?,
    expandTextColor: Color = MaterialTheme.colorScheme.primary
) {
    if (moduleResult == null) {
        return
    }

    val module = moduleResult.module
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var moduleFile by rememberSaveable { mutableStateOf(module.filename) }

    var textLayoutResultState by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    val licenseDescription by remember { mutableStateOf(module.license.description) }
    var licenseText by remember { mutableStateOf(AnnotatedString(licenseDescription)) }
    LaunchedEffect(textLayoutResultState) {
        when {
            isExpanded -> {
                licenseText = buildAnnotatedString {
                    append(licenseDescription)
                    withStyle(
                        style = SpanStyle(
                            color = expandTextColor,
                            fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(" Show Less")
                    }
                }
            }

            !isExpanded && textLayoutResultState!!.hasVisualOverflow -> {
                val lastCharIndex = textLayoutResultState!!.getLineEnd(1, true)
                val showMoreString = "Show More"
                val adjustedText = module.license.description
                    .take(lastCharIndex)
                    .dropLast(showMoreString.length)
                    .dropLastWhile { it == ' ' || it == '.' }

                licenseText = buildAnnotatedString {
                    append("$adjustedText... ")
                    withStyle(
                        style = SpanStyle(
                            color = expandTextColor,
                            fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(showMoreString)
                    }
                }
            }
        }
    }

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

        val size = (module.bytes.div(1024))
        val info = stringResource(
            R.string.result_by,
            module.format,
            module.artist,
            size
        )

        Spacer(modifier = Modifier.height(10.dp))
        // Title
        Text(text = module.songtitle)
        Spacer(modifier = Modifier.height(5.dp))
        // Filename
        Text(text = module.filename, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(10.dp))
        // Info
        Text(
            text = annotatedLinkStringCombined(info, module.infopage),
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        // License
        HeaderText(stringResource(id = R.string.license))
        Spacer(modifier = Modifier.height(5.dp))
        // Licence Link
        Text(
            text = annotatedLinkStringCombined(module.license.title, module.license.legalurl),
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(5.dp))
        // Licence Statement
        Text(
            modifier = modifier
                .padding(start = 10.dp, end = 10.dp)
                .clickable { isExpanded = !isExpanded }
                .animateContentSize(),
            text = licenseText,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            onTextLayout = { textLayoutResultState = it }
        )

        Spacer(modifier = Modifier.height(10.dp))
        if (module.comment.isNotEmpty()) {
            // Song Message
            HeaderText(stringResource(id = R.string.song_message))
            Spacer(modifier = Modifier.height(10.dp))
            // Song Message Content
            MonoSpaceText(text = module.formattedComment)
            Spacer(modifier = Modifier.height(10.dp))
        }
        // Instruments
        HeaderText(stringResource(id = R.string.instruments))
        Spacer(modifier = Modifier.height(10.dp))
        // Instruments Content
        MonoSpaceText(text = module.formattedInstruments)
        Spacer(modifier = Modifier.height(10.dp))
        // Sponsor
        if (moduleResult.hasSponsor) {
            val sponsor = moduleResult.sponsor.details
            HeaderText(stringResource(id = R.string.sponsor))
            Spacer(modifier = Modifier.height(10.dp))
            // Sponsor Content
            Text(
                text = annotatedLinkStringCombined(sponsor.text, sponsor.link),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
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

@Preview
@Composable
private fun Preview_ModuleLayout() {
    val result = ModuleResult(
        sponsor = Sponsor(
            details = SponsorDetails(
                link = "http://localhost",
                text = "Some Sponsor Text"
            )
        ),
        module = Module(
            filename = "",
            bytes = 669669,
            format = "XM",
            artistInfo = ArtistInfo(artist = persistentListOf(Artist(alias = "Some Artist"))),
            infopage = "http://localhost",
            license = License(
                title = "Some License Title",
                legalurl = "http://localhost",
                description = "Some License Description"
            ),
            comment = "Some Comment",
            instruments = buildAnnotatedString {
                repeat(12) {
                    append("Some Instrument $it\n")
                }
            }.toString()
        )
    )
    XmpTheme(useDarkTheme = true) {
        Surface {
            ModuleLayout(modifier = Modifier.fillMaxWidth(), moduleResult = result)
        }
    }
}
