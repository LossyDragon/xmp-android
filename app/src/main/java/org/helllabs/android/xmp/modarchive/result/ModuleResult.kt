package org.helllabs.android.xmp.modarchive.result

import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.ArrayList

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.modarchive.Downloader
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.modarchive.model.Module
import org.helllabs.android.xmp.modarchive.model.Sponsor
import org.helllabs.android.xmp.modarchive.request.ModuleRequest
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.ModuleResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.Log
import org.helllabs.android.xmp.util.Message

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest

open class ModuleResult : Result(), ModArchiveRequest.OnResponseListener, Downloader.DownloaderListener {
    private var title: TextView? = null
    private var filename: TextView? = null
    private var info: TextView? = null
    private var instruments: TextView? = null
    private var license: TextView? = null
    private var licenseDescription: TextView? = null
    private var sponsorText: TextView? = null
    private var module: Module? = null
    private var downloader: Downloader? = null
    private var downloadButton: Button? = null
    private var deleteButton: Button? = null
    private var playButton: Button? = null
    private var errorMessage: TextView? = null
    private var dataView: View? = null

    private var mPrefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_module)
        setupCrossfade()

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        title = findViewById<View>(R.id.module_title) as TextView
        filename = findViewById<View>(R.id.module_filename) as TextView
        info = findViewById<View>(R.id.module_info) as TextView
        instruments = findViewById<View>(R.id.module_instruments) as TextView
        license = findViewById<View>(R.id.module_license) as TextView
        licenseDescription = findViewById<View>(R.id.module_license_description) as TextView
        sponsorText = findViewById<View>(R.id.module_sponsor) as TextView

        downloadButton = findViewById<View>(R.id.module_download) as Button
        downloadButton!!.isEnabled = false

        deleteButton = findViewById<View>(R.id.module_delete) as Button
        deleteButton!!.isEnabled = false

        playButton = findViewById<View>(R.id.module_play) as Button
        playButton!!.isEnabled = false

        errorMessage = findViewById<View>(R.id.error_message) as TextView
        dataView = findViewById(R.id.result_data)

        downloader = Downloader(this)
        downloader!!.setDownloaderListener(this)

        val id = intent.getLongExtra(Search.MODULE_ID, -1)
        Log.d(TAG, "request module ID $id")
        makeRequest(id.toString())
    }

    protected open fun makeRequest(query: String) {
        val key = getString(R.string.modarchive_apikey)
        try {
            val request = ModuleRequest(key, ModArchiveRequest.MODULE, query)
            request.setOnResponseListener(this).send()
        } catch (e: UnsupportedEncodingException) {
            handleQueryError()
        }

    }


    // ModuleRequest callbacks

    override fun onResponse(response: ModArchiveResponse) {

        val moduleList = response as ModuleResponse
        if (moduleList.isEmpty) {
            dataView!!.visibility = View.GONE
        } else {
            val module = moduleList[0]
            Log.i(TAG, "Response: title=" + module.songTitle!!)
            title!!.text = module.songTitle
            filename!!.text = module.filename
            val size = module.bytes / 1024
            info!!.text = String.format("%s by %s (%d KB)", module.format, module.artist, size)
            license!!.text = Html.fromHtml("License: <a href=\"" + module.legalUrl + "\">" + module.license + "</a>")
            license!!.movementMethod = LinkMovementMethod.getInstance()
            licenseDescription!!.text = module.licenseDescription
            instruments!!.text = module.instruments
            this.module = module

            updateButtons(module)
        }

        val sponsor = response.sponsor
        if (sponsor != null) {
            sponsorText!!.text = Html.fromHtml("Download mirrors provided by <a href=\"" + sponsor.link + "\">" + sponsor.name + "</a>")
            sponsorText!!.movementMethod = LinkMovementMethod.getInstance()
        }

        crossfade()
    }

    override fun onSoftError(response: SoftErrorResponse) {
        errorMessage!!.text = response.message
        dataView!!.visibility = View.GONE
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

    fun downloadClick(view: View) {
        val modDir = getDownloadPath(module)
        val url = module!!.url

        Log.i(TAG, "Download $url to $modDir")
        downloader!!.download(url!!, modDir, module!!.bytes)
    }

    fun deleteClick(view: View) {
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
                if (contents != null && contents.size == 0) {
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

    fun playClick(view: View) {
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
        downloadButton!!.isEnabled = true
        deleteButton!!.isEnabled = exists
        playButton!!.isEnabled = exists
    }

    private fun localFile(module: Module?): File {
        val path = getDownloadPath(module)
        val url = module!!.url
        val filename = url!!.substring(url.lastIndexOf('#') + 1, url.length)
        return File(path, filename)
    }

    companion object {
        private val TAG = "ModuleResult"
        private val MODARCHIVE_DIRNAME = "TheModArchive"
    }
}
