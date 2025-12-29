package org.helllabs.android.xmp.compose.ui.search.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.persistentSetOf
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.ui.search.screen.SearchType

@Stable
data class SearchSegmentedButton(val type: SearchType, @field:StringRes val string: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButtons(
    modifier: Modifier = Modifier,
    searchType: SearchType,
    onSearchType: (SearchType) -> Unit
) {
    val buttonOptions = persistentSetOf(
        SearchSegmentedButton(SearchType.TITLE, R.string.title_or_filename),
        SearchSegmentedButton(SearchType.ARTIST, R.string.artist)
    )

    //
    //
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        buttonOptions.forEachIndexed { idx, item ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = idx,
                    count = buttonOptions.size
                ),
                onClick = { onSearchType(item.type) },
                selected = searchType == item.type,
                label = { Text(text = stringResource(id = item.string)) }
            )
        }
    }
}
