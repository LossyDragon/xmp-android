package org.helllabs.android.xmp.presentation.ui.search.searchResult

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.navigationBarsPadding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.presentation.components.AppBar
import org.helllabs.android.xmp.presentation.components.DeleteMenu
import org.helllabs.android.xmp.presentation.components.DialogSnackbar
import org.helllabs.android.xmp.presentation.components.ErrorLayout
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.sectionBackground
import org.helllabs.android.xmp.presentation.ui.player.PlayerActivity
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.presentation.ui.search.SearchError
import org.helllabs.android.xmp.presentation.ui.search.searchResult.ModuleResultViewModel.ModuleState
import org.helllabs.android.xmp.presentation.utils.annotatedLink
import org.helllabs.android.xmp.util.*

@AndroidEntryPoint
class ModuleResult : ComponentActivity() {

    private val viewModel: ModuleResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val id = intent.getIntExtra(MODULE_ID, -1)
        logD("request module ID $id")
        with(viewModel) {
            if (id < 0) getRandomModule() else getModuleById(id)
        }

        logD("onCreate")
        setContent {
            val viewModelState = viewModel.moduleState.collectAsState()
            var appTitle by rememberSaveable {
                mutableStateOf(
                    if (id < 0) R.string.search_random_title
                    else R.string.search_module_title
                )
            }
            ModuleResultScreen(
                isDarkTheme = isSystemInDarkTheme(),
                appTitle = appTitle,
                onBack = { onBackPressed() },
                onDelete = { deleteClick(it) },
                viewModelState = viewModelState,
                onError = { error ->
                    val message = error ?: getString(R.string.search_unknown_error)
                    Intent(this, SearchError::class.java).apply {
                        putExtra(ERROR, message)
                        flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    }.also {
                        startActivity(it)
                    }
                },
                onPlay = {
                    playClick(it)
                },
                onRandom = {
                    appTitle = R.string.search_random_title
                    viewModel.getRandomModule()
                },
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

    private fun playClick(module: Module) {
        if (FileUtils.localFile(module)?.exists() == true) {
            val path = FileUtils.localFile(module)!!.path
            val modList = ArrayList<String>()

            modList.add(path)
            XmpApplication.fileList = modList

            logI("Play $path")
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra(PlayerActivity.PARM_START, 0)
            startActivity(intent)
        } else {
            // Does not exist, download module
            val modDir = FileUtils.getDownloadPath(module)
            val url = module.url

            logI("Downloaded $url to $modDir")
            download(module.filename!!, url!!, modDir)
        }
    }

    private fun deleteClick(module: Module) {
        val file = FileUtils.localFile(module)!!
        val message = getString(R.string.msg_delete_file, module.filename)
        yesNoDialog(
            this,
            R.string.title_delete_file,
            message,
            onConfirm = {
                logD("Delete " + file.path)
                if (file.delete()) {
                    viewModel.touch()
                } else {
                    toast(R.string.error)
                }
                if (PrefManager.useArtistFolder) {
                    val parent = file.parentFile!!
                    val contents = parent.listFiles()
                    if (contents != null && contents.isEmpty()) {
                        try {
                            val path = PrefManager.mediaPath
                            val mediaPath = File(path).canonicalPath
                            val parentPath = parent.canonicalPath

                            if (parentPath.startsWith(mediaPath) && parentPath != mediaPath) {
                                logI("Remove empty directory " + parent.path)
                                if (!parent.delete()) {
                                    toast(R.string.msg_error_remove_directory)
                                    logE("error removing directory")
                                }
                            }
                        } catch (e: IOException) {
                            logE(e.message.toString())
                        }
                    }
                }
                viewModel.touch()
            }
        )
    }

    private fun download(mod: String, url: String, path: String) {
        if (FileUtils.localFile(url, path).exists()) {
            val message = getString(R.string.msg_file_exists_overwrite)
            yesNoDialog(
                this,
                R.string.msg_file_exists,
                message,
                onConfirm = { viewModel.downloadModule(mod, url, path) }
            )
        } else {
            viewModel.downloadModule(mod, url, path)
        }
    }
}

@Composable
private fun ModuleResultScreen(
    isDarkTheme: Boolean,
    @StringRes appTitle: Int,
    onBack: () -> Unit,
    onDelete: (module: Module) -> Unit,
    viewModelState: State<ModuleState>,
    onError: (error: String?) -> Unit,
    onPlay: (module: Module) -> Unit,
    onRandom: () -> Unit,
) {
    AppTheme(
        isDarkTheme = isDarkTheme,
        onlyStyleStatusBar = true,
    ) {
        var module by remember { mutableStateOf<Module?>(null) }
        var moduleExists by rememberSaveable { mutableStateOf(false) }
        val scaffoldState = rememberScaffoldState()
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(id = appTitle),
                    navIconClick = { onBack() },
                    menuActions = { if (moduleExists) DeleteMenu { onDelete(module!!) } },
                )
            },
            scaffoldState = scaffoldState,
            snackbarHost = { scaffoldState.snackbarHostState },
        ) {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            var isLoading by rememberSaveable { mutableStateOf(false) }
            var buttonText by rememberSaveable { mutableStateOf("") }

            context.logD("State: ${viewModelState.value}")
            when (val state = viewModelState.value) {
                ModuleState.Cancelled -> {
                    val msg = stringResource(id = R.string.msg_download_cancelled)
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = msg,
                            actionLabel = context.getString(R.string.ok)
                        )
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
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = state.downloadError,
                            actionLabel = context.getString(R.string.ok)
                        )
                    }
                    isLoading = false
                }
                is ModuleState.Error -> {
                    onError(state.error)
                    isLoading = false
                }
                is ModuleState.SearchResult -> {
                    isLoading = false
                    module = state.result.module!!
                }
                is ModuleState.SoftError -> {
                    context.logW(state.softError)
                    ErrorLayout(state.softError)
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
                    module = module,
                )

                DialogSnackbar(
                    modifier = Modifier.constrainAs(snack) {
                        width = Dimension.fillToConstraints
                        bottom.linkTo(buttons.top)
                    },
                    snackBarState = scaffoldState.snackbarHostState,
                    onDismiss = {
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                    }
                )

                moduleExists = FileUtils.localFile(module)?.exists() ?: false
                val isUnSupported =
                    listOf(*ModArchiveConstants.UNSUPPORTED).contains(module?.format)
                if (!isLoading) {
                    context.logD("State: isLoading: $isLoading for Buttons")
                    buttonText =
                        if (moduleExists)
                            stringResource(id = R.string.play)
                        else
                            stringResource(id = R.string.download)
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
                        onPlay(module!!)
                    },
                    onRandom = {
                        onRandom()
                    },
                )
            }
        }
    }
}

@Composable
private fun ButtonBar(
    modifier: Modifier,
    playButtonText: String,
    isLoading: Boolean,
    isUnsupported: Boolean,
    onPlay: () -> Unit,
    onRandom: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(sectionBackground)
            .padding(12.dp)
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onPlay() },
            enabled = !isLoading && !isUnsupported,
        ) {
            Text(
                color = Color.White,
                text = playButtonText.upperCase()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onRandom() },
            enabled = !isLoading,
        ) {
            Text(
                color = Color.White,
                text = stringResource(id = R.string.random).upperCase()
            )
        }
    }
}

@Composable
private fun ModuleLayout(
    modifier: Modifier,
    module: Module?,
) {
    if (module == null)
        return

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var moduleFile by rememberSaveable { mutableStateOf(module.filename) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Scroll to the top on a new module.
        if (module.filename != moduleFile) {
            moduleFile = module.filename
            LaunchedEffect(scrollState) {
                scope.launch {
                    scrollState.scrollTo(0)
                }
            }
        }

        val uriHandler = LocalUriHandler.current
        val size = (module.bytes?.div(1024)) ?: 0
        val info =
            stringResource(R.string.search_result_by, module.getFormat(), module.getArtist(), size)

        Spacer(modifier = Modifier.height(10.dp))
        // Title
        Text(text = module.getSongTitle())
        Spacer(modifier = Modifier.height(5.dp))
        // Filename
        Text(text = module.getFilename())
        Spacer(modifier = Modifier.height(10.dp))
        // Info
        val infoLink = annotatedLink(info, module.infopage.orEmpty())
        ClickableText(
            text = infoLink,
            onClick = {
                infoLink
                    .getStringAnnotations("URL", it, it)
                    .firstOrNull()?.let { stringAnnotation ->
                        uriHandler.openUri(stringAnnotation.item)
                    }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        // License
        HeaderText(stringResource(id = R.string.text_license))
        Spacer(modifier = Modifier.height(5.dp))
        // Licence Link
        val licenseLink =
            annotatedLink(module.getLicence().getLegalTitle(), module.getLicence().getLegalUrl())
        ClickableText(
            text = licenseLink,
            style = TextStyle(fontSize = 16.sp),
            onClick = {
                licenseLink
                    .getStringAnnotations("URL", it, it)
                    .firstOrNull()?.let { stringAnnotation ->
                        uriHandler.openUri(stringAnnotation.item)
                    }
            }
        )
        Spacer(modifier = Modifier.height(5.dp))
        // Licence Statement
        Text(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
            text = module.license?.description ?: "...",
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (!module.comment.isNullOrEmpty()) {
            // Song Message
            HeaderText(stringResource(id = R.string.text_song_message))
            Spacer(modifier = Modifier.height(10.dp))
            // Song Message Content
            MonoSpaceText(text = module.parseComment())
            Spacer(modifier = Modifier.height(10.dp))
        }
        // Instruments
        HeaderText(stringResource(id = R.string.text_instruments))
        Spacer(modifier = Modifier.height(10.dp))
        // Instruments Content
        MonoSpaceText(text = module.parseInstruments())
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun HeaderText(text: String) {
    Text(
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        text = text
    )
}

@Composable
private fun MonoSpaceText(text: String) {
    Text(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        text = text
    )
}

@Preview
@Composable
private fun ModuleLayoutPreview() {
    AppTheme(true) {
        ModuleLayout(modifier = Modifier, module = Module())
    }
}

@Preview
@Composable
private fun ButtonBarPreview() {
    AppTheme(true) {
        ButtonBar(
            modifier = Modifier,
            playButtonText = "Play",
            onPlay = {},
            onRandom = {},
            isLoading = false,
            isUnsupported = false,
        )
    }
}
