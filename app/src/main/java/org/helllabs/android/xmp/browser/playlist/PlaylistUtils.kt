package org.helllabs.android.xmp.browser.playlist

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.view.*
import android.widget.EditText
import java.io.File
import java.io.IOException
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.testModule
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.Message.error
import org.helllabs.android.xmp.util.Message.toast
import org.helllabs.android.xmp.util.ModInfo

object PlaylistUtils {
    fun newPlaylistDialog(activity: Activity) {
        newPlaylistDialog(activity, null)
    }

    @SuppressLint("InflateParams")
    fun newPlaylistDialog(activity: Activity, runnable: Runnable?) {
        val alert = AlertDialog.Builder(activity)
        alert.setTitle("New playlist")
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.newlist, null)
        alert.setView(layout)
        alert.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            val e1 = layout.findViewById<View>(R.id.new_playlist_name) as EditText
            val e2 = layout.findViewById<View>(R.id.new_playlist_comment) as EditText
            val name = e1.text.toString()
            val comment = e2.text.toString()
            if (createEmptyPlaylist(activity, name, comment) && runnable != null) {
                runnable.run()
            }
        }
        alert.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
        alert.show()
    }

    /*
     * Send files to the specified playlist
     */
    private fun addFiles(activity: Activity, fileList: List<String?>?, playlistName: String?) {
        val list: MutableList<PlaylistItem> = ArrayList()
        val modInfo = ModInfo()
        var hasInvalid = false
        val id = 0
        for (filename in fileList!!) {
            if (testModule(filename!!, modInfo)) {
                val item = PlaylistItem(PlaylistItem.TYPE_FILE, modInfo.name, modInfo.type)
                item.file = File(filename)
                list.add(item)
            } else {
                hasInvalid = true
            }
        }
        if (list.isNotEmpty()) {
            Playlist.addToList(activity, playlistName, list)
            if (hasInvalid) {
                activity.runOnUiThread {
                    if (list.size > 1) {
                        toast(activity, R.string.msg_only_valid_files_added)
                    } else {
                        error(activity, R.string.unrecognized_format)
                    }
                }
            }
        }
        renumberIds(list)
    }

    fun filesToPlaylist(activity: Activity, fileList: List<String?>?, playlistName: String?) {
        val progressDialog = ProgressDialog.show(
            activity,
            "Please wait",
            "Scanning module files...",
            true
        )
        object : Thread() {
            override fun run() {
                addFiles(activity, fileList, playlistName)
                progressDialog.dismiss()
            }
        }.start()
    }

    fun filesToPlaylist(activity: Activity, filename: String?, playlistName: String?) {
        val fileList: MutableList<String?> = ArrayList()
        fileList.add(filename)
        addFiles(activity, fileList, playlistName)
    }

    fun list(): Array<String?> {
        val ret = Preferences.DATA_DIR.list(PlaylistFilter())
        return ret ?: arrayOfNulls(0)
    }

    fun listNoSuffix(): Array<String?> {
        val pList = list()
        for (i in pList.indices) {
            pList[i] = pList[i]!!.substring(0, pList[i]!!.lastIndexOf(Playlist.PLAYLIST_SUFFIX))
        }
        return pList
    }

    fun getPlaylistName(index: Int): String {
        val pList = list()
        return pList[index]!!.substring(0, pList[index]!!.lastIndexOf(Playlist.PLAYLIST_SUFFIX))
    }

    fun createEmptyPlaylist(activity: Activity, name: String?, comment: String?): Boolean {
        return try {
            val playlist = Playlist(activity, name)
            playlist.comment = comment
            playlist.commit()
            true
        } catch (e: IOException) {
            error(activity, activity.getString(R.string.error_create_playlist))
            false
        }
    }

    // Stable IDs for used by Advanced RecyclerView
    fun renumberIds(list: List<PlaylistItem>) {
        list.forEachIndexed { index, playlistItem ->
            playlistItem.id = index
        }
    }
}
