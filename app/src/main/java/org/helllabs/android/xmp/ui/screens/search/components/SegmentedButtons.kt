package org.helllabs.android.xmp.ui.screens.search.components

import androidx.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import kotlinx.collections.immutable.persistentSetOf
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.screens.search.screen.SearchType

@Stable
data class SearchSegmentedButton(val type: SearchType, @field:StringRes val string: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButtons(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    searchType: SearchType,
    onSearchType: (SearchType) -> Unit
) {
    val buttonOptions = persistentSetOf(
        SearchSegmentedButton(SearchType.TITLE, R.string.title_or_filename),
        SearchSegmentedButton(SearchType.ARTIST, R.string.artist)
    )

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        buttonOptions.forEachIndexed { idx, item ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = idx,
                    count = buttonOptions.size
                ),
                enabled = enabled,
                onClick = { onSearchType(item.type) },
                selected = searchType == item.type,
                label = { Text(text = stringResource(id = item.string)) }
            )
        }
    }
}
