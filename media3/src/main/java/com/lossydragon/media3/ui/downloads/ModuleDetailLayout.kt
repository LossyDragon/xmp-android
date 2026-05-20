package com.lossydragon.media3.ui.downloads

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.lossydragon.media3.model.Artist
import com.lossydragon.media3.model.ArtistInfo
import com.lossydragon.media3.model.License
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.model.ModuleResult
import com.lossydragon.media3.model.Sponsor
import com.lossydragon.media3.model.SponsorDetails
import com.lossydragon.media3.ui.downloads.components.HeaderText
import com.lossydragon.media3.ui.downloads.components.MonoSpaceText
import com.lossydragon.media3.ui.theme.XmpTheme
import com.lossydragon.media3.ui.util.annotatedLinkString
import com.lossydragon.media3.util.formatSize
import kotlinx.coroutines.launch

@Composable
internal fun ModuleDetailLayout(
    modifier: Modifier = Modifier,
    moduleResult: ModuleResult
) {
    val expandTextColor = MaterialTheme.colorScheme.primary

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
                        ),
                        block = { append(" Show Less") }
                    )
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

        val size = module.bytes.toLong().formatSize()
        val info = "${module.format} by ${module.artist} ($size)"

        Spacer(modifier = Modifier.height(10.dp))
        // Title
        Text(text = module.songtitle)
        Spacer(modifier = Modifier.height(5.dp))
        // Filename
        Text(text = module.filename, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(10.dp))
        // Info
        Text(
            text = annotatedLinkString(info, module.infopage),
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        // License
        HeaderText(text = "License")
        Spacer(modifier = Modifier.height(5.dp))
        // Licence Link
        Text(
            text = annotatedLinkString(module.license.title, module.license.legalurl),
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
        // Comment
        if (module.comment.isNotEmpty()) {
            // Song Message
            HeaderText(text = "Song Message")
            Spacer(modifier = Modifier.height(10.dp))
            // Song Message Content
            MonoSpaceText(text = module.formattedComment)
            Spacer(modifier = Modifier.height(10.dp))
        }
        // Instruments
        HeaderText(text = "Instruments")
        Spacer(modifier = Modifier.height(10.dp))
        // Instruments Content
        MonoSpaceText(text = module.formattedInstruments)
        Spacer(modifier = Modifier.height(10.dp))
        // Sponsor
        if (moduleResult.hasSponsor) {
            val sponsor = moduleResult.sponsor.details
            HeaderText(text = "Sponsor")
            Spacer(modifier = Modifier.height(10.dp))
            // Sponsor Content
            Text(
                text = annotatedLinkString(sponsor.text, sponsor.link),
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

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        Surface {
            ModuleDetailLayout(
                moduleResult = ModuleResult(
                    sponsor = Sponsor(
                        details = SponsorDetails(
                            text = "Some Sponsor Text"
                        )
                    ),
                    module = Module(
                        filename = "tomorrow_by_kh.mod",
                        bytes = 669669,
                        format = "XM",
                        artistInfo = ArtistInfo(artist = listOf(Artist(alias = "Some Artist"))),
                        infopage = "",
                        license = License(
                            title = "Some License Title",
                            legalurl = "",
                            description = "Some License Description"
                        ),
                        comment = "Some Comment",
                        instruments = buildAnnotatedString {
                            repeat(20) {
                                append("Some Instrument $it\n")
                            }
                        }.toString()
                    )
                )
            )
        }
    }
}
