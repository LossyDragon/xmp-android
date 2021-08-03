package org.helllabs.android.xmp.ui.playlist_list

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import java.io.File
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivityPlaylistMenuBinding
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.BasePlaylistAdapter
import org.helllabs.android.xmp.ui.PlaylistLayoutType
import org.helllabs.android.xmp.ui.browser.FilelistActivity
import org.helllabs.android.xmp.ui.modarchive.Search
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.playlist_detail.PlaylistActivity
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.preferences.Preferences
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.PlaylistUtils
import org.helllabs.android.xmp.util.PlaylistUtils.createEmptyPlaylist

class PlaylistMenu : AppCompatActivity() {

    private lateinit var playlistAdapter: BasePlaylistAdapter
    private lateinit var mediaPath: String

    private var resultAdd = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == RESULT_OK)
            addPlaylist(it.data)
    }

    private var resultEdit = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == RESULT_OK)
            editPlaylist(it.data)
    }

    private var resultUpdate = registerForActivityResult(StartActivityForResult()) {
        updateList()
    }

    private lateinit var binder: ActivityPlaylistMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivityPlaylistMenuBinding.inflate(layoutInflater)

        logI("Start application")
        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        // Appbar text tomfoolery
        val spannable = SpannableString(getString(R.string.app_name))
        val color = resources.color(R.color.accent)
        spannable.setSpan(ForegroundColorSpan(color), 0, 3, SPAN_EXCLUSIVE_EXCLUSIVE)
        binder.appbar.toolbarText.apply {
            text = spannable
            click { startPlayerActivity() }
        }

        // Swipe refresh
        binder.swipeContainer.apply {
            setColorSchemeResources(R.color.refresh_color)
            setOnRefreshListener {
                updateList()
                isRefreshing = false
            }
        }

        // Playlist adapter
        playlistAdapter = BasePlaylistAdapter(PlaylistLayoutType.TYPE_CARD, false)
        playlistAdapter.onClick = { position -> onClick(position) }
        playlistAdapter.onLongClick = { position -> onLongClick(position) }

        binder.plistMenuList.apply {
            adapter = playlistAdapter
            setOnItemTouchListener(
                onInterceptTouchEvent = { _, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        var enable = false
                        if (childCount > 0) {
                            enable = !canScrollVertically(-1)
                        }
                        binder.swipeContainer.isEnabled = enable
                    }
                    false
                }
            )
        }

        // FAB
        binder.fab.click {
            resultAdd.launch(Intent(this, PlaylistAddEdit::class.java))
        }

        if (!Preferences.checkStorage()) {
            dialogMessage(
                lifecycleOwner = this,
                message = getString(R.string.error_storage),
                block = { finish() }
            )
        }

        if (Api.isAtLeastM) {
            val hasPermission =
                ContextCompat.checkSelfPermission(
                    this,
                    WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                setupDataDir()
                updateList()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_STORAGE
                )
            }
        } else {
            setupDataDir()
        }

        // Show Changelog
        showChangeLog(this)

        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            startPlayerActivity()
        }
    }

    public override fun onResume() {
        super.onResume()
        playlistAdapter.submitList(null) // Stop flicker
        updateList()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupDataDir()
                updateList()
            }
        }
    }

    // Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)

        // Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> startPlayerActivity()
            R.id.menu_prefs -> {
                resultUpdate.launch(Intent(this, Preferences::class.java))
            }
            R.id.menu_download -> {
                val intent = Intent(this, Search::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onNewIntent(intent: Intent) {
        // If we launch from launcher and we're playing a module, go straight to the player activity
        super.onNewIntent(intent)
        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            startPlayerActivity()
        }
    }

    private fun onClick(position: Int) {
        val intent: Intent
        if (position == 0) {
            intent = Intent(this@PlaylistMenu, FilelistActivity::class.java)
        } else {
            intent = Intent(this@PlaylistMenu, PlaylistActivity::class.java)
            intent.putExtra("name", playlistAdapter.currentList[position].name)
        }
        resultUpdate.launch(intent)
    }

    private fun onLongClick(position: Int) {
        if (position == 0) {
            changeDir()
        } else {
            val playlist = playlistAdapter.currentList[position]
            val intent = Intent(this, PlaylistAddEdit::class.java).apply {
                putExtra(PlaylistAddEdit.EXTRA_ID, playlist.id)
                putExtra(PlaylistAddEdit.EXTRA_NAME, playlist.name)
                putExtra(PlaylistAddEdit.EXTRA_COMMENT, playlist.comment)
                putExtra(PlaylistAddEdit.EXTRA_TYPE, playlist.type)
            }
            resultEdit.launch(intent)
        }
    }

    // Create application directory and populate with empty playlist
    private fun setupDataDir() {
        if (!Preferences.DATA_DIR.isDirectory) {
            if (Preferences.DATA_DIR.mkdirs()) {
                createEmptyPlaylist(
                    this,
                    this,
                    getString(R.string.empty_playlist),
                    getString(R.string.empty_comment)
                )
            } else {
                dialogMessage(
                    lifecycleOwner = this,
                    title = R.string.error,
                    message = getString(R.string.error_datadir),
                    block = { finish() }
                )
            }
        }
    }

    private fun startPlayerActivity() {
        if (PrefManager.startOnPlayer) {
            if (PlayerService.isPlayerAlive.value == true) {
                val playerIntent = Intent(this, PlayerActivity::class.java)
                startActivity(playerIntent)
            }
        }
    }

    private fun updateList() {
        playlistAdapter.submitList(null) // Stop Flicker

        val list = mutableListOf<PlaylistItem>()
        mediaPath = PrefManager.mediaPath
        val browserItem = PlaylistItem(
            PlaylistType.TYPE_SPECIAL,
            getString(R.string.playlist_special_title),
            getString(R.string.playlist_special_comment, mediaPath)
        )
        list.add(browserItem)

        PlaylistUtils.listNoSuffix().forEach { name ->
            val item = PlaylistItem(
                PlaylistType.TYPE_PLAYLIST,
                name,
                PlaylistUtils.readComment(this, this, name)
            )
            list.add(item)
        }

        PlaylistUtils.renumberIds(playlistAdapter.getItems())
        playlistAdapter.submitList(list)
    }

    private fun addPlaylist(data: Intent?) {
        if (data == null) {
            toast("Couldn't add playlist")
            return
        }

        val name = data.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!
        val comment = data.getStringExtra(PlaylistAddEdit.EXTRA_COMMENT)!!
        if (!createEmptyPlaylist(this, this, name, comment)) {
            dialogMessage(
                lifecycleOwner = this,
                message = getString(R.string.error_create_playlist),
            )
        }

        updateList()
    }

    private fun editPlaylist(data: Intent?) {

        if (data == null) {
            toast(R.string.msg_edit_playlist_failed)
            return
        }

        val id = data.getIntExtra(PlaylistAddEdit.EXTRA_ID, -1)
        val name = data.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!
        val comment = data.getStringExtra(PlaylistAddEdit.EXTRA_COMMENT)!!
        val oldName = data.getStringExtra(PlaylistAddEdit.EXTRA_OLD_NAME)

        when (id) {
            PlaylistAddEdit.RESULT_DELETE_PLAYLIST -> PlaylistUtils.delete(name)
            PlaylistAddEdit.RESULT_EDIT_PLAYLIST -> {
                if (!PlaylistUtils.rename(oldName!!, name)) {
                    dialogMessage(
                        lifecycleOwner = this,
                        message = getString(R.string.error_rename_playlist),
                    )
                    return // Don't attempt to edit comment if failed.
                }

                val file = File(Preferences.DATA_DIR, name + PlaylistUtils.COMMENT_SUFFIX)
                if (!PlaylistUtils.editComment(file, comment)) {
                    dialogMessage(
                        lifecycleOwner = this,
                        message = getString(R.string.error_edit_comment),
                    )
                }
            }
            else -> throw IllegalArgumentException("Edit playlist id was not correct: $id")
        }

        updateList()
    }

    @SuppressLint("CheckResult")
    private fun changeDir() {
        MaterialDialog(this).show {
            title(R.string.dialog_change_dir_title)
            message(R.string.dialog_change_dir_msg)
            input(
                prefill = mediaPath,
                waitForPositiveButton = true,
                allowEmpty = false
            ) { _, text ->
                if (mediaPath != mediaPath) {
                    PrefManager.mediaPath = text.toString()
                    updateList()
                }
            }
            negativeButton(R.string.cancel)
        }
    }

    companion object {
        private const val REQUEST_WRITE_STORAGE = 112
    }
}
