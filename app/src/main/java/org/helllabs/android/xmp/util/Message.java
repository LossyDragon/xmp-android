package org.helllabs.android.xmp.util;

import org.helllabs.android.xmp.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public final class Message {

    private Message() {

    }

    public static void fatalError(final Activity activity, final String message) {

        activity.runOnUiThread(() -> {
            final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
            alertDialog.setTitle(R.string.error);
            alertDialog.setMessage(message);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.exit), (dialog, which) -> {
                dialog.dismiss();
                activity.finish();
            });
            alertDialog.show();
        });
    }

    public static void error(final Activity activity, final String message) {

        activity.runOnUiThread(() -> {
            final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
            alertDialog.setTitle(R.string.error);
            alertDialog.setMessage(message);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.dismiss), (dialog, which) -> {
                //
            });
            alertDialog.show();
        });
    }

    public static void error(final Activity activity, final int resId) {
        error(activity, activity.getString(resId));
    }

    public static void toast(final Context context, final String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void toast(final Context context, final int resId) {
        toast(context, context.getString(resId));
    }

    public static void yesNoDialog(final Activity activity, final String title, final String message, final Runnable runnable) {

        activity.runOnUiThread(() -> {
            final DialogInterface.OnClickListener listener = (dialog, which) -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    runnable.run();
                }
            };

            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.yes, listener)
                    .setNegativeButton(R.string.no, listener)
                    .show();
        });
    }
}
