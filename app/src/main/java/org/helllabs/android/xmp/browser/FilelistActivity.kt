package org.helllabs.android.xmp.browser

import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import kotlinx.android.synthetic.main.activity_modlist.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.*
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.*
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
            filesToPlaylist(this@FilelistActivity, recursiveList(mNavigation!!.currentDir),
                    getPlaylistName(playlistSelection))
        }
    }

    /**
     * Recursively add directory to playlist
     */
    private val addRecursiveToPlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            filesToPlaylist(this@FilelistActivity, recursiveList(mPlaylistAdapter.getFile(fileSelection)),
                    getPlaylistName(playlistSelection))
        }
    }

    /**
     * Add one file to playlist
     */
    private val addFileToPlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            filesToPlaylist(this@FilelistActivity, mPlaylistAdapter.getFilename(fileSelection),
                    getPlaylistName(playlistSelection))
        }

    }

    /**
     * Add file list to playlist
     */
    private val addFileListToPlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            filesToPlaylist(this@FilelistActivity, mPlaylistAdapter.filenameList,
                    getPlaylistName(playlistSelection))
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

        val textColor = current_path.currentTextColor
        current_path.apply {
            setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    setTextColor(textColor)
                } else {
                    @Suppress("DEPRECATION")
                    setTextColor(resources.getColor(R.color.pressed_color))
                }

                view.performClick()
                false
            }
            setOnLongClickListener {
                onLongClickMenu(R.string.title_all_files, R.array.dialog_all_files, ALL_FILES, 0)
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

    //Override the 'back' arrow on toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Return to parent dir up to the starting level, then act as regular back
        if (mBackButtonParentdir) {
            if (!mNavigation!!.isAtTopDir) {
                parentDir()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
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
        val dialogTitle: Int
        val dialogItems: Int
        val dialogOption: Int

        if (mPlaylistAdapter.getFile(position)!!.isDirectory) {
            dialogTitle = R.string.title_this_directory
            dialogItems = R.array.dialog_directory
            dialogOption = THIS_DIRECTORY
        } else {
            dialogTitle = R.string.title_this_file
            dialogItems = R.array.dialog_file
            dialogOption = THIS_FILE
        }

        onLongClickMenu(dialogTitle, dialogItems, dialogOption, position)
    }

    private fun onLongClickMenu(dialogTitle: Int, dialogItems: Int, dialogOption: Int, position: Int) {
        MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(dialogTitle)
            listItems(dialogItems) { _, index, _ ->
                when (dialogOption) {
                    ALL_FILES -> onMenuAll(index)
                    THIS_DIRECTORY -> onMenuDirectory(index, position)
                    THIS_FILE -> onMenuFile(index, position)
                }
            }
        }
    }

    private fun pathNotFound(media_path: String) {
        MaterialDialog(this).show {
            title(R.string.title_no_path)
            message(text = String.format(getString(R.string.msg_no_path), media_path))
            positiveButton(R.string.create) {
                val ret = Examples.install(this@FilelistActivity, media_path, prefs.getBoolean(Preferences.EXAMPLES, true))

                if (ret < 0)
                    this@FilelistActivity.error(text = String.format(getString(R.string.error_create_path), media_path))

                mNavigation!!.startNavigation(File(media_path))
                updateModlist()
            }
            negativeButton(R.string.cancel) {
                finish()
            }
        }
    }

    private fun readShuffleModePref(): Boolean {
        return prefs.getBoolean(Preferences.OPTIONS_SHUFFLE_MODE, DEFAULT_SHUFFLE_MODE)
    }

    private fun readLoopModePref(): Boolean {
        return prefs.getBoolean(Preferences.OPTIONS_LOOP_MODE, DEFAULT_LOOP_MODE)
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
                    val comment = date + String.format(getString(R.string.format_kb), file.length() / 1024)
                    PlaylistItem(PlaylistItem.TYPE_FILE, file.name, comment)
                }
                item.file = file
                list.add(item)
            }
        }
        list.sort()
        renumberIds(list)
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
        if (list().isEmpty()) {
            toast(R.string.msg_no_playlists)
            return
        }

        val list = mutableListOf<String>()
        listNoSuffix().forEach { list.add(it) }

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
        toast(R.string.toast_default_path)
    }

    private fun deleteDirectory(position: Int) {
        val deleteName = mPlaylistAdapter.getFilename(position)
        val mediaPath = prefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH)

        if (deleteName.startsWith(mediaPath!!) && deleteName != mediaPath) {
            yesNoDialog(getString(R.string.title_delete_directory), String.format(getString(R.string.msg_delete_file, FileUtils.basename(deleteName))), Runnable {
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
        yesNoDialog(getString(R.string.title_delete_file), String.format(getString(R.string.msg_delete_file, FileUtils.basename(deleteName))), Runnable {
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

    private fun onMenuDirectory(index: Int, position: Int) {
        when (index) {
            0 -> choosePlaylist(position, addRecursiveToPlaylistChoice)                      // Add to playlist (recursive)
            1 -> addToQueue(recursiveList(mPlaylistAdapter.getFile(position)))               // Add to play queue (recursive)
            2 -> playModule(modList = recursiveList(mPlaylistAdapter.getFile(position)))     // Play now (recursive)
            3 -> deleteDirectory(position)                                                   // delete directory
        }
    }

    private fun onMenuFile(index: Int, position: Int) {
        when (index) {
            0 -> choosePlaylist(position, addFileToPlaylistChoice)                       //   Add to playlist
            1 -> addToQueue(mPlaylistAdapter.getFilename(position))                      //   Add to queue
            2 -> playModule(mod = mPlaylistAdapter.getFilename(position))                //   Play this module
            3 -> playModule(modList = mPlaylistAdapter.filenameList, start = position)   //   Play all starting here
            4 -> deleteFile(position)                                                    //   Delete file
        }
    }

    companion object {
        private val TAG = FilelistActivity::class.java.simpleName

        private const val ALL_FILES = 0
        private const val THIS_FILE = 1
        private const val THIS_DIRECTORY = 2
    }
}
