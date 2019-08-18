package org.helllabs.android.xmp.browser.playlist

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.ModInfo
import org.helllabs.android.xmp.util.error
import org.helllabs.android.xmp.util.toast
import java.io.File
import java.io.IOException
import java.util.*


object PlaylistUtils {

    //TODO replace with an activity
    @SuppressLint("InflateParams")
    fun newPlaylistDialog(activity: Activity, runnable: Runnable?) {
        val alert = AlertDialog.Builder(activity)
        alert.setTitle("New playlist")
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.newlist, null)

        alert.setView(layout)

        alert.setPositiveButton(R.string.ok) { _, _ ->
            val e1 = layout.findViewById<View>(R.id.new_playlist_name) as EditText
            val e2 = layout.findViewById<View>(R.id.new_playlist_comment) as EditText
            val name = e1.text.toString()
            val comment = e2.text.toString()

            if (createEmptyPlaylist(activity, name, comment) && runnable != null) {
                runnable.run()
            }
        }

        alert.setNegativeButton(R.string.cancel) { _, _ ->
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

        for (filename in fileList) {
            if (Xmp.testModule(filename, modInfo)) {
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
                        activity.toast(R.string.msg_only_valid_files_added)
                    } else {
                        activity.error(R.string.unrecognized_format)
                    }
                }
            }
        }

        renumberIds(list)
    }

    fun filesToPlaylist(activity: Activity, fileList: List<String>, playlistName: String) {

        activity.toast("Please wait\nScanning module files...")

        object : Thread() {
            override fun run() {
                addFiles(activity, fileList, playlistName)
                activity.toast("Scan finished")

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
            pList[i] = pList[i].substring(0, pList[i].lastIndexOf(Playlist.PLAYLIST_SUFFIX))
        }
        return pList
    }

    fun getPlaylistName(index: Int): String {
        val pList = list()
        return pList[index].substring(0, pList[index].lastIndexOf(Playlist.PLAYLIST_SUFFIX))
    }

    fun createEmptyPlaylist(activity: Activity, name: String, comment: String): Boolean {
        return try {
            val playlist = Playlist(activity, name)
            playlist.comment = comment
            playlist.commit()
            true
        } catch (e: IOException) {
            activity.error(activity.getString(R.string.error_create_playlist))
            false
        }
    }

    // Stable IDs for used by Advanced RecyclerView
    fun renumberIds(list: List<PlaylistItem>) {
        for ((index, item) in list.withIndex()) {
            item.id = index
        }
    }
}
