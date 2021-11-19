package org.helllabs.android.xmp.ui.search.result

import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.collectLatest
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.ui.search.SearchError
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.sectionBackgroundDark
import org.helllabs.android.xmp.util.*

@AndroidEntryPoint
class ModuleResult : AppCompatActivity() {

    private val viewModel: ModuleResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val id = intent.getIntExtra(MODULE_ID, -1)

        logD("request module ID $id")
        val event = if (id < 0) ModuleEvent.RandomModule else ModuleEvent.Module(id)
        viewModel.onEvent(event)

        logD("onCreate")
        setContent {
            val uiController = rememberSystemUiController()
            SideEffect {
                uiController.setNavigationBarColor(color = sectionBackgroundDark)
            }

            ProvideWindowInsets {
                ModuleResultScreen(
                    intentId = id,
                    onBack = { onBackPressed() },
                )
            }
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
    viewModel: ModuleResultViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = viewModel.state
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var appTitle by rememberSaveable {
        mutableStateOf(
            if (intentId < 0) R.string.search_random_title
            else R.string.search_module_title
        )
    }

    LaunchedEffect(true) {
        viewModel.uiState.collectLatest { event ->
            when (event) {
                is ModuleResultViewModel.ModuleUiState.Error -> {
                    val intent = Intent(context, SearchError::class.java)
                    intent.putExtra(ERROR, event.error)
                    intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    context.launchActivity(intent)
                }
                is ModuleResultViewModel.ModuleUiState.Loading ->
                    isLoading = event.isLoading
                is ModuleResultViewModel.ModuleUiState.Random ->
                    appTitle = R.string.search_random_title
            }
        }
    }

    ModuleResultLayout(
        appTitle = appTitle,
        viewModelState = state.value,
        isLoading = isLoading,
        onBack = { onBack() },
        onDelete = { module ->
            val file = FileUtils.localFile(module)!!
            context.yesNoDialog(
                lifecycleOwner = lifecycleOwner,
                title = context.getString(R.string.title_delete_file),
                message = context.getString(R.string.msg_delete_file, module.filename),
                onPositiveButton = {

                    context.logD("Delete " + file.path)
                    if (!file.delete()) {
                        context.toast(R.string.error)
                    } else {
                        if (PrefManager.useArtistFolder) {
                            val parent = file.parentFile!!
                            val contents = parent.listFiles()
                            if (contents != null && contents.isEmpty()) {
                                try {
                                    val path = PrefManager.mediaPath!!
                                    val mediaPath = File(path).canonicalPath
                                    val parentPath = parent.canonicalPath

                                    if (parentPath.startsWith(mediaPath) &&
                                        parentPath != mediaPath
                                    ) {
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
                    }
                }
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
                context.launchActivity(intent)
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
                            val mod = module.filename!!
                            viewModel.onEvent(ModuleEvent.DownloadModule(mod, url, modDir))
                        }
                    )
                } else {
                    val mod = module.filename!!
                    viewModel.onEvent(ModuleEvent.DownloadModule(mod, url, modDir))
                }
            }
        },
        onRandom = { viewModel.onEvent(ModuleEvent.RandomModule) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleResultLayout(
    @StringRes appTitle: Int,
    viewModelState: ModuleState,
    isLoading: Boolean,
    onBack: () -> Unit,
    onDelete: (module: Module) -> Unit,
    onPlay: (module: Module) -> Unit,
    onRandom: () -> Unit,
) {
    XmpTheme3 {
        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
        val scaffoldState = rememberScaffoldState()

        Scaffold(
            topBar = {
                XmpAppBar3(
                    scrollBehavior = scrollBehavior,
                    onNavIconPressed = onBack,
                    titleText = stringResource(id = appTitle),
                    actions = {
                        if (viewModelState.moduleExists)
                            DeleteMenu({ onDelete(viewModelState.module!!.module!!) })
                    }
                )
            },
            bottomBar = {
                val buttonText = when {
                    isLoading ->
                        stringResource(id = R.string.button_loading)
                    viewModelState.moduleExists ->
                        stringResource(id = R.string.play)
                    !viewModelState.moduleSupported ->
                        stringResource(id = R.string.button_download_unsupported)
                    else ->
                        stringResource(id = R.string.download)
                }

                ButtonBar(
                    modifier = Modifier.navigationBarsPadding(),
                    playButtonText = buttonText,
                    isLoading = isLoading,
                    isSupported = viewModelState.moduleSupported,
                    onPlay = { onPlay(viewModelState.module!!.module!!) },
                    onRandom = { onRandom() },
                )
            },
            scaffoldState = scaffoldState,
        ) { contentPadding ->
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(.5f)
                ) {
                    ProgressbarIndicator(isLoading)

                    if (!isLoading) {
                        ModuleLayout(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                            moduleResult = viewModelState.module,
                        )
                    }
                }
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
    val state = ModuleState(module = fakeModuleResult())
    ModuleResultLayout(
        appTitle = R.string.search_module_title,
        viewModelState = state,
        isLoading = false,
        onBack = {},
        onDelete = {},
        onPlay = {},
        onRandom = {},
    )
}
