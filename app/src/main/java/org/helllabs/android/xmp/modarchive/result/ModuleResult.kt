package org.helllabs.android.xmp.modarchive.result

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.PreferenceManager
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.showProgress
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Func
import com.tonyodev.fetch2core.Reason
import kotlinx.android.synthetic.main.layout_error.*
import kotlinx.android.synthetic.main.result_module.*
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.extension.toast
import org.helllabs.android.xmp.extension.yesNoDialog
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.modarchive.model.Module
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest
import org.helllabs.android.xmp.modarchive.request.ModuleRequest
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.ModuleResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.Log
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.*

// Download sampled from Fetch-3.0 SingleDownloaderActivity.
open class ModuleResult : Result(), ModArchiveRequest.OnResponseListener, FetchObserver<Download> {

    private var module: Module? = null

    private var mPrefs: SharedPreferences? = null

    private var shouldPlay = false

    private var fetch: Fetch? = null
    private var request: Request? = null

    private lateinit var deleteMenu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_module)
        setupCrossfade()

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        module_button_play.apply {
            attachTextChangeAnimator()
            bindProgressButton(this)
            setOnClickListener { playClick() }
        }

        module_button_random.setOnClickListener { randomClick() }

        // updateButtons(module)

        val id = intent.getLongExtra(Search.MODULE_ID, -1)

        Log.d(TAG, "request module ID $id")
        makeRequest(id.toString())
    }

    protected open fun makeRequest(query: String) {
        val key = BuildConfig.ApiKey
        try {
            val request = ModuleRequest(key, ModArchiveRequest.MODULE, query)
            request.setOnResponseListener(this).send(this)
        } catch (e: UnsupportedEncodingException) {
            handleQueryError()
        }
    }

    override fun onResume() {
        super.onResume()
        fetch?.attachFetchObserversForDownload(request!!.id, this)
    }

    override fun onPause() {
        super.onPause()
        fetch?.removeFetchObserversForDownload(request!!.id, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        fetch?.close()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        deleteMenu = menu!!

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            deleteClick()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onChanged(data: Download, reason: Reason) {
        updateView(data, reason)
    }

    private fun updateView(download: Download, reason: Reason) {
        if (request!!.id == download.id) {

            updateProgressView(download.status)

            if (download.error != Error.NONE) {
                error(download.error.toString() + " " + reason)
            }
        }
    }

    private fun updateProgressView(status: Status) {

        when (status) {
            Status.CANCELLED -> {
                toast(R.string.msg_download_cancelled)
                updateButtons(module)
                module_button_random.isEnabled = true
            }
            Status.QUEUED -> {
                Log.d(TAG, "Download Queued")
                module_button_random.isEnabled = false
            }
            Status.ADDED -> {
                Log.d(TAG, "Download Added")
            }
            Status.COMPLETED -> {
                Log.d(TAG, "Download Complete")
                updateButtons(module)
                module_button_random.isEnabled = true
            }
            Status.DOWNLOADING -> {
                Log.d(TAG, "Download Downloading")
            }
            else -> {
                Log.w(TAG, "updateProgressView unknown: $status")
            }
        }
    }

    // ModuleRequest callbacks
    override fun onResponse(response: ModArchiveResponse) {

        val moduleList = response as ModuleResponse
        if (moduleList.isEmpty) {
            result_data!!.visibility = View.GONE
        } else {
            val module = moduleList[0]
            Log.i(TAG, "Response: title - " + module.songTitle!!)
            module_title!!.text = module.songTitle
            module_filename!!.text = module.filename
            val size = module.bytes / 1024
            module_info!!.text =
                    String.format("%s by %s (%d KB)", module.format, module.artist, size)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                module_license!!.text = Html.fromHtml(
                        "License: <a href=\"" +
                                module.legalUrl + "\">" +
                                module.license + "</a>",
                        Html.FROM_HTML_MODE_LEGACY
                )
            } else {
                @Suppress("DEPRECATION")
                module_license!!.text = Html.fromHtml(
                        "License: <a href=\"" +
                                module.legalUrl + "\">" +
                                module.license + "</a>"
                )
            }

            module_license!!.movementMethod = LinkMovementMethod.getInstance()
            module_license_description!!.text = module.licenseDescription
            module_instruments!!.text = module.instruments
            this.module = module

            updateButtons(module)
        }

        val sponsor = response.sponsor
        if (sponsor?.name != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                module_sponsor!!.text =
                        Html.fromHtml(
                                "Download mirrors provided by <a href=\"" +
                                        sponsor.link + "\">" +
                                        sponsor.name + "</a>",
                                Html.FROM_HTML_MODE_LEGACY
                        )
            } else {
                @Suppress("DEPRECATION")
                module_sponsor!!.text =
                        Html.fromHtml(
                                "Download mirrors provided by <a href=\"" +
                                        sponsor.link + "\">" +
                                        sponsor.name + "</a>"
                        )
            }
            module_sponsor!!.movementMethod = LinkMovementMethod.getInstance()
        }

        crossfade()
    }

    override fun onSoftError(response: SoftErrorResponse) {
        error_message!!.text = response.message
        result_data!!.visibility = View.GONE
        crossfade()
    }

    override fun onHardError(response: HardErrorResponse) {
        handleError(response.error)
    }

    private fun randomClick() {

        val intent = Intent(this, RandomResult::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        startActivity(Intent(intent))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun playClick() {

        if (localFile(module).exists()) {
            // Exist, play module
            val path = localFile(module).path
            val modList = ArrayList<String>()

            modList.add(path)

            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra(PlayerActivity.PARM_START, 0)

            xmpApplication().fileList = modList
            Log.i(TAG, "Play $path")
            startActivity(intent)
        } else {
            // Does not exist, download module
            val modDir = getDownloadPath(module)
            val url = module!!.url

            shouldPlay = true

            module_button_play.showProgress {
                buttonText = getString(R.string.button_downloading)
                progressColor = Color.WHITE
                gravity = DrawableButton.GRAVITY_TEXT_START
            }

            Log.i(TAG, "Download $url to $modDir")
            download(module!!.filename!!, url!!, modDir)
        }
    }

    private fun deleteClick() {
        val file = localFile(module)
        val title = getString(R.string.title_delete_file)
        val message = String.format(getString(R.string.msg_delete_file), module!!.filename)
        yesNoDialog(title, message) { result ->
            if (result) {
                Log.i(TAG, "Delete " + file.path)
                if (file.delete()) {
                    updateButtons(module)
                } else {
                    toast(R.string.error)
                }
                if (PrefManager.artistFolder) {
                    val parent = file.parentFile!!
                    val contents = parent.listFiles()
                    if (contents != null && contents.isEmpty()) {
                        try {
                            val path = PrefManager.mediaPath
                            val mediaPath = File(path).canonicalPath
                            val parentPath = parent.canonicalPath

                            if (parentPath.startsWith(mediaPath) && parentPath != mediaPath) {
                                Log.i(TAG, "Remove empty directory " + parent.path)
                                if (!parent.delete()) {
                                    toast(R.string.msg_error_remove_directory)
                                    Log.e(TAG, "error removing directory")
                                }
                            }
                        } catch (e: IOException) {
                            Log.e(TAG, e.message.toString())
                        }
                    }
                }
                updateButtons(module)
            }
        }
    }

    private fun getDownloadPath(module: Module?): String {

        val sb = StringBuilder()
        sb.append(PrefManager.mediaPath)

        if (PrefManager.modArchiveFolder) {
            sb.append(File.separatorChar)
            sb.append(getString(R.string.dirname_theModArchive))
        }

        if (PrefManager.artistFolder) {
            sb.append(File.separatorChar)
            sb.append(module!!.artist)
        }

        return sb.toString()
    }

    private fun updateButtons(module: Module?) {

        if (localFile(module).exists()) {
            // module exists, update button to reflect existance and enable Menu Delete
            deleteMenu.findItem(R.id.menu_delete).isEnabled = true
            module_button_play.text = getString(R.string.button_play)
        } else if (!localFile(module).exists() || module == null) {
            // module does not exist, update button to download and disable Menu Delete
            deleteMenu.findItem(R.id.menu_delete).isEnabled = false
            module_button_play.text = getString(R.string.button_download)
        }
    }

    private fun download(mod: String, url: String, path: String) {

        if (localFile(url, path).exists()) {
            yesNoDialog(
                    "File exists!",
                    "This module already exists. Do you want to overwrite?") { result ->
                if (result) {
                    modDownloader(mod, url, path)
                }
            }
        } else {
            modDownloader(mod, url, path)
        }
    }

    private fun modDownloader(mod: String, url: String, file: String) {

        val pathFile = File(file)
        pathFile.mkdirs()

        if (fetch == null)
            fetch = Fetch.getDefaultInstance()

        request = Request(url, "$pathFile/$mod")

        fetch!!.attachFetchObserversForDownload(request!!.id, this)
                .enqueue(request!!, Func { updatedRequests ->
                    request = updatedRequests
                }, Func { error ->
                    Log.e(TAG, "enqueue: $error")
                })
    }

    private fun localFile(module: Module?): File {
        val path = getDownloadPath(module)
        val url = module!!.url
        val moduleFilename = url!!.substring(url.lastIndexOf('#') + 1, url.length)
        return File(path, moduleFilename)
    }

    private fun localFile(url: String, path: String): File {
        val filename = url.substring(url.lastIndexOf('#') + 1, url.length)
        return File(path, filename)
    }

    companion object {
        private val TAG = ModuleResult::class.java.simpleName
    }
}
