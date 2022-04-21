package org.helllabs.android.xmp.ui.search.result

import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import kotlinx.coroutines.flow.collectLatest
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.ui.NavScreens
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.components.waterfallPadding
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.sectionBackgroundDark
import org.helllabs.android.xmp.util.*

@Composable
fun ModuleResultScreen(
    navController: NavController,
    moduleId: Int,
    viewModel: ModuleResultViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.state
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var appTitle by rememberSaveable {
        mutableStateOf(
            if (moduleId < 0) R.string.search_random_title
            else R.string.search_module_title
        )
    }

    DisposableEffect(viewModel) {
        viewModel.attachObserver()
        onDispose {
            viewModel.removeObserver()
            viewModel.removeFetch()
        }
    }

    val uiController = rememberSystemUiController()
    SideEffect {
        uiController.setNavigationBarColor(color = sectionBackgroundDark)
    }

    LaunchedEffect(true) {
        viewModel.onEvent(
            if (moduleId < 0) ModuleEvent.RandomModule else ModuleEvent.Module(moduleId)
        )

        viewModel.uiState.collectLatest { event ->
            when (event) {
                is ModuleUiState.Error -> {
                    navController.navigate(
                        NavScreens.SearchError.route + "?errorMsg=${event.error}"
                    )
                }
                is ModuleUiState.Loading ->
                    isLoading = event.isLoading
                is ModuleUiState.Random ->
                    appTitle = R.string.search_random_title
            }
        }
    }

    val fileExistsState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = fileExistsState,
        title = R.string.msg_file_exists,
        message = R.string.msg_file_exists_overwrite,
        positiveButtonText = R.string.yes,
        negativeButtonText = R.string.cancel,
        onPositiveButton = { viewModel.onEvent(ModuleEvent.ExistingModule) },
        onNegativeButton = {},
        onDismiss = { fileExistsState.hide() }
    )

    val moduleDeleteState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = moduleDeleteState,
        title = R.string.title_delete_file,
        messageText = stringResource(
            id = R.string.msg_delete_file,
            state.value.module?.module?.filename.ifNullOrEmpty { "..." }
        ),
        positiveButtonText = R.string.yes,
        negativeButtonText = R.string.cancel,
        onPositiveButton = { viewModel.onEvent(ModuleEvent.DeleteModule) },
        onNegativeButton = {},
        onDismiss = { moduleDeleteState.hide() }
    )

    ModuleResultLayout(
        appTitle = appTitle,
        state = state.value,
        isLoading = isLoading,
        onBack = { navController.popBackStack() },
        onDelete = { moduleDeleteState.show() },
        onPlay = { module ->
            if (Files.localFile(module)!!.exists()) {
                val path = Files.localFile(module)!!.path
                val modList = ArrayList<String>()

                modList.add(path)
                XmpApplication.fileList = modList

                context.logI("Play $path")
                val intent = Intent(context, PlayerActivity::class.java)
                intent.putExtra(PlayerActivity.PARM_START, 0)
                context.launchActivity(intent)
            } else {
                // Does not exist, download module
                val modDir = Files.getDownloadPath(module)
                val url = module.url

                context.logI("Downloaded $url to $modDir")
                if (Files.localFile(url, modDir).exists()) {
                    fileExistsState.show()
                } else {
                    val mod = module.filename
                    viewModel.onEvent(ModuleEvent.Download(mod, url, modDir))
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
    state: ModuleState,
    isLoading: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onPlay: (module: Module) -> Unit,
    onRandom: () -> Unit,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    Scaffold(
        topBar = {
            XmpAppBar3(
                scrollBehavior = scrollBehavior,
                onNavIconPressed = onBack,
                titleText = stringResource(id = appTitle),
                actions = {
                    if (state.moduleExists)
                        DeleteMenu { onDelete() }
                }
            )
        },
        bottomBar = {
            val buttonText = when {
                isLoading -> stringResource(id = R.string.button_loading)
                state.moduleExists -> stringResource(id = R.string.play)
                !state.moduleSupported ->
                    stringResource(id = R.string.button_download_unsupported)
                else -> stringResource(id = R.string.download)
            }

            ButtonBar(
                modifier = Modifier.navigationBarsPadding(),
                playButtonText = buttonText,
                isLoading = isLoading,
                isSupported = state.moduleSupported,
                onPlay = { onPlay(state.module!!.module) },
                onRandom = { onRandom() },
            )
        },
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
                    .weight(.5f),
                contentAlignment = Alignment.Center,
            ) {
                ModuleLayout(
                    modifier = Modifier.waterfallPadding(),
                    moduleResult = state.module,
                )

                state.softError?.let {
                    ErrorLayout(message = it)
                }

                ProgressbarIndicator(isLoading)
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
    val state = ModuleState(module = fakeModuleResult(), softError = "Test Error")
    XmpTheme3 {
        ModuleResultLayout(
            appTitle = R.string.search_module_title,
            state = state,
            isLoading = true,
            onBack = {},
            onDelete = {},
            onPlay = {},
            onRandom = {},
        )
    }
}
