package org.helllabs.android.xmp.modarchive;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.widget.Toast;

import com.telly.groundy.Groundy;
import com.telly.groundy.GroundyTask;
import com.telly.groundy.TaskHandler;
import com.telly.groundy.TaskResult;
import com.telly.groundy.annotations.OnFailure;
import com.telly.groundy.annotations.OnProgress;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.annotations.Param;
import com.telly.groundy.util.DownloadUtils;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import java.io.File;

/*
 * Based on the Groundy download example
 */
public class Downloader {

    private static final String TAG = "Downloader";
    private final Activity mActivity;
    private ProgressDialog mProgressDialog;
    private TaskHandler mTaskHandler;
    private int mSize;
    private DownloaderListener listener;

    private final Object mCallback = new Object() {
        @SuppressLint("NewApi")
        @OnProgress(DownloadTask.class)
        public void onProgress(@Param(Groundy.PROGRESS) final int progress) {
            if (progress == Groundy.NO_SIZE_AVAILABLE) {
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setProgressNumberFormat(null);
                mProgressDialog.setProgressPercentFormat(null);
            } else {
                mProgressDialog.setProgress(mSize * progress / 100);
            }
        }

        @OnSuccess(DownloadTask.class)
        public void onSuccess() {
            Log.d(TAG, "download success");
            Toast.makeText(mActivity, R.string.file_downloaded, Toast.LENGTH_LONG).show();
            mProgressDialog.dismiss();
            if (listener != null) {
                listener.onSuccess();
            }
        }

        @OnFailure(DownloadTask.class)
        public void onFailure(@Param(Groundy.CRASH_MESSAGE) String error) {
            Log.d(TAG, "download fail: " + error);
            if (error == null) {
                error = "Download failed";
            }
            Toast.makeText(mActivity, error, Toast.LENGTH_LONG).show();
            mProgressDialog.dismiss();
            if (listener != null) {
                listener.onFailure();
            }
        }
    };

    public interface DownloaderListener {
        void onSuccess();

        void onFailure();
    }


    public static class DownloadTask extends GroundyTask {
        public static final String PARAM_URL = "org.helllabs.android.xmp.modarchive.URL";
        public static final String PARAM_PATH = "org.helllabs.android.xmp.modarchive.PATH";

        @Override
        protected TaskResult doInBackground() {
            try {
                final String url = getStringArg(PARAM_URL);
                final String path = getStringArg(PARAM_PATH);
                final String name = new File(url).getName();
                final int start = name.indexOf('#') + 1;
                final File dest = new File(path, name.substring(start));
                DownloadUtils.downloadFile(getContext(), url, dest,
                        DownloadUtils.getDownloadListenerForTask(this), () -> isQuitting());

                if (isQuitting()) {
                    return cancelled();
                }
                return succeeded();
            } catch (Exception e) {
                return failed();
            }
        }
    }


    public Downloader(final Activity activity) {
        this.mActivity = activity;
    }

    public void setDownloaderListener(final DownloaderListener listener) {
        this.listener = listener;
    }

    public void download(final String url, final String path, final int size) {

        mSize = size / 1024;

        if (localFile(url, path).exists()) {
            Message.yesNoDialog(mActivity, "File exists!", "This module already exists. Do you want to overwrite?", () -> downloadUrl(url, path));
        } else {
            downloadUrl(url, path);
        }
    }

    @SuppressLint("NewApi")
    private void downloadUrl(final String url, final String path) {

        final File pathFile = new File(path);
        pathFile.mkdirs();

        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMax(mSize);
        mProgressDialog.setProgressNumberFormat("%d KB");
        mProgressDialog.setOnCancelListener(dialogInterface -> {
            if (mTaskHandler != null) {
                mTaskHandler.cancel(mActivity, 0, (id, result) -> Toast.makeText(mActivity, R.string.download_cancelled, Toast.LENGTH_LONG).show());
            }
        });
        mProgressDialog.show();

        mTaskHandler = Groundy.create(Downloader.DownloadTask.class)
                .callback(mCallback)
                .arg(DownloadTask.PARAM_URL, url)
                .arg(DownloadTask.PARAM_PATH, path)
                .queueUsing(mActivity);
    }

    private static File localFile(final String url, final String path) {
        final String filename = url.substring(url.lastIndexOf('#') + 1);
        return new File(path, filename);
    }
}
