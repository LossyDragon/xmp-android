package org.helllabs.android.xmp.browser


import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import org.helllabs.android.xmp.util.Log

import java.io.File
import java.util.Stack

class FilelistNavigation {
    private val mPathStack: Stack<ListState>
    /**
     * Get the current directory.
     *
     * @return The current directory pathname.
     */
    var currentDir: File? = null
        private set

    /**
     * Check whether we're at the file system root.
     *
     * @return True if we're at the file system root.
     */
    val isAtTopDir: Boolean
        get() = mPathStack.isEmpty()

    /**
     * To restore list position when traversing directories.
     */
    private class ListState(recyclerView: RecyclerView) {
        private val index: Int
        private val top: Int

        init {
            val layoutManager = recyclerView.getLayoutManager() as LinearLayoutManager
            this.index = layoutManager.findFirstVisibleItemPosition()
            val view = recyclerView.getChildAt(0)
            this.top = if (view == null) 0 else view!!.getTop()
        }

        fun restoreState(recyclerView: RecyclerView) {
            val layoutManager = recyclerView.getLayoutManager() as LinearLayoutManager
            recyclerView.post(Runnable { layoutManager.scrollToPositionWithOffset(index, top) })
        }
    }

    init {
        mPathStack = Stack()
    }

    /**
     * Change to the specified directory.
     *
     * @param file The name of the directory to change to.
     *
     * @return True if current directory was changed.
     */
    fun changeDirectory(file: File?): Boolean {
        var file: File? = file ?: return false

        val isDir = file!!.isDirectory

        if (isDir) {
            if (file.name == "..") {
                file = file.parentFile!!.parentFile
                if (file == null) {
                    file = File("/")
                }
            }
            currentDir = file
        }

        return isDir
    }

    /**
     * Save RecyclerView position.
     *
     * @param recyclerView The RecyclerView whose position is to be saved.
     */
    fun saveListPosition(recyclerView: RecyclerView) {
        mPathStack.push(ListState(recyclerView))
    }

    /**
     * Restore RecyclerView position.
     *
     * @param recyclerView The RecyclerView whose position is to be restored.
     */
    fun restoreListPosition(recyclerView: RecyclerView) {
        if (!mPathStack.isEmpty()) {
            val state = mPathStack.pop()
            state.restoreState(recyclerView)
        }
    }

    /**
     * Start filesystem navigation and clear previous history.
     *
     * @param currentDir The directory to start navigation at.
     */
    fun startNavigation(currentDir: File) {
        Log.d(TAG, "start navigation at " + currentDir.path)
        this.currentDir = currentDir
        mPathStack.clear()
    }

    /**
     * Navigate to the parent directory.
     *
     * @return True if the current directory was changed.
     */
    fun parentDir(): Boolean {
        if (currentDir == null) {
            return false
        }

        val parent = currentDir!!.parentFile ?: return false

        currentDir = parent
        return true
    }

    companion object {

        private val TAG = "FilelistNavigation"
    }
}
