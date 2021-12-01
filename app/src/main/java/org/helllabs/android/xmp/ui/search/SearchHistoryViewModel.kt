package org.helllabs.android.xmp.ui.search

import androidx.lifecycle.ViewModel
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.util.PrefManager

@HiltViewModel
class SearchHistoryViewModel
@Inject constructor(
    private var moshiAdapter: JsonAdapter<List<Module>>
) : ViewModel() {

    val historyList: List<Module>
        get() = PrefManager.searchHistory?.let {
            moshiAdapter.fromJson(it)
        }.orEmpty()
}
