package org.helllabs.android.xmp.presentation.ui.search.searchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Reason
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.presentation.ui.search.SearchHistory
import org.helllabs.android.xmp.repository.Repository
import org.helllabs.android.xmp.util.logE

@HiltViewModel
class ModuleResultViewModel
@Inject constructor(
    private val repository: Repository,
    private val fetchDownloader: Fetch,
    private val moshiAdapter: JsonAdapter<List<Module>>
) : ViewModel() {

    private val _moduleState = MutableStateFlow<ModuleState>(ModuleState.None)
    val moduleState: StateFlow<ModuleState> = _moduleState

    private var request: Request? = null

    private val fetchObserver = object : FetchObserver<Download> {
        override fun onChanged(data: Download, reason: Reason) {
            if (request!!.id == data.id) {
                when (data.status) {
                    Status.CANCELLED -> _moduleState.value = ModuleState.Cancelled
                    Status.QUEUED -> _moduleState.value = ModuleState.Queued
                    Status.COMPLETED -> _moduleState.value = ModuleState.Complete
                    else -> Unit // Don't care about the rest
                }

                if (data.error != Error.NONE) {
                    val error = data.error.toString() + " " + reason
                    _moduleState.value = ModuleState.DownloadError(error)
                }
            }
        }
    }

    fun attachObserver() {
        if (request != null)
            fetchDownloader.attachFetchObserversForDownload(request!!.id, fetchObserver)
    }

    fun removeObserver() {
        if (request != null)
            fetchDownloader.removeFetchObserversForDownload(request!!.id, fetchObserver)
    }

    fun removeFetch() {
        fetchDownloader.close()
    }

    fun touch() {
        _moduleState.value = ModuleState.None
    }

    fun downloadModule(mod: String, url: String, file: String) {
        val pathFile = File(file)
        pathFile.mkdirs()

        request = Request(url, "$pathFile/$mod")

        fetchDownloader.attachFetchObserversForDownload(request!!.id, fetchObserver)
            .enqueue(
                request!!,
                { updatedRequests -> request = updatedRequests },
                { error ->
                    logE("enqueue: $error")
                    _moduleState.value = ModuleState.Error(error.toString())
                }
            )
    }

    fun getModuleById(id: Int) =
        viewModelScope.launch {
            _moduleState.value = ModuleState.Load
            _moduleState.value = try {
                val result = repository.getModuleById(id)
                result.error?.let {
                    ModuleState.SoftError(it)
                }
                saveModuleToHistory(result.module!!)
                ModuleState.SearchResult(result)
            } catch (e: Exception) {
                ModuleState.Error(e.localizedMessage)
            }
        }

    fun getRandomModule() =
        viewModelScope.launch {
            _moduleState.value = ModuleState.Load
            _moduleState.value = try {
                val result = repository.getRandomModule()
                if (!result.error.isNullOrBlank()) {
                    ModuleState.SoftError(result.error!!)
                } else {
                    saveModuleToHistory(result.module!!)
                    ModuleState.SearchResult(result)
                }
            } catch (e: Exception) {
                ModuleState.Error(e.localizedMessage)
            }
        }

    private fun saveModuleToHistory(module: Module) {
        // Load history list first
        val searchHistory = getSearchHistory().toMutableList()

        // Check to see if the module has been searched before. Skip if true
        searchHistory.forEach {
            if (it.id == module.id)
                return
        }

        // Remove the oldest item if history length is reached
        if (searchHistory.size >= SearchHistory.HISTORY_LENGTH)
            searchHistory.removeFirst()

        // Add the current module into the history
        searchHistory.add(module)

        // Convert into JSON and save it
        PrefManager.searchHistory = moshiAdapter.toJson(searchHistory)
    }

    private fun getSearchHistory(): List<Module> {
        return PrefManager.searchHistory?.let {
            moshiAdapter.fromJson(it)
        }.orEmpty()
    }

    sealed class ModuleState {
        object Cancelled : ModuleState()
        object Complete : ModuleState()
        object Load : ModuleState()
        object None : ModuleState()
        object Queued : ModuleState()
        class DownloadError(val downloadError: String) : ModuleState()
        class Error(val error: String?) : ModuleState()
        class SearchResult(var result: ModuleResult) : ModuleState()
        class SoftError(var softError: String) : ModuleState()
    }
}
