package org.helllabs.android.xmp.browser

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import java.io.File
import java.text.DateFormat
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter.Companion.LAYOUT_LIST
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistItem.Companion.TYPE_DIRECTORY
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.FileUtils.basename
import org.helllabs.android.xmp.util.InfoCache.clearCache
import org.helllabs.android.xmp.util.InfoCache.delete
import org.helllabs.android.xmp.util.InfoCache.deleteRecursive

// TODO: Replace current path with bread crumb trails
// TODO: Implement MVI/Coroutines, dir parsing slow.
class FilelistActivity : BasePlaylistActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var mCrossfade: Crossfader
    private lateinit var curPath: TextView
    private lateinit var mNavigation: FilelistNavigation

    override var isLoopMode = false
    override var isShuffleMode = false
    override val allFiles: List<String>
        get() = recursiveList(mNavigation.currentDir)

    // region [REGION] PlaylistChoice
    /**
     * For actions based on playlist selection made using choosePlaylist()
     */
    private interface PlaylistChoice {
        fun execute(fileSelection: Int, playlistSelection: Int)
    }

    /**
     * Recursively add current directory to playlist
     */
    private val addCurrentRecursiveChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                recursiveList(mNavigation.currentDir),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Recursively add directory to playlist
     */
    private val addRecursiveToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                recursiveList(mPlaylistAdapter.getFile(fileSelection)),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add one file to playlist
     */
    private val addFileToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                mPlaylistAdapter.getFilename(fileSelection),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add file list to playlist
     */
    private val addFileListToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                mPlaylistAdapter.filenameList,
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_modlist)
        findViewById<TextView>(R.id.toolbarText).text = getString(R.string.browser_filelist_title)

        mNavigation = FilelistNavigation()

        mCrossfade = Crossfader(this)
        mCrossfade.setup(R.id.modlist_content, R.id.modlist_spinner)

        mPlaylistAdapter = PlaylistAdapter(LAYOUT_LIST, false)
        mPlaylistAdapter.onClick = { position -> onClick(position) }
        mPlaylistAdapter.onLongClick = { position -> onLongClick(position) }

        recyclerView = findViewById<RecyclerView>(R.id.modlist_listview).apply {
            adapter = mPlaylistAdapter
            setHasFixedSize(true)
            addItemDecoration(
                DividerItemDecoration(this@FilelistActivity, LinearLayoutManager.HORIZONTAL)
            )
        }

        findViewById<ImageButton>(R.id.up_button).click { parentDir() }

        curPath = findViewById<TextView>(R.id.current_path).apply {
            val textColor = currentTextColor
            setOnTouchListener { view, event ->
                when (event?.action) {
                    MotionEvent.ACTION_UP -> setTextColor(textColor)
                    else -> setTextColor(resources.color(R.color.pressed_color))
                }
                view?.performClick() ?: false
            }
            longClick {
                onPathClick()
                true
            }
        }

        // Check if directory exists
        val modDir = File(PrefManager.mediaPath)
        if (modDir.isDirectory) {
            mNavigation.startNavigation(modDir)
            updateModlist()
        } else {
            pathNotFound(PrefManager.mediaPath)
        }

        isShuffleMode = readShuffleModePref()
        isLoopMode = readLoopModePref()

        // Swipe
        setSwipeRefresh(recyclerView)

        // Play buttons
        setupButtons()
    }

    public override fun onDestroy() {
        super.onDestroy()

        var saveModes = false
        if (isShuffleMode != readShuffleModePref() || isLoopMode != readLoopModePref())
            saveModes = true

        if (saveModes) {
            logI("Save new file list preferences")
            PrefManager.setBooleanPref(OPTIONS_SHUFFLE_MODE, isShuffleMode)
            PrefManager.setBooleanPref(OPTIONS_LOOP_MODE, isLoopMode)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mNavigation.isAtTopDir) {
                parentDir()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun onClick(position: Int) {
        val file = mPlaylistAdapter.getFile(position)
        if (mNavigation.changeDirectory(file)) {
            mNavigation.saveListPosition(recyclerView)
            updateModlist()
        } else {
            onItemClick(mPlaylistAdapter, position)
        }
    }

    private fun onLongClick(position: Int) {
        val item = mPlaylistAdapter.getFile(position)
        if (item.isDirectory) {
            val items = listOf(
                "Add to playlist",
                "Add to play queue",
                "Play contents",
                "Delete directory"
            )

            MaterialDialog(this).show {
                title(text = "This directory")
                listItemsSingleChoice(items = items) { _, index, _ ->
                    when (index) {
                        0 -> choosePlaylist(position, addRecursiveToPlaylistChoice)
                        1 -> addToQueue(recursiveList(mPlaylistAdapter.getFile(position)))
                        2 -> playModule(recursiveList(mPlaylistAdapter.getFile(position)))
                        3 -> deleteDirectory(position)
                    }
                }
                positiveButton(R.string.select)
            }
        } else {
            val items = listOf(
                "Add to playlist",
                "Add to play queue",
                "Play this file",
                "Play all starting here",
                "Delete file"
            )

            MaterialDialog(this).show {
                title(text = "This file")
                listItemsSingleChoice(items = items) { _, index, _ ->
                    when (index) {
                        0 -> choosePlaylist(position, addFileToPlaylistChoice)
                        1 -> addToQueue(mPlaylistAdapter.getFilename(position))
                        2 -> playModule(mPlaylistAdapter.getFilename(position))
                        3 -> playModule(mPlaylistAdapter.filenameList, position)
                        4 -> {
                            val deleteName = mPlaylistAdapter.getFilename(position)
                            yesNoDialog(
                                "Delete",
                                "Are you sure you want to delete ${basename(deleteName)}?"
                            ) {
                                if (delete(deleteName)) {
                                    updateModlist()
                                    toast(R.string.msg_file_deleted)
                                } else {
                                    toast(R.string.msg_cant_delete)
                                }
                            }
                        }
                    }
                }
                positiveButton(R.string.select)
            }
        }
    }

    private fun onPathClick() {
        val items = listOf(
            "Add to playlist",
            "Recursive add to playlist",
            "Add to play queue",
            "Set as default path",
            "Clear cache"
        )

        MaterialDialog(this).show {
            title(text = "All files")
            listItemsSingleChoice(items = items) { _, index, _ ->
                when (index) {
                    0 -> choosePlaylist(0, addFileListToPlaylistChoice)
                    1 -> choosePlaylist(0, addCurrentRecursiveChoice)
                    2 -> addToQueue(mPlaylistAdapter.filenameList)
                    3 -> {
                        PrefManager.mediaPath = mNavigation.currentDir!!.path
                        toast("Set as default module path")
                    }
                    4 -> clearCachedEntries(mPlaylistAdapter.filenameList)
                }
            }
            positiveButton(R.string.select)
        }
    }

    override fun update() {
        updateModlist()
    }

    // TODO: Dialog
    private fun pathNotFound(mediaPath: String) {
        AlertDialog.Builder(this).create().apply {
            setTitle("Path not found")
            setMessage(
                "$mediaPath not found. Create this directory or change the module path."
            )
            setButton(
                AlertDialog.BUTTON_POSITIVE,
                getString(R.string.create)
            ) { _: DialogInterface?, _: Int ->
                val ret = installAssets(mediaPath, PrefManager.installExamples)
                if (ret < 0) {
                    generalError("Error creating directory $mediaPath.")
                }
                mNavigation.startNavigation(File(mediaPath))
                updateModlist()
            }
            setButton(
                AlertDialog.BUTTON_NEGATIVE,
                getString(R.string.cancel)
            ) { _: DialogInterface?, _: Int ->
                finish()
            }
        }.show()
    }

    private fun readShuffleModePref(): Boolean {
        return PrefManager.getBooleanPref(OPTIONS_SHUFFLE_MODE, DEFAULT_SHUFFLE_MODE)
    }

    private fun readLoopModePref(): Boolean {
        return PrefManager.getBooleanPref(OPTIONS_LOOP_MODE, DEFAULT_LOOP_MODE)
    }

    private fun parentDir() {
        if (mNavigation.parentDir()) {
            updateModlist()
            mNavigation.restoreListPosition(recyclerView)
        }
    }

    private fun updateModlist() {
        mPlaylistAdapter.onSwap(null) // Stop flicker

        val modDir = mNavigation.currentDir ?: return
        curPath.text = modDir.path
        val list = mutableListOf<PlaylistItem>()

        modDir.listFiles()?.forEach { file ->
            val item: PlaylistItem
            item = if (file.isDirectory) {
                PlaylistItem(TYPE_DIRECTORY, file.name, getString(R.string.directory))
            } else {
                val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                    .format(file.lastModified())
                val comment = date + String.format(" (%d kB)", file.length() / 1024)
                PlaylistItem(PlaylistItem.TYPE_FILE, file.name, comment)
            }
            item.file = file
            list.add(item)
        }

        list.sort()
        PlaylistUtils.renumberIds(list)
        mPlaylistAdapter.onSwap(list)
        mCrossfade.crossfade()

        if (list.isEmpty()) {
            findViewById<TextView>(R.id.empty_message).show()
        } else {
            findViewById<TextView>(R.id.empty_message).hide()
        }
    }

    private fun deleteDirectory(position: Int) {
        val deleteName = mPlaylistAdapter.getFilename(position)
        val mediaPath = PrefManager.mediaPath
        if (deleteName.startsWith(mediaPath) && deleteName != mediaPath) {
            yesNoDialog(
                "Delete directory",
                "Are you sure you want to delete directory" +
                    " \"${basename(deleteName)}\" and all its contents?"
            ) {
                if (deleteRecursive(deleteName)) {
                    updateModlist()
                    toast(getString(R.string.msg_dir_deleted))
                } else {
                    toast(getString(R.string.msg_cant_delete_dir))
                }
            }
        } else {
            toast(R.string.error_dir_not_under_moddir)
        }
    }

    private fun choosePlaylist(fileSelection: Int, choice: PlaylistChoice) {

        // Return if no playlists exist
        if (PlaylistUtils.list().isEmpty()) {
            toast(getString(R.string.msg_no_playlists))
            return
        }

        val playlistSelection = IntArray(1)
        val listener = DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
            if (which == DialogInterface.BUTTON_POSITIVE && playlistSelection[0] >= 0) {
                choice.execute(fileSelection, playlistSelection[0])
            }
        }

        AlertDialog.Builder(this).apply {
            setTitle(R.string.msg_select_playlist)
            setPositiveButton(R.string.ok, listener)
            setNegativeButton(R.string.cancel, listener)
            setSingleChoiceItems(
                PlaylistUtils.listNoSuffix(),
                0
            ) { _: DialogInterface?, which: Int ->
                playlistSelection[0] = which
            }
        }.show()
    }

    private fun clearCachedEntries(fileList: List<String>) {
        fileList.forEach {
            clearCache(it)
        }
    }

    private fun recursiveList(file: File?): List<String> {
        val list = mutableListOf<String>()

        if (file == null) {
            return list
        }

        file.walkTopDown().forEach {
            if (it.isFile)
                list.add(it.path)
        }

        return list
    }

    companion object {
        private const val OPTIONS_SHUFFLE_MODE = "options_shuffleMode"
        private const val OPTIONS_LOOP_MODE = "options_loopMode"
        private const val DEFAULT_SHUFFLE_MODE = true
        private const val DEFAULT_LOOP_MODE = false
    }
}
