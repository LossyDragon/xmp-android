package org.helllabs.android.xmp.ui.browser

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.databinding.LayoutListControlsBinding
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.ui.modarchive.Search
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.preferences.Preferences
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.InfoCache.testModule
import org.helllabs.android.xmp.util.InfoCache.testModuleForceIfInvalid

abstract class BasePlaylistActivity : AppCompatActivity() {

    private lateinit var mModPlayer: PlayerService
    private var mShowToasts = false
    private var mAddList: MutableList<String>? = null

    protected lateinit var mPlaylistAdapter: PlaylistAdapter
    protected abstract var isShuffleMode: Boolean
    protected abstract var isLoopMode: Boolean
    protected abstract val allFiles: List<String>
    protected abstract fun update()

    private val shuffleIcon
        get() = if (isShuffleMode) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off
    private val loopIcon
        get() = if (isLoopMode) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off

    // Connection
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.PlayerBinder
            mModPlayer = binder.getService()
            mModPlayer.add(mAddList!!.toList())
            unbindService(this)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            logW("Service unexpectedly disconnected")
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mShowToasts = PrefManager.showToast
    }

    // Let the menu's inflate after the layout's inflated.
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    public override fun onResume() {
        super.onResume()
        update()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        logI("Activity result $requestCode,$resultCode")
        when (requestCode) {
            SETTINGS_REQUEST -> mShowToasts = PrefManager.showToast
            PLAY_MOD_REQUEST -> if (resultCode != RESULT_OK) update()
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
            android.R.id.home -> {
                val intent = Intent(this, PlaylistMenu::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                return true
            }
            R.id.menu_prefs -> {
                val intent = Intent(this, Preferences::class.java)
                startActivityForResult(intent, SETTINGS_REQUEST)
            }
            R.id.menu_download -> {
                val intent = Intent(this, Search::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    open fun onItemClick(adapter: PlaylistAdapter, position: Int) {
        val filename = adapter.currentList[position].file!!.path
        val mode = PrefManager.playlistMode.toInt()

        /* Test module again if invalid, in case a new file format is added to the
         * player library and the file was previously unrecognized and cached as invalid.
         */
        if (testModuleForceIfInvalid(filename)) {
            when (mode) {
                1 -> {
                    val count = position - adapter.directoryCount
                    if (count >= 0) {
                        playModule(adapter.filenameList, count, isShuffleMode)
                    }
                }
                2 -> playModule(filename)
                3 -> {
                    addToQueue(filename)
                    toast(R.string.msg_queue_added)
                }
            }
        } else {
            toast(R.string.msg_file_unrecognized)
        }
    }

    protected fun setSwipeRefresh(swipe: SwipeRefreshLayout, recyclerView: RecyclerView) {
        swipe.apply {
            setOnRefreshListener {
                update()
                isRefreshing = false
            }
            setColorSchemeResources(R.color.refresh_color)
        }

        recyclerView.setOnItemTouchListener(
            onInterceptTouchEvent = { _, e ->
                if (e.action == MotionEvent.ACTION_DOWN) {
                    var enable = false
                    if (recyclerView.childCount > 0) {
                        enable = !recyclerView.canScrollVertically(-1)
                    }
                    swipe.isEnabled = enable
                }
                false
            }
        )
    }

    protected fun setupButtons(controls: LayoutListControlsBinding) {
        controls.controlButtonPlay.click {
            if (allFiles.isEmpty()) {
                toast(R.string.error_no_files_to_play)
            } else {
                playModule(allFiles)
            }
        }
        controls.controlButtonLoop.apply {
            setImageResource(loopIcon)
            click {
                isLoopMode = !isLoopMode
                setImageResource(loopIcon)
                if (mShowToasts)
                    toast(if (isLoopMode) R.string.msg_loop_on else R.string.msg_loop_off)
            }
        }
        controls.controlButtonShuffle.apply {
            setImageResource(shuffleIcon)
            click {
                isShuffleMode = !isShuffleMode
                setImageResource(shuffleIcon)
                if (mShowToasts)
                    toast(if (isShuffleMode) R.string.msg_shuffle_on else R.string.msg_shuffle_off)
            }
        }
    }

    // Play this module
    protected fun playModule(mod: String) {
        playModule(listOf(mod), 0, false)
    }

    // Play all modules in list and honor default shuffle mode
    protected fun playModule(modList: List<String>) {
        playModule(modList, 0, false)
    }

    protected fun playModule(modList: List<String>, start: Int) {
        playModule(modList, start, false)
    }

    private fun playModule(modList: List<String>, start: Int, keepFirst: Boolean) {
        XmpApplication.fileList = modList
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.PARM_SHUFFLE, isShuffleMode)
            putExtra(PlayerActivity.PARM_LOOP, isLoopMode)
            putExtra(PlayerActivity.PARM_START, start)
            putExtra(PlayerActivity.PARM_KEEPFIRST, keepFirst)
        }

        logI("Start Player activity")
        startActivityForResult(intent, PLAY_MOD_REQUEST)
    }

    protected fun addToQueue(filename: String) {
        if (testModule(filename)) {
            if (PlayerService.isPlayerAlive.value == true) {
                mAddList = ArrayList()
                mAddList!!.add(filename)
                val service = Intent(this, PlayerService::class.java)
                bindService(service, connection, BIND_AUTO_CREATE)
            } else {
                playModule(filename)
            }
        }
    }

    protected fun addToQueue(list: List<String>) {
        val realList = mutableListOf<String>()
        val invalid = mutableListOf<String>()

        if (list.isEmpty()) {
            toast(R.string.msg_queue_empty)
            logW("Queue list empty")
            return
        }

        list.forEach {
            if (testModule(it)) {
                realList.add(it)
            } else {
                invalid.add(it)
            }
        }

        if (invalid.isNotEmpty()) {
            toast(R.string.msg_only_valid_files_sent)
            logW("addToQueue() invalid items: $invalid")
        }

        if (realList.isNotEmpty()) {
            if (PlayerService.isPlayerAlive.value == true) {
                val service = Intent(this, PlayerService::class.java)
                mAddList = realList
                bindService(service, connection, BIND_AUTO_CREATE)
            } else {
                playModule(realList)
            }
        }
    }

    companion object {
        private const val SETTINGS_REQUEST = 45
        private const val PLAY_MOD_REQUEST = 669
    }
}
