package org.helllabs.android.xmp.ui.search

import androidx.lifecycle.ViewModel
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.util.PrefManager
import org.helllabs.android.xmp.util.logW

@HiltViewModel
class SearchHistoryViewModel
@Inject constructor(
    private var moshiAdapter: JsonAdapter<List<Module>>
) : ViewModel() {

    val historyList: List<Module>
        get() = PrefManager.searchHistory?.let {
            try {
                moshiAdapter.fromJson(it)
            } catch (e: JsonDataException) {
                // Something terrible happened, wipe the data to prevent a crash.
                logW("Wiping search history because it has an error\n ${e.stackTraceToString()}")
                PrefManager.clearSearchHistory()
                listOf()
            }
        }.orEmpty()
}
