package org.helllabs.android.xmp.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import kotlinx.android.synthetic.main.activity_modlist.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.FileUtils
import org.helllabs.android.xmp.util.InfoCache
import org.helllabs.android.xmp.util.toast
import org.helllabs.android.xmp.util.yesNoDialog
import java.io.File
import java.text.DateFormat

class FilelistActivity : BasePlaylistActivity(), PlaylistAdapter.OnItemClickListener, PlaylistAdapter.OnItemLongClickListener {

    private var mNavigation: FilelistNavigation? = null
    override var isLoopMode: Boolean = false
    override var isShuffleMode: Boolean = false
    private var mBackButtonParentdir: Boolean = false

    //region [region] PlaylistChoice
    /**
     * Recursively add current directory to playlist
     */
    private val addCurrentRecursiveChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(this@FilelistActivity, recursiveList(mNavigation!!.currentDir),
                    PlaylistUtils.getPlaylistName(playlistSelection))
        }
    }


    /**
     * Recursively add directory to playlist
     */
    private val addRecursiveToPlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(this@FilelistActivity, recursiveList(mPlaylistAdapter.getFile(fileSelection)),
                    PlaylistUtils.getPlaylistName(playlistSelection))
        }
    }

    /**
     * Add one file to playlist
     */
    private val addFileToPlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(this@FilelistActivity, mPlaylistAdapter.getFilename(fileSelection),
                    PlaylistUtils.getPlaylistName(playlistSelection))
        }

    }

    /**
     * Add file list to playlist
     */
    private val addFileListToPlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(this@FilelistActivity, mPlaylistAdapter.filenameList,
                    PlaylistUtils.getPlaylistName(playlistSelection))
        }
    }
    //endregion

    override val allFiles: List<String>
        get() = recursiveList(mNavigation!!.currentDir)

    /**
     * For actions based on playlist selection made using choosePlaylist()
     */
    private interface PlaylistChoice {
        fun execute(fileSelection: Int, playlistSelection: Int)
    }

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mediaPath = prefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH)

        setContentView(R.layout.activity_modlist)
        setTitle(R.string.browser_filelist_title)

        // Adapter
        mPlaylistAdapter = PlaylistAdapter(this, ArrayList(), false, PlaylistAdapter.LAYOUT_LIST)
        mPlaylistAdapter.setOnItemClickListener(this)
        mPlaylistAdapter.setOnItemLongClickListener(this)

        modlist_listview.apply {
            layoutManager = LinearLayoutManager(this@FilelistActivity)
            adapter = mPlaylistAdapter
            addItemDecoration(DividerItemDecoration(this@FilelistActivity, DividerItemDecoration.VERTICAL))
        }

        setSwipeRefresh(swipeContainer)

        mNavigation = FilelistNavigation()

        //TODO I broke the color
        current_path.apply {
            setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    setTextColor(R.color.white)
                } else {
                    setTextColor(R.color.pressed_color)
                }

                view.performClick()
                false
            }
            setOnLongClickListener {
                val dialogTitle = "All Files"
                val dialogItems = listOf("Add to playlist", "Recursive add to playlist", "Add to play queue", "Set as default path", "Clear cache")
                val dialogOption = 0

                onLongClickMenu(dialogTitle, dialogItems, dialogOption)

                if (it.isPressed)
                    setTextColor(R.color.pressed_color)
                else
                    setTextColor(R.color.white)

                true
            }
        }

        mBackButtonParentdir = prefs.getBoolean(Preferences.BACK_BUTTON_NAVIGATION, true)

        // Back/Up button
        up_button.setOnClickListener {
            if (!mNavigation!!.isAtTopDir)
                parentDir()
        }

        // Check if directory exists
        val modDir = File(mediaPath!!)

        if (modDir.isDirectory) {
            mNavigation!!.startNavigation(modDir)
            updateModlist()
        } else {
            pathNotFound(mediaPath)
        }

        isShuffleMode = readShuffleModePref()
        isLoopMode = readLoopModePref()

        setupButtons()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Return to parent dir up to the starting level, then act as regular back
            if (!mNavigation!!.isAtTopDir) {
                parentDir()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    public override fun update() {
        updateModlist()
    }

    override fun onItemClick(adapter: PlaylistAdapter, view: View, position: Int) {
        val file = mPlaylistAdapter.getFile(position)

        if (mNavigation!!.changeDirectory(file)) {
            mNavigation!!.saveListPosition(modlist_listview)
            updateModlist()
        } else {
            super.onItemClick(adapter, view, position)
        }
    }

    override fun onLongItemClick(adapter: PlaylistAdapter, view: View, position: Int) {

        println("onLongItemClick: " + adapter + "\n" + view + "\n" + position)

        val dialogTitle: String
        val dialogItems: List<String>
        val dialogOption: Int

        if (mPlaylistAdapter.getFile(position)!!.isDirectory) {
            dialogTitle = "This Directory"
            dialogItems = listOf("Add to playlist", "Add to play queue", "Play contents", "Delete directory")
            dialogOption = 1
        } else {
            dialogTitle = "This File"
            dialogItems = listOf("Add to playlist", "Add to play queue", "Play this file", "Play all starting here", "Delete file")
            dialogOption = 2
        }

        onLongClickMenu(dialogTitle, dialogItems, dialogOption)
    }

    private fun onLongClickMenu(dialogTitle: String, dialogItems: List<String>, dialogOption: Int) {
        MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(text = dialogTitle)
            listItems(items = dialogItems) { _, index, _ ->
                when (dialogOption) {
                    0 -> onMenuAll(index)
                    1 -> onMenuDirectory(index)
                    2 -> onMenuFile(index)
                }
            }
        }
    }

    private fun pathNotFound(media_path: String) {

        MaterialDialog(this).show {
            title(text = "Path not found")
            message(text = "$media_path not found.\nCreate this directory or change the module path.")
            positiveButton(R.string.create) {
                val ret = Examples.install(this@FilelistActivity, media_path, prefs.getBoolean(Preferences.EXAMPLES, true))

                if (ret < 0)
                    error("Error creating directory $media_path.")

                mNavigation!!.startNavigation(File(media_path))
                updateModlist()
            }
            negativeButton(R.string.cancel) {
                finish()
            }
        }
    }

    private fun readShuffleModePref(): Boolean {
        return prefs.getBoolean(OPTIONS_SHUFFLE_MODE, DEFAULT_SHUFFLE_MODE)
    }

    private fun readLoopModePref(): Boolean {
        return prefs.getBoolean(OPTIONS_LOOP_MODE, DEFAULT_LOOP_MODE)
    }

    private fun parentDir() {
        if (mNavigation!!.parentDir()) {
            updateModlist()
            mNavigation!!.restoreListPosition(modlist_listview)
        }
    }

    private fun updateModlist() {
        val modDir = mNavigation!!.currentDir ?: return

        mPlaylistAdapter.clear()

        current_path!!.text = modDir.path

        val list = ArrayList<PlaylistItem>()
        val dirFiles = modDir.listFiles()
        if (dirFiles != null) {
            for (file in dirFiles) {
                val item: PlaylistItem
                item = if (file.isDirectory) {
                    PlaylistItem(PlaylistItem.TYPE_DIRECTORY, file.name, getString(R.string.directory))
                } else {
                    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(file.lastModified())
                    val comment = date + String.format(" (%d kB)", file.length() / 1024)
                    PlaylistItem(PlaylistItem.TYPE_FILE, file.name, comment)
                }
                item.file = file
                list.add(item)
            }
        }
        list.sort()
        PlaylistUtils.renumberIds(list)
        mPlaylistAdapter.addList(list)
        mPlaylistAdapter.notifyDataSetChanged()

        checkModList(list)
    }

    private fun checkModList(list: ArrayList<PlaylistItem>) {
        if (list.isEmpty()) {
            modlist_empty.visibility = View.VISIBLE
            modlist_listview.visibility = View.GONE
        } else {
            modlist_empty.visibility = View.GONE
            modlist_listview.visibility = View.VISIBLE
        }
    }

    private fun choosePlaylist(fileSelection: Int, choice: PlaylistChoice) {

        // Return if no playlists exist
        if (PlaylistUtils.list().isEmpty()) {
            toast(R.string.msg_no_playlists)
            return
        }

        val list = mutableListOf<String>()
        PlaylistUtils.listNoSuffix().forEach { list.add(it) }

        MaterialDialog(this).show {
            title(R.string.msg_select_playlist)
            listItemsSingleChoice(items = list, waitForPositiveButton = true) { _, index, _ ->
                choice.execute(fileSelection, index)
            }
            positiveButton(R.string.ok)
            negativeButton(R.string.cancel)
        }
    }

    private fun clearCachedEntries(fileList: List<String>) {
        for (filename in fileList) {
            InfoCache.clearCache(filename)
        }
    }

    private fun setDefaultPath() {
        val editor = prefs.edit()
        editor.putString(Preferences.MEDIA_PATH, mNavigation!!.currentDir!!.path)
        editor.apply()
        toast(text = "Set as default module path")
    }

    private fun deleteDirectory(position: Int) {
        val deleteName = mPlaylistAdapter.getFilename(position)
        val mediaPath = prefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH)

        if (deleteName.startsWith(mediaPath!!) && deleteName != mediaPath) {
            yesNoDialog("Delete directory", "Are you sure you want to delete directory \"" +
                    FileUtils.basename(deleteName) + "\" and all its contents?", Runnable {
                if (InfoCache.deleteRecursive(deleteName)) {
                    updateModlist()
                    toast(R.string.msg_dir_deleted)
                } else {
                    toast(R.string.msg_cant_delete_dir)
                }
            })
        } else {
            toast(R.string.error_dir_not_under_moddir)
        }
    }

    private fun deleteFile(position: Int) {
        val deleteName = mPlaylistAdapter.getFilename(position)
        yesNoDialog("Delete", "Are you sure you want to delete " + FileUtils.basename(deleteName) + "?", Runnable {
            if (InfoCache.delete(deleteName)) {
                updateModlist()
                toast(R.string.msg_file_deleted)
            } else {
                toast(R.string.msg_cant_delete)
            }
        })
    }

    private fun recursiveList(file: File?): List<String> {
        val list = ArrayList<String>()

        if (file == null)
            return list

        file.walkTopDown().forEach {
            if (!it.isDirectory)
                list.add(it.path)
        }

        return list
    }

    private fun onMenuAll(index: Int) {
        when (index) {
            0 -> choosePlaylist(0, addFileListToPlaylistChoice)     // Add all to playlist
            1 -> choosePlaylist(0, addCurrentRecursiveChoice)       // Add all to queue // Recursive add to playlist
            2 -> addToQueue(mPlaylistAdapter.filenameList)                      // Add all to queue
            3 -> setDefaultPath()                                               // Set as default path
            4 -> clearCachedEntries(mPlaylistAdapter.filenameList)              // Clear cache
        }
    }

    private fun onMenuDirectory(index: Int) {
        val adapterPosition = mPlaylistAdapter.position
        when (index) {
            0 -> choosePlaylist(adapterPosition, addRecursiveToPlaylistChoice)                      // Add to playlist (recursive)
            1 -> addToQueue(recursiveList(mPlaylistAdapter.getFile(adapterPosition)))               // Add to play queue (recursive)
            2 -> playModule(modList = recursiveList(mPlaylistAdapter.getFile(adapterPosition)))     // Play now (recursive)
            3 -> deleteDirectory(adapterPosition)                                                   // delete directory
        }
    }

    private fun onMenuFile(index: Int) {
        val adapterPosition = mPlaylistAdapter.position
        when (index) {
            0 -> choosePlaylist(adapterPosition, addFileToPlaylistChoice)                       //   Add to playlist
            1 -> addToQueue(mPlaylistAdapter.getFilename(adapterPosition))                      //   Add to queue
            2 -> playModule(mod = mPlaylistAdapter.getFilename(adapterPosition))                //   Play this module
            3 -> playModule(modList = mPlaylistAdapter.filenameList, start = adapterPosition)   //   Play all starting here
            4 -> deleteFile(adapterPosition)                                                    //   Delete file
        }
    }

    companion object {
        private const val TAG = "BasePlaylistActivity"
    }
}
