package org.helllabs.android.xmp.browser

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.layout_list_controls.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.extension.toast
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.InfoCache
import org.helllabs.android.xmp.util.Log

abstract class BasePlaylistActivity : AppCompatActivity() {
    private var mShowToasts: Boolean = false
    private var mModPlayer: ModInterface? = null
    private var mAddList: MutableList<String> = mutableListOf()
    private var refresh: Boolean = false

    protected lateinit var mPlaylistAdapter: PlaylistAdapter

    private val playAllButtonListener = OnClickListener {
        val list = allFiles
        if (list.isEmpty()) {
            toast(R.string.error_no_files_to_play)
        } else {
            playModule(modList = list)
        }
    }

    private val toggleLoopButtonListener = OnClickListener { view ->
        var loopMode = isLoopMode
        loopMode = loopMode xor true
        (view as ImageButton).setImageResource(
                if (loopMode) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off)

        if (mShowToasts) {
            toast(if (loopMode) R.string.msg_loop_on else R.string.msg_loop_off)
        }

        isLoopMode = loopMode
        saveButtonConfig()
    }

    private val toggleShuffleButtonListener = OnClickListener { view ->
        var shuffleMode = isShuffleMode
        shuffleMode = shuffleMode xor true
        (view as ImageButton).setImageResource(
                if (shuffleMode) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)

        if (mShowToasts) {
            toast(if (shuffleMode) R.string.msg_shuffle_on else R.string.msg_shuffle_off)
        }

        isShuffleMode = shuffleMode
        saveButtonConfig()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mModPlayer = ModInterface.Stub.asInterface(service)
            try {
                mModPlayer!!.add(mAddList)
            } catch (e: RemoteException) {
                toast(R.string.error_adding_mod)
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
        mShowToasts = PrefManager.showToast

        // Action bar icon navigation
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_back)
    }

    override fun onResume() {
        super.onResume()
        if (refresh)
            update()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveButtonConfig()
    }

    protected abstract fun update()

    protected fun setSwipeRefresh(swipeContainer: SwipeRefreshLayout) {
        swipeContainer.apply {
            setColorSchemeResources(R.color.colorRefresh)
            setOnRefreshListener {
                update()
                isRefreshing = false
            }
        }
    }

    private fun saveButtonConfig() {
        Log.d(TAG, "Saving button preferences")
        PrefManager.optionsModeShuffle = isShuffleMode
        PrefManager.optionsModeLoop = isLoopMode
    }

    protected fun setupButtons() {
        control_button_play.apply {
            setImageResource(R.drawable.ic_play)
            setOnClickListener(playAllButtonListener)
        }

        control_button_loop.apply {
            setImageResource(
                    if (isLoopMode) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off)
            setOnClickListener(toggleLoopButtonListener)
        }

        control_button_shuffle.apply {
            setImageResource(
                    if (isShuffleMode) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)
            setOnClickListener(toggleShuffleButtonListener)
        }
    }

    open fun onItemClick(adapter: PlaylistAdapter, view: View, position: Int) {
        val filename = adapter.getItem(position).file!!.path

        // It's a string because of "ListPreference" :U
        val mode = PrefManager.playlistMode.toInt()

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
                        playModule(
                                modList = adapter.filenameList,
                                start = count,
                                keepFirst = isShuffleMode
                        )
                    }
                }
                // play this one
                2 -> playModule(mod = filename)
                // add to queue
                3 -> {
                    addToQueue(filename)
                    toast(text = String.format(getString(R.string.msg_added_queue), filename))
                }
            }
        } else {
            toast(text = String.format(getString(R.string.msg_unreconized_format, filename)))
        }
    }

    // Play this module or all modules in list
    protected fun playModule(
            mod: String? = null,
            modList: List<String>? = null,
            start: Int = 0,
            keepFirst: Boolean = false
    ) {

        (application as XmpApplication).fileList =
                if (!mod.isNullOrEmpty())
                    mutableListOf(mod)
                else
                    modList!!.toMutableList()

        startService(Intent(this, PlayerService::class.java))
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.PARM_SHUFFLE, isShuffleMode)
            putExtra(PlayerActivity.PARM_LOOP, isLoopMode)
            putExtra(PlayerActivity.PARM_START, start)
            putExtra(PlayerActivity.PARM_KEEPFIRST, keepFirst)
        }

        Log.i(TAG, "Start Player activity")
        startActivityForResult(intent, PLAY_MOD_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "Activity result $requestCode,$resultCode")
        when (requestCode) {
            SETTINGS_REQUEST -> {
                update()
                mShowToasts = PrefManager.showToast
            }
            PLAY_MOD_REQUEST -> {
                if (resultCode != RESULT_OK) {
                    update()
                }
            }
            SEARCH_REQUEST -> refresh = true
        }
    }

    protected fun addToQueue(filename: String) {
        if (InfoCache.testModule(filename)) {
            if (PlayerService.isAlive) {
                val service = Intent(this, PlayerService::class.java)
                mAddList.clear()
                mAddList.add(filename)
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
            toast(R.string.msg_only_valid_files_sent)
        }

        if (realSize > 0) {
            if (PlayerService.isAlive) {
                val service = Intent(this, PlayerService::class.java)
                mAddList.clear()
                mAddList.addAll(realList)
                bindService(service, connection, 0)
            } else {
                playModule(modList = realList)
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
            android.R.id.home -> {
                val intent = Intent(this, PlaylistMenu::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                return true
            }
            R.id.menu_prefs ->
                startActivityForResult(
                        Intent(this, Preferences::class.java), SETTINGS_REQUEST
                )
            R.id.menu_download ->
                startActivityForResult(
                        Intent(this, Search::class.java), SEARCH_REQUEST
                )
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG = BasePlaylistActivity::class.java.simpleName
        private const val SETTINGS_REQUEST = 45
        private const val PLAY_MOD_REQUEST = 669
        private const val SEARCH_REQUEST = 47

        // const val DEFAULT_SHUFFLE_MODE = true
        // const val DEFAULT_LOOP_MODE = false
    }
}
