package org.helllabs.android.xmp.browser

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.helllabs.android.xmp.util.Log.d
import java.io.File
import java.util.*

class FilelistNavigation {

    private val mPathStack: Stack<ListState> = Stack()

    /**
     * Get the current directory.
     *
     * @return The current directory pathname.
     */
    var currentDir: File? = null
        private set

    /**
     * To restore list position when traversing directories.
     */
    private class ListState(recyclerView: RecyclerView?) {
        private val index: Int
        private val top: Int
        fun restoreState(recyclerView: RecyclerView?) {
            val layoutManager = recyclerView!!.layoutManager as LinearLayoutManager?
            recyclerView.post { layoutManager!!.scrollToPositionWithOffset(index, top) }
        }

        init {
            val layoutManager = recyclerView!!.layoutManager as LinearLayoutManager?
            index = layoutManager!!.findFirstVisibleItemPosition()
            val view = recyclerView.getChildAt(0)
            top = view?.top ?: 0
        }
    }

    /**
     * Change to the specified directory.
     *
     * @param file The name of the directory to change to.
     * @return True if current directory was changed.
     */
    fun changeDirectory(file: File?): Boolean {
        var dirFile: File? = file ?: return false
        val isDir = dirFile!!.isDirectory
        if (isDir) {
            if (dirFile.name == "..") {
                dirFile = dirFile.parentFile.parentFile
                if (dirFile == null) {
                    dirFile = File("/")
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
    fun saveListPosition(recyclerView: RecyclerView?) {
        mPathStack.push(ListState(recyclerView))
    }

    /**
     * Restore RecyclerView position.
     *
     * @param recyclerView The RecyclerView whose position is to be restored.
     */
    fun restoreListPosition(recyclerView: RecyclerView?) {
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
        d(TAG, "start navigation at " + currentDir.path)
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

    /**
     * Check whether we're at the file system root.
     *
     * @return True if we're at the file system root.
     */
    val isAtTopDir: Boolean
        get() = mPathStack.isEmpty()

    companion object {
        private const val TAG = "FilelistNavigation"
    }

}