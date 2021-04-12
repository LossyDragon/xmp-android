package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.presentation.theme.AppTheme

@Composable
fun ErrorLayout(
    message: String? = stringResource(id = R.string.search_error)
) {
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.frowny_face)
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            text = message!!
        )
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
fun ErrorLayoutPreview() {
    AppTheme(false) {
        ErrorLayout("Some very long error message that should wrap around")
    }
}
