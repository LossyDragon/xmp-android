package org.helllabs.android.xmp.ui.modarchive.result

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.navigationBarsPadding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.ui.modarchive.SearchError
import org.helllabs.android.xmp.ui.modarchive.result.ModuleResultViewModel.ModuleState
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.toast
import org.helllabs.android.xmp.util.yesNoDialog

@AndroidEntryPoint
class ModuleResult : AppCompatActivity() {

    private val viewModel: ModuleResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val id = intent.getIntExtra(MODULE_ID, -1)

        logD("request module ID $id")
        if (id < 0) viewModel.getRandomModule() else viewModel.getModuleById(id) // Effect?

        logD("onCreate")
        setContent {
            ModuleResultScreen(
                intentId = id,
                onBack = { onBackPressed() },
                viewModel = viewModel,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.attachObserver()
    }

    override fun onPause() {
        super.onPause()
        viewModel.removeObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.removeFetch()
    }
}

@Composable
private fun ModuleResultScreen(
    intentId: Int,
    onBack: () -> Unit,
    viewModel: ModuleResultViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelState = viewModel.moduleState.collectAsState()
    var appTitle by rememberSaveable {
        mutableStateOf(
            if (intentId < 0) R.string.search_random_title
            else R.string.search_module_title
        )
    }

    ModuleResultLayout(
        appTitle = appTitle,
        viewModelState = viewModelState.value,
        onBack = { onBack() },
        onDelete = { module ->
            val file = FileUtils.localFile(module)!!
            context.yesNoDialog(
                lifecycleOwner = lifecycleOwner,
                title = context.getString(R.string.title_delete_file),
                message = context.getString(R.string.msg_delete_file, module.filename),
                onPositiveButton = {
                    context.logD("Delete " + file.path)
                    if (file.delete()) {
                        viewModel.touch()
                    } else {
                        context.toast(R.string.error)
                    }
                    if (PrefManager.useArtistFolder) {
                        val parent = file.parentFile!!
                        val contents = parent.listFiles()
                        if (contents != null && contents.isEmpty()) {
                            try {
                                val path = PrefManager.mediaPath!!
                                val mediaPath = File(path).canonicalPath
                                val parentPath = parent.canonicalPath

                                if (parentPath.startsWith(mediaPath) && parentPath != mediaPath) {
                                    context.logI("Remove empty directory " + parent.path)
                                    if (!parent.delete()) {
                                        context.toast(R.string.msg_error_remove_directory)
                                        context.logE("error removing directory")
                                    }
                                }
                            } catch (e: IOException) {
                                context.logE(e.message.toString())
                            }
                        }
                    }
                    viewModel.touch()
                }
            )
        },
        onError = {
            val message = it ?: context.getString(R.string.search_unknown_error)
            val intent = Intent(context, SearchError::class.java)
            intent.putExtra(ERROR, message)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            context.startActivity(intent)
            (context as Activity).overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        },
        onPlay = { module ->
            if (FileUtils.localFile(module)?.exists() == true) {
                val path = FileUtils.localFile(module)!!.path
                val modList = ArrayList<String>()

                modList.add(path)
                XmpApplication.fileList = modList

                context.logI("Play $path")
                val intent = Intent(context, PlayerActivity::class.java)
                intent.putExtra(PlayerActivity.PARM_START, 0)
                context.startActivity(intent)
                (context as Activity).overridePendingTransition(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            } else {
                // Does not exist, download module
                val modDir = FileUtils.getDownloadPath(module)
                val url = module.url

                context.logI("Downloaded $url to $modDir")
                if (FileUtils.localFile(url!!, modDir).exists()) {
                    context.yesNoDialog(
                        lifecycleOwner = lifecycleOwner,
                        title = context.getString(R.string.msg_file_exists),
                        message = context.getString(R.string.msg_file_exists_overwrite),
                        onPositiveButton = {
                            viewModel.downloadModule(
                                module.filename!!,
                                url, modDir
                            )
                        }
                    )
                } else {
                    viewModel.downloadModule(module.filename!!, url, modDir)
                }
            }
        },
        onRandom = {
            appTitle = R.string.search_random_title
            viewModel.getRandomModule()
        },
    )
}

@Composable
private fun ModuleResultLayout(
    @StringRes appTitle: Int,
    viewModelState: ModuleState,
    onBack: () -> Unit,
    onDelete: (module: Module) -> Unit,
    onError: (error: String?) -> Unit,
    onPlay: (module: Module) -> Unit,
    onRandom: () -> Unit,
) {
    XmpTheme(
        onlyStyleStatusBar = true,
    ) {
        var moduleResult by remember { mutableStateOf<ModuleResult?>(null) }
        var moduleExists by rememberSaveable { mutableStateOf(false) }
        val scaffoldState = rememberScaffoldState()
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(id = appTitle),
                    navIconClick = { onBack() },
                    menuActions = {
                        if (moduleExists) DeleteMenu({ onDelete(moduleResult!!.module!!) })
                    },
                )
            },
            scaffoldState = scaffoldState,
            snackbarHost = { scaffoldState.snackbarHostState },
        ) {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            var isLoading by rememberSaveable { mutableStateOf(false) }
            var buttonText by rememberSaveable { mutableStateOf("") }

            context.logD("State: $viewModelState")
            when (viewModelState) {
                ModuleState.Cancelled -> {
                    val msg = stringResource(id = R.string.msg_download_cancelled)
                    SideEffect {
                        scope.launch {
                            scaffoldState.snackbarHostState.showSnackbar(
                                message = msg,
                                actionLabel = context.getString(R.string.ok)
                            )
                        }
                    }
                    isLoading = false
                }
                ModuleState.Complete -> {
                    isLoading = false
                }
                ModuleState.Load -> {
                    buttonText = stringResource(id = R.string.button_loading)
                    isLoading = true
                }
                ModuleState.None -> {
                }
                ModuleState.Queued -> {
                    isLoading = true
                    buttonText = stringResource(id = R.string.button_downloading)
                }
                is ModuleState.DownloadError -> {
                    SideEffect {
                        scope.launch {
                            scaffoldState.snackbarHostState.showSnackbar(
                                message = viewModelState.downloadError,
                                actionLabel = context.getString(R.string.ok)
                            )
                        }
                    }
                    isLoading = false
                }
                is ModuleState.Error -> {
                    onError(viewModelState.error)
                    isLoading = false
                }
                is ModuleState.SearchResult -> {
                    isLoading = false
                    moduleResult = viewModelState.result
                }
                is ModuleState.SoftError -> {
                    context.logW(viewModelState.softError)
                    ErrorLayout(viewModelState.softError)
                    isLoading = false
                }
            }

            ConstraintLayout(
                modifier = Modifier
                    .fillMaxHeight()
                    .navigationBarsPadding()
            ) {
                val (column, snack, loading, buttons) = createRefs()

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .constrainAs(loading) {
                                width = Dimension.fillToConstraints
                                top.linkTo(parent.top)
                            }
                            .fillMaxWidth()
                    )
                }

                ModuleLayout(
                    modifier = Modifier.constrainAs(column) {
                        height = Dimension.fillToConstraints
                        width = Dimension.fillToConstraints
                        top.linkTo(parent.top)
                        bottom.linkTo(buttons.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                    moduleResult = moduleResult,
                )

                Snackbar(
                    modifier = Modifier.constrainAs(snack) {
                        width = Dimension.fillToConstraints
                        bottom.linkTo(buttons.top)
                    },
                    snackBarState = scaffoldState.snackbarHostState,
                    onDismiss = {
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                    }
                )

                moduleExists = FileUtils.localFile(moduleResult?.module)?.exists() ?: false
                val isUnSupported =
                    listOf(*ModArchiveConstants.UNSUPPORTED).contains(moduleResult?.module?.format)
                if (!isLoading) {
                    context.logD("State: isLoading: $isLoading for Buttons")
                    buttonText =
                        stringResource(id = if (moduleExists) R.string.play else R.string.download)
                    if (isUnSupported)
                        buttonText = stringResource(id = R.string.button_download_unsupported)
                }

                ButtonBar(
                    modifier = Modifier
                        .constrainAs(buttons) {
                            width = Dimension.fillToConstraints
                            top.linkTo(column.bottom)
                            bottom.linkTo(parent.bottom)
                        },
                    playButtonText = buttonText,
                    isLoading = isLoading,
                    isUnsupported = isUnSupported,
                    onPlay = {
                        onPlay(moduleResult!!.module!!)
                    },
                    onRandom = {
                        onRandom()
                    },
                )
            }
        }
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun ModuleResultPreview() {
    val state = ModuleState.SearchResult(fakeModuleResult())
    ModuleResultLayout(
        appTitle = R.string.search_module_title,
        viewModelState = state,
        onBack = { },
        onDelete = {},
        onError = {},
        onPlay = {},
        onRandom = {},
    )
}
