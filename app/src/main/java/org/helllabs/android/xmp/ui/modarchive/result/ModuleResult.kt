package org.helllabs.android.xmp.ui.modarchive.result

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.showProgress
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.collect
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.databinding.ActivityResultModuleBinding
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.ui.modarchive.SearchError
import org.helllabs.android.xmp.ui.modarchive.result.ModuleResultViewModel.ModuleState
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.util.dialogMessage
import org.helllabs.android.xmp.ui.util.toast
import org.helllabs.android.xmp.ui.util.yesNoDialog
import org.helllabs.android.xmp.util.*

@AndroidEntryPoint
class ModuleResult : AppCompatActivity() {

    private lateinit var binder: ActivityResultModuleBinding

    private val viewModel: ModuleResultViewModel by viewModels()

    lateinit var module: Module
    private var shouldPlay = false
    private var deleteMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivityResultModuleBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        with(binder) {
            // Toolbar Text
            appbar.toolbarText.text = getString(R.string.search_module_title)
            // Play|Download Button
            moduleButtonPlay.apply {
                attachTextChangeAnimator()
                bindProgressButton(this)
                click { playClick() }
            }
            // Random Button
            moduleButtonRandom.click {
                viewModel.getRandomModule()
                binder.appbar.toolbarText.text = getString(R.string.search_random_title)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.moduleState.collect { state ->
                when (state) {
                    ModuleState.None -> Unit
                    ModuleState.Load -> onLoad()
                    ModuleState.Cancelled -> onCancelled()
                    ModuleState.Queued -> onQueued()
                    ModuleState.Complete -> onComplete()
                    is ModuleState.DownloadError -> onDownLoadError(state.downloadError)
                    is ModuleState.Error -> onError(state.error)
                    is ModuleState.SoftError -> onSoftError(state.softError)
                    is ModuleState.SearchResult -> onResult(state.result)
                }
            }
        }

        val id = intent.getIntExtra(MODULE_ID, -1)
        logD("request module ID $id")
        if (id < 0) {
            viewModel.getRandomModule()
            binder.appbar.toolbarText.text = getString(R.string.search_random_title)
        } else {
            viewModel.getModuleById(id)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        deleteMenu = menu

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            deleteClick()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun onLoad() {
        binder.resultFrame.hide()
        binder.resultSpinner.show()
        binder.moduleButtonPlay.isEnabled = false
        binder.moduleButtonPlay.text = getString(R.string.button_loading)
    }

    private fun onCancelled() {
        toast(R.string.msg_download_cancelled)
        updateButtons()
        binder.moduleButtonRandom.isEnabled = true
    }

    private fun onQueued() {
        logI("Download Queued")
        binder.moduleButtonRandom.isEnabled = false
    }

    private fun onComplete() {
        logI("Download Complete")
        updateButtons()
        binder.moduleButtonRandom.isEnabled = true
    }

    private fun onDownLoadError(downloadError: String) {
        dialogMessage(
            lifecycleOwner = this,
            message = downloadError
        )
    }

    private fun onError(error: String?) {
        val message = error ?: getString(R.string.search_unknown_error)
        val intent = Intent(this, SearchError::class.java).apply {
            putExtra(ERROR, message)
            flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        overridePendingTransition(0, 0)
        startActivity(intent)
    }

    private fun onSoftError(softError: String) {
        logW(softError)
        binder.resultSpinner.hide()
        binder.resultData.hide()
        binder.layoutError.layout.show()
        binder.layoutError.message.text = softError
    }

    private fun onResult(result: ModuleResult) {
        binder.resultFrame.show()
        binder.resultSpinner.hide()
        updateView(result)
    }

    private fun updateView(result: ModuleResult) {
        module = result.module!!

        logI("Response: title - " + module.getSongTitle())

        // Save module result into Search History
        viewModel.saveModuleToHistory(module)

        binder.resultData.scrollTo(0, 0)
        updateButtons()

        val size = module.bytes!! / 1024
        val info = getString(R.string.search_result_by, module.format, module.getArtist(), size)

        with(binder) {
            moduleTitle.text = module.getSongTitle()
            moduleFilename.text = module.filename
            moduleLicenseDescription.text = module.license!!.description
            moduleInstruments.text = module.parseInstruments()
            with(moduleInfo) {
                text = module.infoPageToHyperlink(info)
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
            }
            with(moduleLicense) {
                text = module.license!!.toHyperlink()
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
            }
            if (!module.comment.isNullOrEmpty()) {
                moduleCommentTitle.show()
                moduleCommentText.show()
                moduleCommentText.text = module.getComment()
            }
            if (result.hasSponsor()) {
                with(moduleSponsor) {
                    isClickable = true
                    movementMethod = LinkMovementMethod.getInstance()
                    text = result.sponsor!!.toHyperLink()
                    show()
                }
            }
        }
    }

    private fun playClick() {
        if (FileUtils.localFile(module).exists()) {
            val path = FileUtils.localFile(module).path
            val modList = ArrayList<String>()

            modList.add(path)
            XmpApplication.fileList = modList

            logI("Play $path")
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.PARM_START, 0)
            }
            startActivity(intent)
        } else {
            // Does not exist, download module
            val modDir = FileUtils.getDownloadPath(module)
            val url = module.url

            shouldPlay = true

            binder.moduleButtonPlay.showProgress {
                buttonText = getString(R.string.button_downloading)
                progressColor = Color.WHITE
                gravity = DrawableButton.GRAVITY_TEXT_START
            }

            logI("Downloading $url to $modDir")
            viewModel.downloadModule(module.filename!!, url!!, modDir)
        }
    }

    private fun deleteClick() {
        val file = FileUtils.localFile(module)
        val title = getString(R.string.title_delete_file)
        val message = getString(R.string.msg_delete_file, module.filename)
        yesNoDialog(this, title, message) {
            logD("Delete " + file.path)
            if (file.delete()) {
                updateButtons()
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
            updateButtons()
        }
    }

    private fun updateButtons() {
        // Block download of unsupported formats
        val isUnSupported = listOf(*UNSUPPORTED).contains(module.format)

        if (isUnSupported) {
            binder.moduleButtonPlay.text = getString(R.string.button_download_unsupported)
            binder.moduleButtonPlay.isEnabled = false
        } else {
            binder.moduleButtonPlay.isEnabled = true

            if (FileUtils.localFile(module).exists()) {
                // module exists, update button to reflect existence and enable Menu Delete
                deleteMenu?.findItem(R.id.menu_delete)?.isEnabled = true
                binder.moduleButtonPlay.text = getString(R.string.play)
            } else {
                // module does not exist, update button to download and disable Menu Delete
                deleteMenu?.findItem(R.id.menu_delete)?.isEnabled = false
                binder.moduleButtonPlay.text = getString(R.string.download)
            }
        }
    }

    companion object {
        private val UNSUPPORTED = arrayOf("AHX", "HVL", "MO3")
    }
}
