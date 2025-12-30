package org.helllabs.android.xmp.compose.ui.playlist.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * @author https://github.com/m-derakhshan/PermissionHandler
 */

@Stable
data class PermissionModel(val permission: String, val rational: String)

@Stable
data class PermissionState(
    val askPermission: Boolean = true,
    val showRational: Boolean = false,
    val rationals: List<String> = emptyList(),
    val permissions: List<String>,
    val navigateToSetting: Boolean = false
)

@Stable
class PermissionViewModelFactory(private val permissions: List<PermissionModel>) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T = PermissionViewModel(permissions = permissions) as T
}

@Stable
class PermissionViewModel(private val permissions: List<PermissionModel>) : ViewModel() {
    private val _state = MutableStateFlow(
        PermissionState(permissions = permissions.map { it.permission }, askPermission = true)
    )
    val state = _state.asStateFlow()

    fun onResult(result: Map<String, Boolean>) {
        result.forEach {
            Timber.d("Perm: ${it.key} was ${it.value}")
        }

        _state.update { it.copy(askPermission = false) }

        val notGrantedPermissions = permissions.filter {
            it.permission in result.filter { permission -> permission.value.not() }
        }

        if (notGrantedPermissions.isEmpty()) {
            onConsumeRational()
        } else {
            _state.update { state ->
                state.copy(
                    navigateToSetting = true,
                    permissions = notGrantedPermissions.map { it.permission },
                    showRational = notGrantedPermissions.isNotEmpty(),
                    rationals = notGrantedPermissions.map { it.rational }
                )
            }
        }
    }

    fun onPermissionRequested() {
        _state.update {
            it.copy(askPermission = false, navigateToSetting = false)
        }
    }

    private fun onConsumeRational() {
        _state.update {
            it.copy(showRational = false)
        }
    }
}
