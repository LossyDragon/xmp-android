package org.helllabs.android.xmp.compose.ui.search.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.annotatedLinkString

@Composable
fun DownloadsText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = annotatedLinkString(
            text = stringResource(id = R.string.search_provided_by),
            url = "modarchive.org"
        ),
        style = TextStyle(color = MaterialTheme.colorScheme.onBackground)
    )
}
