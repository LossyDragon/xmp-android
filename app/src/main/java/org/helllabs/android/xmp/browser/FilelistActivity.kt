package org.helllabs.android.xmp.browser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator
import kotlinx.android.synthetic.main.modlist.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.*
import java.io.File
import java.text.DateFormat
import java.util.*

class FilelistActivity : BasePlaylistActivity(), PlaylistAdapter.OnItemClickListener {

    private var mNavigation: FilelistNavigation? = null
    private var isPathMenu: Boolean = false
    override var isLoopMode: Boolean = false
    override var isShuffleMode: Boolean = false
    private var mBackButtonParentdir: Boolean = false
    private var mCrossfade: Crossfader? = null

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
            PlaylistUtils.filesToPlaylist(this@FilelistActivity, recursiveList(mPlaylistAdapter!!.getFile(fileSelection)),
                    PlaylistUtils.getPlaylistName(playlistSelection))
        }
    }

    /**
     * Add one file to playlist
     */
    private val addFileToPlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(this@FilelistActivity, mPlaylistAdapter!!.getFilename(fileSelection),
                    PlaylistUtils.getPlaylistName(playlistSelection))
        }

    }

    /**
     * Add file list to playlist
     */
    private val addFileListToPlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(this@FilelistActivity, mPlaylistAdapter!!.filenameList,
                    PlaylistUtils.getPlaylistName(playlistSelection))
        }
    }

    override val allFiles: List<String>
        get() = recursiveList(mNavigation!!.currentDir)

    /**
     * For actions based on playlist selection made using choosePlaylist()
     */
    private interface PlaylistChoice {
        fun execute(fileSelection: Int, playlistSelection: Int)
    }

    override fun onItemClick(adapter: PlaylistAdapter, view: View, position: Int) {
        val file = mPlaylistAdapter!!.getFile(position)

        if (mNavigation!!.changeDirectory(file)) {
            mNavigation!!.saveListPosition(modlist_listview)
            updateModlist()
        } else {
            super.onItemClick(adapter, view, position)
        }
    }

    private fun pathNotFound(media_path: String) {

        val alertDialog = AlertDialog.Builder(this).create()

        alertDialog.setTitle("Path not found")
        alertDialog.setMessage("$media_path not found. Create this directory or change the module path.")
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.create)) { _, _ ->
            val ret = Examples.install(this@FilelistActivity, media_path, mPrefs.getBoolean(Preferences.EXAMPLES, true))

            if (ret < 0) {
                Message.error(this@FilelistActivity, "Error creating directory $media_path.")
            }

            mNavigation!!.startNavigation(File(media_path))
            updateModlist()
        }

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel)) { _, _ ->
            finish()
        }

        alertDialog.show()
    }

    private fun readShuffleModePref(): Boolean {
        return mPrefs.getBoolean(OPTIONS_SHUFFLE_MODE, DEFAULT_SHUFFLE_MODE)
    }

    private fun readLoopModePref(): Boolean {
        return mPrefs.getBoolean(OPTIONS_LOOP_MODE, DEFAULT_LOOP_MODE)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.modlist)
        setTitle(R.string.browser_filelist_title)

        // Adapter
        mPlaylistAdapter = PlaylistAdapter(this, ArrayList(), false, PlaylistAdapter.LAYOUT_LIST)
        mPlaylistAdapter!!.setOnItemClickListener(this)

        modlist_listview.apply {
            layoutManager = LinearLayoutManager(this@FilelistActivity)
            adapter = mPlaylistAdapter

            @Suppress("DEPRECATION")
            addItemDecoration(SimpleListDividerDecorator(resources.getDrawable(R.drawable.list_divider), true))
        }

        setSwipeRefresh(swipeContainer)

        // fast scroll
        fast_scroller.attachRecyclerView(modlist_listview)

        registerForContextMenu(modlist_listview)

        val mediaPath = mPrefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH)

        mNavigation = FilelistNavigation()
        mCrossfade = Crossfader(this)
        mCrossfade!!.setup(R.id.modlist_content, R.id.modlist_spinner)

        current_path.apply {
            setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    setTextColor(currentTextColor)
                } else {
                    @Suppress("DEPRECATION")
                    setTextColor(resources.getColor(R.color.pressed_color))
                }
                view.performClick()
                false
            }
        }

        registerForContextMenu(current_path)

        mBackButtonParentdir = mPrefs.getBoolean(Preferences.BACK_BUTTON_NAVIGATION, true)

        // Back/Up button
        up_button.setOnClickListener {
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

    private fun parentDir() {
        if (mNavigation!!.parentDir()) {
            updateModlist()
            mNavigation!!.restoreListPosition(modlist_listview)
        }
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

    override fun onDestroy() {
        super.onDestroy()


        val saveModes = isShuffleMode != readShuffleModePref() || isLoopMode != readLoopModePref()

        if (saveModes) {
            Log.i(TAG, "Save new file list preferences")
            val editor = mPrefs.edit()
            editor.putBoolean(OPTIONS_SHUFFLE_MODE, isShuffleMode)
            editor.putBoolean(OPTIONS_LOOP_MODE, isLoopMode)
            editor.apply()
        }
    }

    public override fun update() {
        updateModlist()
    }

    private fun updateModlist() {
        val modDir = mNavigation!!.currentDir ?: return

        mPlaylistAdapter!!.clear()

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
        mPlaylistAdapter!!.addList(list)
        mPlaylistAdapter!!.notifyDataSetChanged()

        mCrossfade!!.crossfade()
    }

    private fun choosePlaylist(fileSelection: Int, choice: PlaylistChoice) {

        // Return if no playlists exist
        if (PlaylistUtils.list().isEmpty()) {
            Message.toast(this, getString(R.string.msg_no_playlists))
            return
        }

        val playlistSelection = IntArray(1)

        val listener = DialogInterface.OnClickListener { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE && playlistSelection[0] >= 0) {
                choice.execute(fileSelection, playlistSelection[0])
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.msg_select_playlist)
                .setPositiveButton(R.string.ok, listener)
                .setNegativeButton(R.string.cancel, listener)
                .setSingleChoiceItems(PlaylistUtils.listNoSuffix(), 0) { _, which ->
                    playlistSelection[0] = which
                }.show()
    }

    private fun clearCachedEntries(fileList: List<String>) {
        for (filename in fileList) {
            InfoCache.clearCache(filename)
        }
    }


    // Playlist context menu

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo?) {

        if (view == current_path) {
            isPathMenu = true
            menu.setHeaderTitle("All files")
            menu.add(Menu.NONE, 0, 0, "Add to playlist")
            menu.add(Menu.NONE, 1, 1, "Recursive add to playlist")
            menu.add(Menu.NONE, 2, 2, "Add to play queue")
            menu.add(Menu.NONE, 3, 3, "Set as default path")
            menu.add(Menu.NONE, 4, 4, "Clear cache")

            return
        }

        isPathMenu = false

        val position = mPlaylistAdapter!!.position

        if (mPlaylistAdapter!!.getFile(position)!!.isDirectory) {
            // For directory
            menu.setHeaderTitle("This directory")
            menu.add(Menu.NONE, 0, 0, "Add to playlist")
            menu.add(Menu.NONE, 1, 1, "Add to play queue")
            menu.add(Menu.NONE, 2, 2, "Play contents")
            menu.add(Menu.NONE, 3, 3, "Delete directory")
        } else {
            // For files
            val mode = Integer.parseInt(mPrefs.getString(Preferences.PLAYLIST_MODE, "1")!!)

            menu.setHeaderTitle("This file")
            menu.add(Menu.NONE, 0, 0, "Add to playlist")
            if (mode != 3) menu.add(Menu.NONE, 1, 1, "Add to play queue")
            if (mode != 2) menu.add(Menu.NONE, 2, 2, "Play this file")
            if (mode != 1) menu.add(Menu.NONE, 3, 3, "Play all starting here")

            menu.add(Menu.NONE, 4, 4, "Delete file")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {

        if (isPathMenu) {
            when (item.itemId) {
                0 -> choosePlaylist(0, addFileListToPlaylistChoice)     // Add all to playlist
                1 -> choosePlaylist(0, addCurrentRecursiveChoice)       // Add all to queue // Recursive add to playlist
                2 -> addToQueue(mPlaylistAdapter!!.filenameList)                // Add all to queue
                3 -> setDefaultPath()                                           // Set as default path
                4 -> clearCachedEntries(mPlaylistAdapter!!.filenameList)        // Clear cache
            }

            return true
        }

        val position = mPlaylistAdapter!!.position

        if (mPlaylistAdapter!!.getFile(position)!!.isDirectory) {
            // Directories
            when (item.itemId) {
                0 -> choosePlaylist(position, addRecursiveToPlaylistChoice)             // Add to playlist (recursive)
                1 -> addToQueue(recursiveList(mPlaylistAdapter!!.getFile(position)))    // Add to play queue (recursive)
                2 -> playModule(recursiveList(mPlaylistAdapter!!.getFile(position)))    // Play now (recursive)
                3 -> deleteDirectory(position)                                          // delete directory
            }
        } else {
            // Files
            when (item.itemId) {
                0 -> choosePlaylist(position, addFileToPlaylistChoice)      //   Add to playlist
                1 -> addToQueue(mPlaylistAdapter!!.getFilename(position))   //   Add to queue
                2 -> playModule(mPlaylistAdapter!!.getFilename(position))   //   Play this module
                3 -> playModule(mPlaylistAdapter!!.filenameList, position)  //   Play all starting here
                4 -> deleteFile(position)                                   //   Delete file
            }
        }

        return true
    }

    private fun setDefaultPath() {
        val editor = mPrefs.edit()
        editor.putString(Preferences.MEDIA_PATH, mNavigation!!.currentDir!!.path)
        editor.apply()
        Message.toast(this, "Set as default module path")
    }

    private fun deleteDirectory(position: Int) {
        val deleteName = mPlaylistAdapter!!.getFilename(position)
        val mediaPath = mPrefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH)

        if (deleteName.startsWith(mediaPath!!) && deleteName != mediaPath) {
            Message.yesNoDialog(this, "Delete directory", "Are you sure you want to delete directory \"" +
                    FileUtils.basename(deleteName) + "\" and all its contents?", Runnable {
                if (InfoCache.deleteRecursive(deleteName)) {
                    updateModlist()
                    Message.toast(this@FilelistActivity, getString(R.string.msg_dir_deleted))
                } else {
                    Message.toast(this@FilelistActivity, getString(R.string.msg_cant_delete_dir))
                }
            })
        } else {
            Message.toast(this, R.string.error_dir_not_under_moddir)
        }
    }

    private fun deleteFile(position: Int) {
        val deleteName = mPlaylistAdapter!!.getFilename(position)
        Message.yesNoDialog(this, "Delete", "Are you sure you want to delete " + FileUtils.basename(deleteName) + "?", Runnable {
            if (InfoCache.delete(deleteName)) {
                updateModlist()
                Message.toast(this, getString(R.string.msg_file_deleted))
            } else {
                Message.toast(this, getString(R.string.msg_cant_delete))
            }
        })
    }


    companion object {
        private const val TAG = "BasePlaylistActivity"
        private const val OPTIONS_SHUFFLE_MODE = "options_shuffleMode"
        private const val OPTIONS_LOOP_MODE = "options_loopMode"
        private const val DEFAULT_SHUFFLE_MODE = true
        private const val DEFAULT_LOOP_MODE = false

        private fun recursiveList(file: File?): List<String> {
            val list = ArrayList<String>()

            if (file == null) {
                return list
            }

            //TODO introduce Kotlin's recursive
            if (file.isDirectory) {
                val fileArray = file.listFiles()

                if (fileArray != null) {            // prevent crash reported in dev console
                    for (f in fileArray) {
                        if (f.isDirectory) {
                            list.addAll(recursiveList(f))
                        } else {
                            list.add(f.path)
                        }
                    }
                }
            } else {
                list.add(file.path)
            }

            return list
        }
    }
}
