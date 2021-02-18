package org.helllabs.android.xmp.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.preferences.Preferences;

public class ChangeLog {
    private static final String TAG = "ChangeLog";
    private final Context context;

    public ChangeLog(final Context context) {
        this.context = context;
    }

    public int show() {
        try {
            final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            final int versionCode = packageInfo.versionCode;

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final int lastViewed = prefs.getInt(Preferences.CHANGELOG_VERSION, 0);

            if (lastViewed < versionCode) {
                final Editor editor = prefs.edit();
                editor.putInt(Preferences.CHANGELOG_VERSION, versionCode);
                editor.apply();
                showLog();
                return 0;
            } else {
                return -1;
            }
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Unable to get version code");
            return -1;
        }
    }

    private void showLog() {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.changelog, null);

        new AlertDialog.Builder(context)
                .setTitle("Changelog")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setView(view)
                .setNegativeButton("Dismiss", (dialog, whichButton) -> {
                    // Do nothing
                }).show();
    }
}
