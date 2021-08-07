package org.helllabs.android.xmp.ui.playlist_list

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.*
import android.os.*
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import java.util.*
import kotlinx.coroutines.flow.collect
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivityPlaylistMenuBinding
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.BasePlaylistAdapter
import org.helllabs.android.xmp.ui.PlaylistLayoutType
import org.helllabs.android.xmp.ui.browser.FilelistActivity
import org.helllabs.android.xmp.ui.modarchive.Search
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.playlist_detail.PlaylistActivity
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.preferences.Preferences
import org.helllabs.android.xmp.ui.util.dialogMessage
import org.helllabs.android.xmp.ui.util.showChangeLog
import org.helllabs.android.xmp.ui.util.toast
import org.helllabs.android.xmp.util.*

class PlaylistMenu : AppCompatActivity() {

    val viewModel: PlaylistMenuViewModel by viewModels()

    private lateinit var playlistAdapter: BasePlaylistAdapter

    private var resultPermissions = registerForActivityResult(RequestMultiplePermissions()) {
        if (it[WRITE_EXTERNAL_STORAGE] == true && it[READ_EXTERNAL_STORAGE] == true) {
            logD("Perms Granted: ${it.entries}")
            showChangeLog(this) {
                val name = getString(R.string.empty_playlist)
                val comment = getString(R.string.empty_comment)
                when (viewModel.setupDataDir(name, comment)) {
                    0 -> Unit // Success
                    -1 -> dialogMessage(
                        lifecycleOwner = this,
                        message = getString(R.string.error_create_playlist),
                    )
                    -2 -> dialogMessage(
                        lifecycleOwner = this,
                        title = R.string.error,
                        message = getString(R.string.error_datadir),
                        block = { finish() }
                    )
                }
            }
        } else {
            logW("Perms Not-Granted: ${it.entries}")
            dialogMessage(
                lifecycleOwner = this,
                message = "Permissions Not Granted...",
                block = { finish() }
            )
        }
    }

    private var resultAdd = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            if (it.data == null) {
                toast("Couldn't add playlist")
                return@registerForActivityResult
            }
            val data = it.data!!

            val name = data.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!
            val comment = data.getStringExtra(PlaylistAddEdit.EXTRA_COMMENT)!!
            if (!viewModel.addPlaylist(name, comment)) {
                dialogMessage(
                    lifecycleOwner = this,
                    message = getString(R.string.error_create_playlist),
                )
            }
        }
    }

    private var resultEdit = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            if (it.data == null) {
                toast(R.string.msg_edit_playlist_failed)
                return@registerForActivityResult
            }
            val data = it.data!!

            val id = data.getIntExtra(PlaylistAddEdit.EXTRA_ID, -1)
            val name = data.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!
            val comment = data.getStringExtra(PlaylistAddEdit.EXTRA_COMMENT)!!
            val oldName = data.getStringExtra(PlaylistAddEdit.EXTRA_OLD_NAME)

            when (viewModel.editPlaylist(id, name, comment, oldName)) {
                0 -> Unit // Success
                -1 -> dialogMessage(
                    lifecycleOwner = this,
                    message = getString(R.string.error_rename_playlist),
                )
                -2 -> dialogMessage(
                    lifecycleOwner = this,
                    message = getString(R.string.error_edit_comment),
                )
            }
        }
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

        // Playlist adapter
        playlistAdapter = BasePlaylistAdapter(PlaylistLayoutType.TYPE_CARD, false).apply {
            onClick = { position -> onClick(position) }
            onLongClick = { position -> onLongClick(position) }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.playlistState.collect {
                when (it) {
                    PlaylistMenuViewModel.PlaylistMenuState.None -> Unit
                    PlaylistMenuViewModel.PlaylistMenuState.Load -> onLoad()
                    is PlaylistMenuViewModel.PlaylistMenuState.Loaded -> onLoaded(it.list)
                }
            }
        }

        with(binder) {
            // AppBar
            appbar.toolbarText.apply {
                text = spannable
                click { startPlayerActivity() }
            }
            // Swipe refresh
            swipeContainer.apply {
                setColorSchemeResources(R.color.refresh_color)
                setOnRefreshListener {
                    viewModel.updateList()
                    isRefreshing = false
                }
            }
            // RecyclerView
            plistMenuList.apply {
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
            fab.click {
                val intent = Intent(this@PlaylistMenu, PlaylistAddEdit::class.java)
                resultAdd.launch(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!Preferences.checkStorage()) {
            dialogMessage(
                lifecycleOwner = this,
                message = getString(R.string.error_storage),
                block = { finish() }
            )
        }

        val permissions = arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
        resultPermissions.launch(permissions)

        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            startPlayerActivity()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)

        // Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                startPlayerActivity()
                return true
            }
            R.id.menu_prefs -> {
                val intent = Intent(this, Preferences::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return true
            }
            R.id.menu_download -> {
                val intent = Intent(this, Search::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Is this needed anymore? We do terminate the service if swiped away from recents.
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // If we launch from launcher and we're playing a module, go straight to the player activity
        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            startPlayerActivity()
        }
    }

    private fun onClick(position: Int) {
        val intent: Intent =
            if (position == 0) {
                Intent(this@PlaylistMenu, FilelistActivity::class.java)
            } else {
                Intent(this@PlaylistMenu, PlaylistActivity::class.java).apply {
                    putExtra("name", playlistAdapter.currentList[position].name)
                }
            }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
            }
            resultEdit.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun onLoad() {
    }

    private fun onLoaded(list: List<PlaylistItem>) {
        logD("Updating List")
        playlistAdapter.submitList(list)
    }

    private fun startPlayerActivity() {
        if (PrefManager.startOnPlayer) {
            if (PlayerService.isPlayerAlive.value == true) {
                val playerIntent = Intent(this, PlayerActivity::class.java)
                startActivity(playerIntent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun changeDir() {
        val mediaPath = PrefManager.mediaPath
        MaterialDialog(this).show {
            lifecycleOwner(this@PlaylistMenu)
            title(R.string.dialog_change_dir_title)
            message(R.string.dialog_change_dir_msg)
            input(
                prefill = mediaPath,
                waitForPositiveButton = true,
                allowEmpty = false
            ) { _, text ->
                if (text != mediaPath) {
                    PrefManager.mediaPath = text.toString()
                    viewModel.updateList()
                }
            }
            negativeButton(R.string.cancel)
        }
    }
}
