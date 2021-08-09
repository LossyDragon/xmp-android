package org.helllabs.android.xmp.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ItemPlaylistBinding
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.ui.util.recyclerview.ItemTouchHelperAdapter
import org.helllabs.android.xmp.ui.util.recyclerview.ItemTouchHelperViewHolder
import org.helllabs.android.xmp.ui.util.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.PlaylistUtils

enum class PlaylistLayoutType(val value: Int) {
    TYPE_LIST(0),
    TYPE_DRAG(2),
}

class BasePlaylistAdapter(
    private var layoutType: PlaylistLayoutType,
    private var useFilename: Boolean,
) : ListAdapter<PlaylistItem, RecyclerView.ViewHolder>(DIFF_CALLBACK),
    ItemTouchHelperAdapter {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PlaylistItem>() {
            override fun areItemsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    var dragListener: OnStartDragListener? = null
    var onClick: ((position: Int) -> Unit)? = null
    var onLongClick: ((position: Int) -> Unit)? = null
    var isDragEnabled: Boolean = false
        private set

    // Temp list to handle drag & drop
    private lateinit var tempList: MutableList<PlaylistItem>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (layoutType) {
            PlaylistLayoutType.TYPE_LIST,
            PlaylistLayoutType.TYPE_DRAG -> {
                val binder = ItemPlaylistBinding.inflate(inflater, parent, false)
                ListViewHolder(binder)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ListViewHolder -> holder.onBind(item)
        }
    }

    override fun getItemId(position: Int): Long = currentList[position].id.toLong()

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(tempList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    fun getItems(): List<PlaylistItem> = currentList

    fun getFilename(location: Int): String = currentList[location].file!!.path

    fun getFile(location: Int): File = currentList[location].file!!

    fun setUseFilename(useFilename: Boolean) {
        this.useFilename = useFilename
    }

    fun getDirectoryCount(): Int {
        var count = 0
        for (item in currentList) {
            if (item.type != PlaylistType.TYPE_DIRECTORY) {
                break
            }
            count++
        }
        return count
    }

    fun getFilenameList(): List<String> {
        val list: MutableList<String> = ArrayList()
        for (item in currentList) {
            if (item.type == PlaylistType.TYPE_FILE) {
                list.add(item.file!!.path)
            }
        }
        return list
    }

    inner class ListViewHolder(
        val binder: ItemPlaylistBinding
    ) : RecyclerView.ViewHolder(binder.root), ItemTouchHelperViewHolder {
        fun onBind(item: PlaylistItem) = with(binder) {
            playlistItem = item
            fileName = useFilename
            if (layoutType == PlaylistLayoutType.TYPE_DRAG) {
                isDraggable = true
                handle.let { handle ->
                    handle.touch { _, event ->
                        logD("Touch: ${event.actionMasked}")
                        isDragEnabled =
                            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                                dragListener?.onStartDrag(this@ListViewHolder)
                                true
                            } else {
                                false
                            }
                        true // Continue to consume the touch event.
                    }
                }
            }
            root.click {
                onClick?.invoke(absoluteAdapterPosition)
            }
            root.longClick {
                onLongClick?.invoke(absoluteAdapterPosition)
                true
            }
            executePendingBindings()
        }

        override fun onItemSelected() {
            binder.root.setBackgroundColor(Color.LTGRAY)
            dragListener?.disableSwipe(true)
            tempList = currentList.toMutableList()
        }

        override fun onItemClear() {
            PlaylistUtils.renumberIds(tempList)
            binder.root.setBackgroundColor(0)
            dragListener?.onStopDrag(tempList)
            dragListener?.disableSwipe(false)
        }
    }
}

/* Binding Adapter functions */
@BindingAdapter("android:draggability")
fun setDraggability(view: View, isVisible: Boolean) {
    view.visibility = if (isVisible) View.VISIBLE else View.GONE
}

@BindingAdapter("android:boldTextStyle")
fun setBoldTextStyle(view: TextView, boolean: Boolean) {
    view.setTypeface(null, if (boolean) Typeface.ITALIC else Typeface.NORMAL)
}

@BindingAdapter("android:srcCompat")
fun setSrcCompat(view: AppCompatImageView, type: PlaylistType) {
    val image = when (type) {
        PlaylistType.TYPE_DIRECTORY,
        PlaylistType.TYPE_SPECIAL -> R.drawable.ic_folder
        PlaylistType.TYPE_PLAYLIST -> R.drawable.ic_list
        PlaylistType.TYPE_FILE -> R.drawable.ic_file
    }
    view.setImageResource(image)
}
