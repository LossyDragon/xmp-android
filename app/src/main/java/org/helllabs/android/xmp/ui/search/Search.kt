package org.helllabs.android.xmp.ui.search

import android.content.res.Configuration
import android.util.DisplayMetrics.DENSITY_HIGH
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.insets.imePadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.NavScreensSearch
import org.helllabs.android.xmp.ui.components.RadioGroup
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.ui.components.annotatedLinkString
import org.helllabs.android.xmp.ui.components.waterfallPadding
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.darkAccent
import org.helllabs.android.xmp.ui.theme.darkGray
import org.helllabs.android.xmp.util.upperCase

@Composable
fun SearchScreen(
    navController: NavController,
    innerPadding: PaddingValues,
) {
    SearchLayout(
        innerPadding = innerPadding,
        onBack = { navController.popBackStack() },
        onSearch = { selection, search ->
            when (selection) {
                0 -> {
                    // Search Result
                    val navArgs = "?querySearch=$search"
                    navController.navigate(NavScreensSearch.ListResult.route + navArgs)
                }
                1 -> {
                    // Artist Result
                    val navArgs = "?artistQuery=$search"
                    navController.navigate(NavScreensSearch.ArtistResult.route + navArgs)
                }
            }
        },
        onRandom = {
            navController.navigate(
                NavScreensSearch.ModuleResult.route + "?moduleId=${-1}"
            )
        },
        onHistory = { navController.navigate(NavScreensSearch.History.route) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchLayout(
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onSearch: (selection: Int, search: String) -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    val search = remember { mutableStateOf(TextFieldValue("")) }
    val selection = remember { mutableStateOf(0) }
    val isSearchValid = search.value.text.length >= 3

    val onSearchClick = { onSearch(selection.value, search.value.text.trim()) }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            XmpAppBar3(
                scrollBehavior = scrollBehavior,
                onNavIconPressed = onBack,
                titleText = stringResource(id = R.string.search_title)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(it)
                .verticalScroll(scrollState)
                .waterfallPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SearchBox(search, isSearchValid, onSearchClick)
            Spacer(modifier = Modifier.height(16.dp))
            SearchRadioSelection(selection)
            Spacer(modifier = Modifier.height(16.dp))
            SearchButtons(isSearchValid, onSearchClick, onRandom, onHistory)
            Spacer(modifier = Modifier.height(16.dp))
            SearchProvidedBy()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SearchBox(
    search: MutableState<TextFieldValue>,
    isSearchValid: Boolean,
    onSearch: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .fillMaxWidth(),
        value = search.value,
        onValueChange = { value -> search.value = value },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = MaterialTheme.colorScheme.onBackground,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
        ),
        isError = !isSearchValid,
        keyboardActions = KeyboardActions(
            onSearch = {
                if (isSearchValid) {
                    onSearch()
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
    Row {
        AnimatedVisibility(
            visible = !isSearchValid,
            enter = fadeIn(initialAlpha = 0.4f),
            exit = fadeOut(animationSpec = tween(durationMillis = 250))
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                text = stringResource(id = R.string.search_helper_text),
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SearchRadioSelection(selection: MutableState<Int>) {
    val radioGroup = listOf(
        stringResource(id = R.string.search_title_or_filename),
        stringResource(id = R.string.search_artist)
    )

    RadioGroup(
        modifier = Modifier.fillMaxWidth(),
        radioList = radioGroup,
        selectedOption = selection.value,
        onSelected = { selection.value = it }
    )
}

@Composable
private fun SearchButtons(
    isSearchValid: Boolean,
    onSearch: () -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SearchButtonContent(
            buttonIcon = Icons.Default.Search,
            buttonText = stringResource(id = R.string.search),
            isEnabled = isSearchValid,
            onClick = onSearch
        )
        SearchButtonContent(
            buttonIcon = Icons.Default.HelpOutline,
            buttonText = stringResource(id = R.string.random),
            isEnabled = true,
            onClick = onRandom
        )
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth(.85f)
                .padding(8.dp),
            onClick = onHistory
        ) {
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    imageVector = Icons.Default.History,
                    tint = MaterialTheme.colorScheme.outline,
                    contentDescription = null
                )
                Text(
                    text = stringResource(id = R.string.search_history),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun SearchButtonContent(
    buttonIcon: ImageVector,
    buttonText: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val smallDpi = context.resources.displayMetrics.densityDpi <= DENSITY_HIGH

    Button(
        modifier = Modifier
            .fillMaxWidth(.85f)
            .padding(8.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = darkGray,
            containerColor = darkAccent
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!smallDpi) {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    imageVector = buttonIcon,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Text(
                text = buttonText.upperCase(),
                color = Color.White
            )
        }
    }
}

@Composable
private fun SearchProvidedBy() {
    val uriHandler = LocalUriHandler.current
    val linkString = annotatedLinkString(
        stringResource(id = R.string.search_download_provided),
        "modarchive.org"
    )

    Row(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClickableText(
            text = linkString,
            style = TextStyle(color = MaterialTheme.colorScheme.onBackground),
            onClick = {
                linkString
                    .getStringAnnotations("URL", it, it)
                    .firstOrNull()
                    ?.let { stringAnnotation -> uriHandler.openUri(stringAnnotation.item) }
            }
        )
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Light Theme", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SearchLayoutPreview() {
    XmpTheme3 {
        SearchLayout(
            innerPadding = PaddingValues(),
            onBack = {},
            onSearch = { _, _ -> },
            onRandom = {},
            onHistory = {},
        )
    }
}
