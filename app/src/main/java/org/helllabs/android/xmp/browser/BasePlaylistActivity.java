package org.helllabs.android.xmp.browser;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.XmpApplication;
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter;
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils;
import org.helllabs.android.xmp.modarchive.Search;
import org.helllabs.android.xmp.player.PlayerActivity;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.service.ModInterface;
import org.helllabs.android.xmp.service.PlayerService;
import org.helllabs.android.xmp.util.InfoCache;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import java.util.ArrayList;
import java.util.List;

public abstract class BasePlaylistActivity extends AppCompatActivity {
	private static final String TAG = "PlaylistActivity";
	private static final int SETTINGS_REQUEST = 45;
	private static final int PLAY_MOD_REQUEST = 669;
	private static final int SEARCH_REQUEST = 47;
	private boolean mShowToasts;
	private ModInterface mModPlayer;
	private List<String> mAddList;
	protected SharedPreferences mPrefs;
	protected PlaylistAdapter mPlaylistAdapter;
	private boolean refresh;

	private final OnClickListener playAllButtonListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			final List<String> list = getAllFiles();
			if (list.isEmpty()) {
				Message.toast(BasePlaylistActivity.this, R.string.error_no_files_to_play);
			} else {
				playModule(list);
			}
		}
	};

	private final OnClickListener toggleLoopButtonListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			boolean loopMode = isLoopMode();
			loopMode ^= true;
			((ImageButton) view).setImageResource(loopMode ?
					R.drawable.list_loop_on : R.drawable.list_loop_off);
			if (mShowToasts) {
				Message.toast(view.getContext(), loopMode ? R.string.msg_loop_on : R.string.msg_loop_off);
			}
			setLoopMode(loopMode);
		}
	};

	private final OnClickListener toggleShuffleButtonListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			boolean shuffleMode = isShuffleMode();
			shuffleMode ^= true;
			((ImageButton) view).setImageResource(shuffleMode ? R.drawable.list_shuffle_on : R.drawable.list_shuffle_off);
			if (mShowToasts) {
				Message.toast(view.getContext(), shuffleMode ? R.string.msg_shuffle_on : R.string.msg_shuffle_off);
			}
			setShuffleMode(shuffleMode);
		}
	};


	// Connection

	private final ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(final ComponentName className, final IBinder service) {
			mModPlayer = ModInterface.Stub.asInterface(service);
			try {
				mModPlayer.add(mAddList);
			} catch (RemoteException e) {
				Message.toast(BasePlaylistActivity.this, R.string.error_adding_mod);
			}
			unbindService(connection);
		}

		public void onServiceDisconnected(final ComponentName className) {
			mModPlayer = null;    // NOPMD
		}
	};


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mShowToasts = mPrefs.getBoolean(Preferences.SHOW_TOAST, true);

		// Action bar icon navigation
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (refresh) {
			update();
		}
	}

	protected abstract void setShuffleMode(boolean shuffleMode);

	protected abstract void setLoopMode(boolean loopMode);

	protected abstract boolean isShuffleMode();

	protected abstract boolean isLoopMode();

	protected abstract List<String> getAllFiles();

	protected abstract void update();

	protected void setSwipeRefresh(final RecyclerView recyclerView) {
		final SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
		swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				update();
				swipeRefresh.setRefreshing(false);
			}
		});
		swipeRefresh.setColorSchemeResources(R.color.refresh_color);

		recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
			@Override
			public boolean onInterceptTouchEvent(final RecyclerView rv, final MotionEvent e) {
				if (e.getAction() == MotionEvent.ACTION_DOWN) {
					boolean enable = false;
					if (recyclerView.getChildCount() > 0) {
						enable = !recyclerView.canScrollVertically(-1);
					}
					swipeRefresh.setEnabled(enable);
				}

				return false;
			}

			@Override
			public void onTouchEvent(final RecyclerView rv, final MotionEvent e) {
				// do nothing
			}

			@Override
			public void onRequestDisallowInterceptTouchEvent(final boolean disallowIntercept) {
				// do nothing
			}
		});
	}

	protected void setupButtons() {
		final ImageButton playAllButton = (ImageButton) findViewById(R.id.play_all);
		final ImageButton toggleLoopButton = (ImageButton) findViewById(R.id.toggle_loop);
		final ImageButton toggleShuffleButton = (ImageButton) findViewById(R.id.toggle_shuffle);

		playAllButton.setImageResource(R.drawable.list_play);
		playAllButton.setOnClickListener(playAllButtonListener);

		toggleLoopButton.setImageResource(isLoopMode() ? R.drawable.list_loop_on : R.drawable.list_loop_off);
		toggleLoopButton.setOnClickListener(toggleLoopButtonListener);

		toggleShuffleButton.setImageResource(isShuffleMode() ? R.drawable.list_shuffle_on : R.drawable.list_shuffle_off);
		toggleShuffleButton.setOnClickListener(toggleShuffleButtonListener);
	}

	public void onItemClick(final PlaylistAdapter adapter, final View view, final int position) {
		final String filename = adapter.getItem(position).getFile().getPath();

		final int mode = Integer.parseInt(mPrefs.getString(Preferences.PLAYLIST_MODE, "1"));

		/* Test module again if invalid, in case a new file format is added to the
		 * player library and the file was previously unrecognized and cached as invalid.
		 */
		if (InfoCache.testModuleForceIfInvalid(filename)) {
			switch (mode) {
				case 1:                                // play all starting at this one
					final int count = position - adapter.getDirectoryCount();
					if (count >= 0) {
						playModule(adapter.getFilenameList(), count, isShuffleMode());
					}
					break;
				case 2:                                // play this one
					playModule(filename);
					break;
				case 3:                                // add to queue
					addToQueue(filename);
					Message.toast(this, "Added to queue");
					break;
			}
		} else {
			Message.toast(this, "Unrecognized file format");
		}
	}

    /*
	// Item click	
	protected void setOnItemClickListener(final RecyclerView list) {
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> list, final View view, final int position, final long id) {
				onListItemClick(list, view, position, id);
			}
		});
	}
    */

	// Play this module
	protected void playModule(final String mod) {
		final List<String> modList = new ArrayList<>();
		modList.add(mod);
		playModule(modList, 0, false);
	}

	// Play all modules in list and honor default shuffle mode
	protected void playModule(final List<String> modList) {
		playModule(modList, 0, false);
	}

	protected void playModule(final List<String> modList, final int start) {
		playModule(modList, start, false);
	}

	private void playModule(final List<String> modList, final int start, final boolean keepFirst) {
		final Intent intent = new Intent(this, PlayerActivity.class);
		((XmpApplication) getApplication()).setFileList(modList);
		intent.putExtra(PlayerActivity.PARM_SHUFFLE, isShuffleMode());
		intent.putExtra(PlayerActivity.PARM_LOOP, isLoopMode());
		intent.putExtra(PlayerActivity.PARM_START, start);
		intent.putExtra(PlayerActivity.PARM_KEEPFIRST, keepFirst);
		//intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);	// prevent screen flicker when starting player activity 
		Log.i(TAG, "Start Player activity");
		startActivityForResult(intent, PLAY_MOD_REQUEST);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i(TAG, "Activity result " + requestCode + "," + resultCode);
		switch (requestCode) {
			case SETTINGS_REQUEST:
				update();
				mShowToasts = mPrefs.getBoolean(Preferences.SHOW_TOAST, true);
				break;
			case PLAY_MOD_REQUEST:
				if (resultCode != RESULT_OK) {
					update();
				}
				break;
			case SEARCH_REQUEST:
				refresh = true;
				break;
		}
	}

	protected void addToQueue(final String filename) {
		if (InfoCache.testModule(filename)) {
			if (PlayerService.isAlive) {
				final Intent service = new Intent(this, PlayerService.class);
				mAddList = new ArrayList<>();
				mAddList.add(filename);
				bindService(service, connection, 0);
			} else {
				playModule(filename);
			}
		}
	}

	protected void addToQueue(final List<String> list) {
		final List<String> realList = new ArrayList<>();
		int realSize = 0;
		boolean invalid = false;

		for (final String filename : list) {
			if (InfoCache.testModule(filename)) {
				realList.add(filename);
				realSize++;
			} else {
				invalid = true;
			}
		}

		if (invalid) {
			Message.toast(this, R.string.msg_only_valid_files_sent);
		}

		if (realSize > 0) {
			if (PlayerService.isAlive) {
				final Intent service = new Intent(this, PlayerService.class);
				mAddList = realList;
				bindService(service, connection, 0);
			} else {
				playModule(realList);
			}
		}
	}

	// Menu

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);

		// Calling super after populating the menu is necessary here to ensure that the
		// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				final Intent intent = new Intent(this, PlaylistMenu.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return true;
			case R.id.menu_new_playlist:
				PlaylistUtils.newPlaylistDialog(this);
				break;
			case R.id.menu_prefs:
				startActivityForResult(new Intent(this, Preferences.class), SETTINGS_REQUEST);
				break;
			case R.id.menu_refresh:
				update();
				break;
			case R.id.menu_download:
				startActivityForResult(new Intent(this, Search.class), SEARCH_REQUEST);
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
