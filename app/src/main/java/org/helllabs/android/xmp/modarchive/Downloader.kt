package org.helllabs.android.xmp.modarchive

import java.io.File

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.Log
import org.helllabs.android.xmp.util.Message

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Build
import android.widget.Toast

import com.telly.groundy.Groundy
import com.telly.groundy.GroundyManager
import com.telly.groundy.GroundyTask
import com.telly.groundy.TaskHandler
import com.telly.groundy.TaskResult
import com.telly.groundy.annotations.OnFailure
import com.telly.groundy.annotations.OnProgress
import com.telly.groundy.annotations.OnSuccess
import com.telly.groundy.annotations.Param
import com.telly.groundy.util.DownloadUtils

/*
 * Based on the Groundy download example
 */
class Downloader(private val mActivity: Activity) {
    private var mProgressDialog: ProgressDialog? = null
    private var mTaskHandler: TaskHandler? = null
    private var mSize: Int = 0
    private var listener: DownloaderListener? = null

    private val mCallback = object : Any() {
        @SuppressLint("NewApi")
        @OnProgress(DownloadTask::class)
        fun onProgress(@Param(Groundy.PROGRESS) progress: Int) {
            if (progress == Groundy.NO_SIZE_AVAILABLE) {
                mProgressDialog!!.isIndeterminate = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    mProgressDialog!!.setProgressNumberFormat(null)
                    mProgressDialog!!.setProgressPercentFormat(null)
                }
            } else {
                mProgressDialog!!.progress = mSize * progress / 100
            }
        }

        @OnSuccess(DownloadTask::class)
        fun onSuccess() {
            Log.d(TAG, "download success")
            Toast.makeText(mActivity, R.string.file_downloaded, Toast.LENGTH_LONG).show()
            mProgressDialog!!.dismiss()
            if (listener != null) {
                listener!!.onSuccess()
            }
        }

        @OnFailure(DownloadTask::class)
        fun onFailure(@Param(Groundy.CRASH_MESSAGE) error: String?) {
            var error = error
            Log.d(TAG, "download fail: " + error!!)
            if (error == null) {
                error = "Download failed"
            }
            Toast.makeText(mActivity, error, Toast.LENGTH_LONG).show()
            mProgressDialog!!.dismiss()
            if (listener != null) {
                listener!!.onFailure()
            }
        }
    }

    interface DownloaderListener {
        fun onSuccess()
        fun onFailure()
    }


    class DownloadTask : GroundyTask() {

        override fun doInBackground(): TaskResult {
            try {
                val url = getStringArg(PARAM_URL)
                val path = getStringArg(PARAM_PATH)
                val name = File(url).name
                val start = name.indexOf('#') + 1
                val dest = File(path, name.substring(start))
                DownloadUtils.downloadFile(context, url, dest,
                        DownloadUtils.getDownloadListenerForTask(this), DownloadUtils.DownloadCancelListener { isQuitting })

                return if (isQuitting) {
                    cancelled()
                } else succeeded()
            } catch (e: Exception) {
                return failed()
            }

        }

        companion object {
            val PARAM_URL = "org.helllabs.android.xmp.modarchive.URL"
            val PARAM_PATH = "org.helllabs.android.xmp.modarchive.PATH"
        }
    }

    fun setDownloaderListener(listener: DownloaderListener) {
        this.listener = listener
    }

    fun download(url: String, path: String, size: Int) {

        mSize = size / 1024

        if (localFile(url, path).exists()) {
            Message.yesNoDialog(mActivity, "File exists!", "This module already exists. Do you want to overwrite?", Runnable { downloadUrl(url, path) })
        } else {
            downloadUrl(url, path)
        }
    }

    @SuppressLint("NewApi")
    private fun downloadUrl(url: String, path: String) {

        val pathFile = File(path)
        pathFile.mkdirs()

        mProgressDialog = ProgressDialog(mActivity)
        mProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        mProgressDialog!!.setCancelable(true)
        mProgressDialog!!.max = mSize
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mProgressDialog!!.setProgressNumberFormat("%d KB")
        }
        mProgressDialog!!.setOnCancelListener {
            if (mTaskHandler != null) {
                mTaskHandler!!.cancel(mActivity, 0) { id, result -> Toast.makeText(mActivity, R.string.download_cancelled, Toast.LENGTH_LONG).show() }
            }
        }
        mProgressDialog!!.show()

        mTaskHandler = Groundy.create(Downloader.DownloadTask::class.java)
                .callback(mCallback)
                .arg(DownloadTask.PARAM_URL, url)
                .arg(DownloadTask.PARAM_PATH, path)
                .queueUsing(mActivity)
    }

    companion object {

        private val TAG = "Downloader"

        private fun localFile(url: String, path: String): File {
            val filename = url.substring(url.lastIndexOf('#') + 1, url.length)
            return File(path, filename)
        }
    }
}
