package org.helllabs.android.xmp.modarchive.result

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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.databinding.ActivityResultModuleBinding
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.modarchive.SearchError
import org.helllabs.android.xmp.modarchive.SearchHistory
import org.helllabs.android.xmp.modarchive.result.ModuleResultViewModel.ModuleState
import org.helllabs.android.xmp.model.History
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.*
import java.io.File
import java.io.IOException

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

        binder.appbar.toolbarText.text = getString(R.string.search_module_title)

        binder.moduleButtonPlay.apply {
            attachTextChangeAnimator()
            bindProgressButton(this)
            click { playClick() }
        }

        binder.moduleButtonRandom.click {
            viewModel.getRandomModule()
            binder.appbar.toolbarText.text = getString(R.string.search_random_title)
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

    private fun onLoad() {
        binder.resultFrame.hide()
        binder.resultSpinner.show()
        binder.moduleButtonPlay.isEnabled = false
        binder.moduleButtonPlay.text = getString(R.string.button_loading)
    }

    private fun onCancelled() {
        toast(R.string.msg_download_cancelled)
        updateButtons(module)
        binder.moduleButtonRandom.isEnabled = true
    }

    private fun onQueued() {
        logI("Download Queued")
        binder.moduleButtonRandom.isEnabled = false
    }

    private fun onComplete() {
        logI("Download Complete")
        updateButtons(module)
        binder.moduleButtonRandom.isEnabled = true
    }

    private fun onDownLoadError(downloadError: String) {
        generalError(downloadError)
    }

    private fun onError(error: String?) {
        val message = error ?: getString(R.string.search_unknown_error)
        val intent = Intent(this, SearchError::class.java)
        intent.putExtra(ERROR, message)
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        deleteMenu = menu

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete)
            deleteClick()

        return super.onOptionsItemSelected(item)
    }

    private fun updateView(result: ModuleResult) {
        this.module = result.module!!

        logI("Response: title - " + module.getSongTitle())

        // Save module result into Search History
        saveModuleToHistory(module)

        binder.resultData.scrollTo(0, 0)
        updateButtons(module)

        val size = module.bytes!! / 1024
        val info = getString(R.string.search_result_by, module.format, module.getArtist(), size)

        binder.moduleTitle.text = module.getSongTitle()
        binder.moduleFilename.text = module.filename
        binder.moduleInfo.text = (
            "<a href=\"" + module.infopage + "\">" + info + "</a>"
            ).asHtml()
        binder.moduleInfo.movementMethod = LinkMovementMethod.getInstance()
        binder.moduleInfo.linksClickable = true
        binder.moduleLicense.text = (
            "<a href=\"" + module.license!!.legalurl + "\">" + module.license!!.title + "</a>"
            ).asHtml()
        binder.moduleLicense.movementMethod = LinkMovementMethod.getInstance()
        binder.moduleLicense.linksClickable = true
        binder.moduleLicenseDescription.text = module.license!!.description
        binder.moduleInstruments.text = module.parseInstruments()

        // If a module has a comment / message
        if (!module.comment.isNullOrEmpty()) {
            binder.moduleCommentTitle.show()
            binder.moduleCommentText.show()
            binder.moduleCommentText.text = module.getComment()
        }

        val sponsor = result.sponsor
        if (sponsor!!.hasSponsor()) {
            binder.moduleSponsor.show()
            binder.moduleSponsor.text = (
                "Download mirrors provided by <a href=\"" +
                    sponsor.link + "\">" +
                    sponsor.text + "</a>"
                ).asHtml()
            binder.moduleSponsor.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun playClick() {
        if (localFile(module).exists()) {
            val path = localFile(module).path
            val modList = ArrayList<String>()

            modList.add(path)
            XmpApplication.instance!!.fileList = modList

            logI("Play $path")
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra(PlayerActivity.PARM_START, 0)
            startActivity(intent)
        } else {
            // Does not exist, download module
            val modDir = getDownloadPath(module)
            val url = module.url

            shouldPlay = true

            binder.moduleButtonPlay.showProgress {
                buttonText = getString(R.string.button_downloading)
                progressColor = Color.WHITE
                gravity = DrawableButton.GRAVITY_TEXT_START
            }

            logI("Downloaded $url to $modDir")
            download(module.filename!!, url!!, modDir)
        }
    }

    private fun deleteClick() {
        val file = localFile(module)
        val title = getString(R.string.title_delete_file)
        val message = getString(R.string.msg_delete_file, module.filename)
        yesNoDialog(title, message) {
            logD("Delete " + file.path)
            if (file.delete()) {
                updateButtons(module)
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
            updateButtons(module)
        }
    }

    private fun getDownloadPath(module: Module?): String {
        val sb = StringBuilder()
        sb.append(PrefManager.mediaPath)

        if (PrefManager.useModArchiveFolder) {
            sb.append(File.separatorChar)
            sb.append(getString(R.string.dirname_theModArchive))
        }

        if (PrefManager.useArtistFolder) {
            sb.append(File.separatorChar)
            sb.append(module!!.getArtist().asHtml())
        }

        return sb.toString()
    }

    private fun updateButtons(module: Module?) {
        // Should make sure this never happens
        if (module == null)
            return

        // Block download of unsupported formats
        val isUnSupported = listOf(*UNSUPPORTED).contains(module.format)

        if (isUnSupported) {
            binder.moduleButtonPlay.text = getString(R.string.button_download_unsupported)
            binder.moduleButtonPlay.isEnabled = false
        } else {
            binder.moduleButtonPlay.isEnabled = true

            if (localFile(module).exists()) {
                // module exists, update button to reflect existence and enable Menu Delete
                deleteMenu?.findItem(R.id.menu_delete)?.isEnabled = true
                binder.moduleButtonPlay.text = getString(R.string.result_play)
            } else {
                // module does not exist, update button to download and disable Menu Delete
                deleteMenu?.findItem(R.id.menu_delete)?.isEnabled = false
                binder.moduleButtonPlay.text = getString(R.string.download)
            }
        }
    }

    private fun download(mod: String, url: String, path: String) {
        if (localFile(url, path).exists()) {
            val title = getString(R.string.msg_file_exists)
            val message = getString(R.string.msg_file_exists_overwrite)
            yesNoDialog(title, message) {
                viewModel.downloadModule(mod, url, path)
            }
        } else {
            viewModel.downloadModule(mod, url, path)
        }
    }

    private fun localFile(module: Module?): File {
        val url = module!!.url
        val moduleFilename = url!!.substring(url.lastIndexOf('#') + 1, url.length)
        return File(getDownloadPath(module), moduleFilename)
    }

    private fun localFile(url: String, path: String): File {
        val filename = url.substring(url.lastIndexOf('#') + 1, url.length)
        return File(path, filename)
    }

    private fun saveModuleToHistory(module: Module) {
        // Load history list first
        val searchHistory = getSearchHistory().toMutableList()

        // Load the current module in a data model
        // Load the module into a model
        val historyModel = History(
            module.id!!,
            module.songtitle!!,
            module.getArtist(),
            module.format!!,
            System.currentTimeMillis()
        )

        // Check to see if the module has been searched before. Skip if true
        searchHistory.forEach {
            if (it.id == historyModel.id)
                return
        }

        // Remove the oldest item if history length is reached
        if (searchHistory.size >= SearchHistory.HISTORY_LENGTH)
            searchHistory.removeFirst()

        // Add the current module into the history
        searchHistory.add(historyModel)

        // Convert into GSON and save it
        PrefManager.searchHistory = Gson().toJson(searchHistory)
    }

    private fun getSearchHistory(): List<History> {
        val type = object : TypeToken<List<History?>?>() {}.type
        return Gson().fromJson<List<History>>(PrefManager.searchHistory, type).orEmpty()
    }

    companion object {
        private val UNSUPPORTED = arrayOf("AHX", "HVL", "MO3")
    }
}
