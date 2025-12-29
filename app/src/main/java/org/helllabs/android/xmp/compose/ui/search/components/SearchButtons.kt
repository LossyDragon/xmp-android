package org.helllabs.android.xmp.compose.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.R

@Composable
fun SearchButtons(
    searchText: String,
    onSearch: (String) -> Unit,
    onRandom: () -> Unit
) {
    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            modifier = Modifier.Companion
                .weight(.75f),
            enabled = searchText.isNotEmpty(),
            onClick = { onSearch(searchText) }
        ) {
            Text(text = stringResource(id = R.string.search))
        }
        Spacer(modifier = Modifier.Companion.width(16.dp))
        OutlinedButton(
            modifier = Modifier.Companion
                .weight(.75f),
            onClick = onRandom
        ) {
            Text(text = stringResource(id = R.string.random))
        }
    }
}
