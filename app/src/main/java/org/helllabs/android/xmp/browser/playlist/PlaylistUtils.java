package org.helllabs.android.xmp.browser.playlist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.Xmp;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.util.Message;
import org.helllabs.android.xmp.util.ModInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class PlaylistUtils {

    private PlaylistUtils() {

    }

    public static void newPlaylistDialog(final Activity activity) {
        newPlaylistDialog(activity, null);
    }

    @SuppressLint("InflateParams")
    public static void newPlaylistDialog(final Activity activity, final Runnable runnable) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setTitle("New playlist");
        final LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.newlist, null);

        alert.setView(layout);

        alert.setPositiveButton(R.string.ok, (dialog, whichButton) -> {
            final EditText e1 = (EditText) layout.findViewById(R.id.new_playlist_name);
            final EditText e2 = (EditText) layout.findViewById(R.id.new_playlist_comment);
            final String name = e1.getText().toString();
            final String comment = e2.getText().toString();

            if (createEmptyPlaylist(activity, name, comment) && runnable != null) {
                runnable.run();
            }
        });

        alert.setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
            // Canceled.
        });

        alert.show();
    }

    /*
     * Send files to the specified playlist
     */
    private static void addFiles(final Activity activity, final List<String> fileList, final String playlistName) {
        final List<PlaylistItem> list = new ArrayList<>();
        final ModInfo modInfo = new ModInfo();
        boolean hasInvalid = false;

        int id = 0;
        for (final String filename : fileList) {
            if (Xmp.testModule(filename, modInfo)) {
                final PlaylistItem item = new PlaylistItem(PlaylistItem.TYPE_FILE, modInfo.name, modInfo.type);
                item.setFile(new File(filename));
                list.add(item);
            } else {
                hasInvalid = true;
            }
        }

        if (!list.isEmpty()) {
            Playlist.addToList(activity, playlistName, list);

            if (hasInvalid) {
                activity.runOnUiThread(() -> {
                    if (list.size() > 1) {
                        Message.toast(activity, R.string.msg_only_valid_files_added);
                    } else {
                        Message.error(activity, R.string.unrecognized_format);
                    }
                });
            }
        }

        renumberIds(list);
    }

    public static void filesToPlaylist(final Activity activity, final List<String> fileList, final String playlistName) {

        final ProgressDialog progressDialog = ProgressDialog.show(activity, "Please wait", "Scanning module files...", true);

        new Thread() {
            public void run() {
                addFiles(activity, fileList, playlistName);
                progressDialog.dismiss();
            }
        }.start();
    }

    public static void filesToPlaylist(final Activity activity, final String filename, final String playlistName) {
        final List<String> fileList = new ArrayList<>();
        fileList.add(filename);
        addFiles(activity, fileList, playlistName);
    }

    public static String[] list() {
        final String[] ret = Preferences.DATA_DIR.list(new PlaylistFilter());
        return ret == null ? new String[0] : ret;
    }

    public static String[] listNoSuffix() {
        String[] pList = list();
        for (int i = 0; i < pList.length; i++) {
            pList[i] = pList[i].substring(0, pList[i].lastIndexOf(Playlist.PLAYLIST_SUFFIX));    //NOPMD
        }
        return pList;
    }

    public static String getPlaylistName(final int index) {
        final String[] pList = list();
        return pList[index].substring(0, pList[index].lastIndexOf(Playlist.PLAYLIST_SUFFIX));
    }

    public static boolean createEmptyPlaylist(final Activity activity, final String name, final String comment) {
        try {
            final Playlist playlist = new Playlist(activity, name);
            playlist.setComment(comment);
            playlist.commit();
            return true;
        } catch (IOException e) {
            Message.error(activity, activity.getString(R.string.error_create_playlist));
            return false;
        }
    }

    // Stable IDs for used by Advanced RecyclerView
    public static void renumberIds(final List<PlaylistItem> list) {
        int id = 0;
        for (final PlaylistItem item : list) {
            item.setId(id++);
        }
    }
}
