package org.helllabs.android.xmp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.util.PrefManager
import org.helllabs.android.xmp.util.logW
import javax.inject.Inject

@HiltViewModel
class SearchHistoryViewModel
@Inject constructor(
    private var moshiAdapter: JsonAdapter<List<Module>>
) : ViewModel() {

    var historyList: List<Module> = listOf()

    init {
        viewModelScope.launch {
            val history = PrefManager.getPreference(PrefManager.searchHistoryRequest)
            historyList = kotlin.runCatching {
                moshiAdapter.fromJson(history)
            }.getOrElse {
                // Something terrible happened, wipe the data to prevent a crash.
                logW("An error occurred getting history.\n ${it.stackTraceToString()}")
                clearHistory()
                listOf()
            }.orEmpty()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            PrefManager.dataStoreManager.editPreference(
                key = PrefManager.searchHistoryRequest.key,
                newValue = ""
            )
        }
    }
}
