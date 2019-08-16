package org.helllabs.android.xmp.browser.playlist

import java.io.File
import java.io.IOException
import java.util.ArrayList

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.Message
import org.helllabs.android.xmp.util.ModInfo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText


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

        alert.setPositiveButton(R.string.ok) { dialog, whichButton ->
            val e1 = layout.findViewById<View>(R.id.new_playlist_name) as EditText
            val e2 = layout.findViewById<View>(R.id.new_playlist_comment) as EditText
            val name = e1.text.toString()
            val comment = e2.text.toString()

            if (createEmptyPlaylist(activity, name, comment) && runnable != null) {
                runnable.run()
            }
        }

        alert.setNegativeButton(R.string.cancel) { dialog, whichButton ->
            // Canceled.
        }

        alert.show()
    }

    /*
	 * Send files to the specified playlist
	 */
    private fun addFiles(activity: Activity, fileList: List<String>, playlistName: String) {
        val list = ArrayList<PlaylistItem>()
        val modInfo = ModInfo()
        var hasInvalid = false

        val id = 0
        for (filename in fileList) {
            if (Xmp.testModule(filename, modInfo)) {
                val item = PlaylistItem(PlaylistItem.TYPE_FILE, modInfo.name, modInfo.type)    // NOPMD
                item.file = File(filename)    // NOPMD
                list.add(item)
            } else {
                hasInvalid = true
            }
        }

        if (!list.isEmpty()) {
            Playlist.addToList(activity, playlistName, list)

            if (hasInvalid) {
                activity.runOnUiThread {
                    if (list.size > 1) {
                        Message.toast(activity, R.string.msg_only_valid_files_added)
                    } else {
                        Message.error(activity, R.string.unrecognized_format)
                    }
                }
            }
        }

        renumberIds(list)
    }

    fun filesToPlaylist(activity: Activity, fileList: List<String>, playlistName: String) {

        val progressDialog = ProgressDialog.show(activity, "Please wait", "Scanning module files...", true)

        object : Thread() {
            override fun run() {
                addFiles(activity, fileList, playlistName)
                progressDialog.dismiss()
            }
        }.start()
    }

    fun filesToPlaylist(activity: Activity, filename: String, playlistName: String) {
        val fileList = ArrayList<String>()
        fileList.add(filename)
        addFiles(activity, fileList, playlistName)
    }

    fun list(): Array<String> {
        val ret = Preferences.DATA_DIR.list(PlaylistFilter())
        return ret ?: emptyArray()
    }

    fun listNoSuffix(): Array<String> {
        val pList = list()
        for (i in pList.indices) {
            pList[i] = pList[i].substring(0, pList[i].lastIndexOf(Playlist.PLAYLIST_SUFFIX))    //NOPMD
        }
        return pList
    }

    fun getPlaylistName(index: Int): String {
        val pList = list()
        return pList[index].substring(0, pList[index].lastIndexOf(Playlist.PLAYLIST_SUFFIX))
    }

    fun createEmptyPlaylist(activity: Activity, name: String, comment: String): Boolean {
        try {
            val playlist = Playlist(activity, name)
            playlist.comment = comment
            playlist.commit()
            return true
        } catch (e: IOException) {
            Message.error(activity, activity.getString(R.string.error_create_playlist))
            return false
        }

    }

    // Stable IDs for used by Advanced RecyclerView
    fun renumberIds(list: List<PlaylistItem>) {
        var id = 0
        for (item in list) {
            item.id = id++
        }
    }
}
