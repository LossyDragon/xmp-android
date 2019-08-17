package org.helllabs.android.xmp.browser

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.layout_list_controls.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.InfoCache
import org.helllabs.android.xmp.util.Log
import org.helllabs.android.xmp.util.Message.toast
import java.util.*

abstract class BasePlaylistActivity : AppCompatActivity() {
    private var mShowToasts: Boolean = false
    private var mModPlayer: ModInterface? = null
    private var mAddList: MutableList<String>? = null
    protected lateinit var mPrefs: SharedPreferences
    protected var mPlaylistAdapter: PlaylistAdapter? = null
    private var refresh: Boolean = false

    private val playAllButtonListener = OnClickListener {
        val list = allFiles
        if (list.isEmpty()) {
            toast(this@BasePlaylistActivity, R.string.error_no_files_to_play)
        } else {
            playModule(list)
        }
    }

    private val toggleLoopButtonListener = OnClickListener { view ->
        var loopMode = isLoopMode
        loopMode = loopMode xor true
        (view as ImageButton).setImageResource(if (loopMode) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off)

        if (mShowToasts) {
            toast(view.getContext(), if (loopMode) R.string.msg_loop_on else R.string.msg_loop_off)
        }

        if (mShowToasts)
            toast(view.context, if (loopMode) R.string.msg_loop_on else R.string.msg_loop_off)

        isLoopMode = loopMode
    }

    private val toggleShuffleButtonListener = OnClickListener { view ->
        var shuffleMode = isShuffleMode
        shuffleMode = shuffleMode xor true
        (view as ImageButton).setImageResource(if (shuffleMode) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)
        if (mShowToasts) {
            toast(view.getContext(), if (shuffleMode) R.string.msg_shuffle_on else R.string.msg_shuffle_off)
        }

        isShuffleMode = shuffleMode
    }

    // Connection
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mModPlayer = ModInterface.Stub.asInterface(service)
            try {
                mModPlayer!!.add(mAddList)
            } catch (e: RemoteException) {
                toast(this@BasePlaylistActivity, R.string.error_adding_mod)
            }

            unbindService(this)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mModPlayer = null
        }
    }

    protected abstract var isShuffleMode: Boolean

    protected abstract var isLoopMode: Boolean

    protected abstract val allFiles: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        mShowToasts = mPrefs.getBoolean(Preferences.SHOW_TOAST, true)

        // Action bar icon navigation
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        if (refresh) {
            update()
        }

        // Change the nav bar color to the list controls sheet color
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            @Suppress("DEPRECATION")
//            window.navigationBarColor = resources.getColor(R.color.section_background)
//        }
    }

    protected abstract fun update()

    protected fun setSwipeRefresh(swipeContainer: SwipeRefreshLayout) {
        swipeContainer.apply {
            setColorSchemeResources(R.color.refresh_color)
            setOnRefreshListener {
                update()
                this.isRefreshing = false
            }
        }
    }

    protected fun setupButtons() {
        control_button_play.setImageResource(R.drawable.ic_play)
        control_button_play.setOnClickListener(playAllButtonListener)

        control_button_loop.setImageResource(if (isLoopMode) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off)
        control_button_loop.setOnClickListener(toggleLoopButtonListener)

        control_button_shuffle.setImageResource(if (isShuffleMode) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)
        control_button_shuffle.setOnClickListener(toggleShuffleButtonListener)
    }

    open fun onItemClick(adapter: PlaylistAdapter, view: View, position: Int) {
        val filename = adapter.getItem(position).file!!.path

        val mode = Integer.parseInt(mPrefs.getString(Preferences.PLAYLIST_MODE, "1")!!)

        /*
         * Test module again if invalid, in case a new file format is added to the
		 * player library and the file was previously unrecognized and cached as invalid.
		 */
        if (InfoCache.testModuleForceIfInvalid(filename)) {
            when (mode) {
                // play all starting at this one
                1 -> {
                    val count = position - adapter.directoryCount
                    if (count >= 0) {
                        playModule(adapter.filenameList, count, isShuffleMode)
                    }
                }
                // play this one
                2 -> playModule(filename)
                // add to queue
                3 -> {
                    addToQueue(filename)
                    toast(this, "Added to queue")
                }
            }
        } else {
            toast(this, "Unrecognized file format")
        }
    }


    // Play this module
    protected fun playModule(mod: String) {
        val modList = ArrayList<String>()
        modList.add(mod)
        playModule(modList, 0, false)
    }

    // Play all modules in list and honor default shuffle mode
    protected fun playModule(modList: List<String>) {
        playModule(modList, 0, false)
    }

    protected fun playModule(modList: List<String>, start: Int) {
        playModule(modList, start, false)
    }

    private fun playModule(modList: List<String>, start: Int, keepFirst: Boolean) {
        val intent = Intent(this, PlayerActivity::class.java)
        (application as XmpApplication).fileList = modList.toMutableList()
        intent.putExtra(PlayerActivity.PARM_SHUFFLE, isShuffleMode)
        intent.putExtra(PlayerActivity.PARM_LOOP, isLoopMode)
        intent.putExtra(PlayerActivity.PARM_START, start)
        intent.putExtra(PlayerActivity.PARM_KEEPFIRST, keepFirst)
        Log.i(TAG, "Start Player activity")
        startActivityForResult(intent, PLAY_MOD_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "Activity result $requestCode,$resultCode")
        when (requestCode) {
            SETTINGS_REQUEST -> {
                update()
                mShowToasts = mPrefs.getBoolean(Preferences.SHOW_TOAST, true)
            }
            PLAY_MOD_REQUEST -> if (resultCode != RESULT_OK) {
                update()
            }
            SEARCH_REQUEST -> refresh = true
        }
    }

    protected fun addToQueue(filename: String) {
        if (InfoCache.testModule(filename)) {
            if (PlayerService.isAlive) {
                val service = Intent(this, PlayerService::class.java)
                mAddList = ArrayList()
                mAddList!!.add(filename)
                bindService(service, connection, 0)
            } else {
                playModule(filename)
            }
        }
    }

    protected fun addToQueue(list: List<String>) {
        val realList = ArrayList<String>()
        var realSize = 0
        var invalid = false

        for (filename in list) {
            if (InfoCache.testModule(filename)) {
                realList.add(filename)
                realSize++
            } else {
                invalid = true
            }
        }

        if (invalid) {
            toast(this, R.string.msg_only_valid_files_sent)
        }

        if (realSize > 0) {
            if (PlayerService.isAlive) {
                val service = Intent(this, PlayerService::class.java)
                mAddList = realList
                bindService(service, connection, 0)
            } else {
                playModule(realList)
            }
        }
    }

    // Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_options, menu)

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
            R.id.menu_prefs -> startActivityForResult(Intent(this, Preferences::class.java), SETTINGS_REQUEST)
            R.id.menu_download -> startActivityForResult(Intent(this, Search::class.java), SEARCH_REQUEST)
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "PlaylistActivity"
        private const val SETTINGS_REQUEST = 45
        private const val PLAY_MOD_REQUEST = 669
        private const val SEARCH_REQUEST = 47
    }
}
