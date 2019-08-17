package org.helllabs.android.xmp.modarchive.result

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.error_message.*
import kotlinx.android.synthetic.main.result_module.*
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.modarchive.Downloader
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.modarchive.model.Module
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest
import org.helllabs.android.xmp.modarchive.request.ModuleRequest
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.ModuleResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.Log
import org.helllabs.android.xmp.util.Message
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.*

open class ModuleResult : Result(), ModArchiveRequest.OnResponseListener, Downloader.DownloaderListener {

    private var module: Module? = null
    private var downloader: Downloader? = null

    private var mPrefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_module)
        setupCrossfade()

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        module_download.isEnabled = false
        module_delete.isEnabled = false
        module_play.isEnabled = false

        downloader = Downloader(this)
        downloader!!.setDownloaderListener(this)

        val id = intent.getLongExtra(Search.MODULE_ID, -1)
        Log.d(TAG, "request module ID $id")
        makeRequest(id.toString())

        //Button actions
        module_play.setOnClickListener {
            onPlay()
        }

        module_delete.setOnClickListener {
            onDelete()
        }

        module_download.setOnClickListener {
            onDownload()
        }
    }

    protected open fun makeRequest(query: String) {
        try {
            val request = ModuleRequest(BuildConfig.ApiKey, ModArchiveRequest.MODULE, query)
            request.setOnResponseListener(this).send()
        } catch (e: UnsupportedEncodingException) {
            handleQueryError()
        }
    }

    // ModuleRequest callbacks
    override fun onResponse(response: ModArchiveResponse) {

        val moduleList = response as ModuleResponse
        if (moduleList.isEmpty) {
            result_data.visibility = View.GONE
        } else {
            val module = moduleList[0]
            Log.i(TAG, "Response: title=" + module.songTitle!!)
            module_title.text = module.songTitle
            module_filename.text = module.filename
            val size = module.bytes / 1024
            module_info.text = String.format("%s by %s (%d KB)", module.format, module.artist, size)
            @Suppress("DEPRECATION")
            module_license.text = Html.fromHtml("License: <a href=\"" + module.legalUrl + "\">" + module.license + "</a>")
            module_license.movementMethod = LinkMovementMethod.getInstance()
            module_license_description.text = module.licenseDescription
            module_instruments.text = module.instruments
            this.module = module

            updateButtons(module)
        }

        val sponsor = response.sponsor
        if (sponsor != null) {
            @Suppress("DEPRECATION")
            module_sponsor.text = Html.fromHtml("Download mirrors provided by <a href=\"" + sponsor.link + "\">" + sponsor.name + "</a>")
            module_sponsor.movementMethod = LinkMovementMethod.getInstance()
        }

        crossfade()
    }

    override fun onSoftError(response: SoftErrorResponse) {
        error_message.text = response.message
        result_data.visibility = View.GONE
        crossfade()
    }

    override fun onHardError(response: HardErrorResponse) {
        handleError(response.error)
    }

    // DownloaderListener callbacks

    override fun onSuccess() {
        updateButtons(module)
    }

    override fun onFailure() {
        // do nothing
    }

    // Button click handlers
    private fun onDownload() {
        val modDir = getDownloadPath(module)
        val url = module!!.url

        Log.i(TAG, "Download $url to $modDir")
        downloader!!.download(url!!, modDir, module!!.bytes)
    }

    private fun onDelete() {
        val file = localFile(module)

        Message.yesNoDialog(this, "Delete file", "Are you sure you want to delete " + module!!.filename + "?", Runnable {
            Log.i(TAG, "Delete " + file.path)
            if (file.delete()) {
                updateButtons(module)
            } else {
                Message.toast(this@ModuleResult, "Error")
            }

            // Delete parent directory if empty
            if (mPrefs!!.getBoolean(Preferences.ARTIST_FOLDER, true)) {
                val parent = file.parentFile
                val contents = parent!!.listFiles()
                if (contents != null && contents.isEmpty()) {
                    try {
                        val mediaPath = File(mPrefs!!.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH)!!).canonicalPath
                        val parentPath = parent.canonicalPath

                        if (parentPath.startsWith(mediaPath) && parentPath != mediaPath) {
                            Log.i(TAG, "Remove empty directory " + parent.path)
                            if (!parent.delete()) {
                                Message.toast(this@ModuleResult, "Error removing directory")
                                Log.e(TAG, "error removing directory")
                            }
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, e.message!!)
                    }

                }
            }
        })
    }

    private fun onPlay() {
        val path = localFile(module).path
        val modList = ArrayList<String>()

        modList.add(path)

        val intent = Intent(this, PlayerActivity::class.java)
        (application as XmpApplication).fileList = modList
        intent.putExtra(PlayerActivity.PARM_START, 0)
        Log.i(TAG, "Play $path")
        startActivity(intent)
    }

    private fun getDownloadPath(module: Module?): String {
        val sb = StringBuilder()

        sb.append(mPrefs!!.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH))

        if (mPrefs!!.getBoolean(Preferences.MODARCHIVE_FOLDER, true)) {
            sb.append(File.separatorChar)
            sb.append(MODARCHIVE_DIRNAME)
        }

        if (mPrefs!!.getBoolean(Preferences.ARTIST_FOLDER, true)) {
            sb.append(File.separatorChar)
            sb.append(module!!.artist)
        }

        return sb.toString()
    }

    private fun updateButtons(module: Module?) {
        val exists = localFile(module).exists()
        module_download.isEnabled = true
        module_delete.isEnabled = exists
        module_play.isEnabled = exists
    }

    private fun localFile(module: Module?): File {
        val path = getDownloadPath(module)
        val url = module!!.url
        val filename = url!!.substring(url.lastIndexOf('#') + 1, url.length)
        return File(path, filename)
    }

    companion object {
        private const val TAG = "ModuleResult"
        private const val MODARCHIVE_DIRNAME = "TheModArchive"
    }
}
