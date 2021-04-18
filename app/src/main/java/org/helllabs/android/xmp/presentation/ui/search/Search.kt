package org.helllabs.android.xmp.presentation.ui.search

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics.DENSITY_HIGH
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.navigationBarsWithImePadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.presentation.components.AppBar
import org.helllabs.android.xmp.presentation.components.RadioGroup
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.systemDarkTheme
import org.helllabs.android.xmp.presentation.theme.transparent
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.presentation.ui.search.searchResult.ArtistResult
import org.helllabs.android.xmp.presentation.ui.search.searchResult.ModuleResult
import org.helllabs.android.xmp.presentation.ui.search.searchResult.SearchListResult
import org.helllabs.android.xmp.presentation.utils.annotatedLinkString
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.upperCase

class Search : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        logD("onCreate")
        setContent {
            SearchLayout(
                appTitle = stringResource(id = R.string.search_title),
                onBack = { onBackPressed() },
                onSearch = { selection, search ->
                    var intent: Intent? = null
                    when (selection) {
                        0 -> intent = Intent(this, SearchListResult::class.java)
                            .putExtra(SEARCH_TEXT, search.trim { it <= ' ' })
                        1 -> intent = Intent(this, ArtistResult::class.java)
                            .putExtra(SEARCH_TEXT, search.trim { it <= ' ' })
                    }
                    startActivity(intent)
                },
                onRandom = {
                    Intent(this, ModuleResult::class.java).apply {
                        putExtra(MODULE_ID, -1)
                    }.also {
                        startActivity(it)
                    }
                },
                onHistory = {
                    Intent(this, SearchHistory::class.java).also {
                        startActivity(it)
                    }
                }
            )
        }
    }
}

// This is a bit cluttered.
@Composable
fun SearchLayout(
    isDarkTheme: Boolean = systemDarkTheme(),
    appTitle: String,
    onBack: () -> Unit,
    onSearch: (selection: Int, search: String) -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit,
) {
    AppTheme(
        isDarkTheme = isDarkTheme
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    title = appTitle,
                    navIconClick = { onBack() },
                )
            }
        ) {
            val radioGroup = listOf(
                stringResource(id = R.string.search_title_or_filename),
                stringResource(id = R.string.search_artist)
            )
            var search by rememberSaveable { mutableStateOf("") }
            var selection by rememberSaveable { mutableStateOf(0) }
            val smallDpi = LocalContext.current.resources.displayMetrics.densityDpi <= DENSITY_HIGH
            val focusManager = LocalFocusManager.current
            val isSearchValid = search.length >= 3

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .navigationBarsWithImePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // More error fields to be added:
                // See: https://stackoverflow.com/q/65642533/13225929
                // https://issuetracker.google.com/issues/182142737
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    value = search,
                    onValueChange = { search = it },
                    isError = !isSearchValid,
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (isSearchValid) {
                                onSearch(selection, search)
                            }
                            focusManager.clearFocus()
                        }
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    maxLines = 1,
                    label = { Text(stringResource(id = R.string.hint_search_box)) },
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp),
                    text = stringResource(id = R.string.search_helper_text),
                    fontSize = 10.sp
                )
                RadioGroup(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    radioList = radioGroup,
                    selectedOption = selection,
                    onSelected = { selection = it }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                ) {
                    Button(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(.5f)
                            .padding(end = 16.dp),
                        enabled = isSearchValid,
                        onClick = { onSearch(selection, search) }
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!smallDpi) {
                                Icon(
                                    modifier = Modifier.padding(end = 8.dp),
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = stringResource(id = R.string.search).upperCase(),
                                color = Color.White
                            )
                        }
                    }
                    Button(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxWidth(.5f)
                            .padding(start = 16.dp),
                        onClick = { onRandom() }
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!smallDpi) {
                                Icon(
                                    modifier = Modifier.padding(end = 8.dp),
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = stringResource(id = R.string.random).upperCase(),
                                color = Color.White
                            )
                        }
                    }
                }
                Button(
                    modifier = Modifier.padding(top = 32.dp, start = 32.dp, end = 32.dp),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    colors = ButtonDefaults.buttonColors(backgroundColor = transparent),
                    onClick = { onHistory() }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier.padding(end = 8.dp),
                            imageVector = Icons.Default.History,
                            contentDescription = null
                        )
                        Text(text = stringResource(id = R.string.search_history), maxLines = 1)
                    }
                }
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val uriHandler = LocalUriHandler.current
                    val linkString = annotatedLinkString(
                        stringResource(id = R.string.search_download_provided),
                        "modarchive.org"
                    )
                    ClickableText(
                        text = linkString,
                        style = TextStyle(color = MaterialTheme.colors.onBackground),
                        onClick = {
                            linkString
                                .getStringAnnotations("URL", it, it)
                                .firstOrNull()?.let { stringAnnotation ->
                                    uriHandler.openUri(stringAnnotation.item)
                                }
                        }
                    )
                }
            }
        }
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
fun SearchLayoutPreview() {
    SearchLayout(
        isDarkTheme = false,
        appTitle = stringResource(id = R.string.search_title),
        onBack = {},
        onSearch = { _, _ -> },
        onRandom = {},
        onHistory = {},
    )
}

@Preview
@Composable
fun SearchLayoutPreviewDark() {
    SearchLayout(
        isDarkTheme = true,
        appTitle = stringResource(id = R.string.search_title),
        onBack = {},
        onSearch = { _, _ -> },
        onRandom = {},
        onHistory = {},
    )
}
