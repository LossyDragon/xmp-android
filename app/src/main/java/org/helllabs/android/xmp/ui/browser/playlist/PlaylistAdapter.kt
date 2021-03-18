package org.helllabs.android.xmp.ui.browser.playlist

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
import org.helllabs.android.xmp.databinding.ItemPlaylistCardBinding
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.recyclerview.ItemTouchHelperAdapter
import org.helllabs.android.xmp.util.recyclerview.ItemTouchHelperViewHolder
import org.helllabs.android.xmp.util.recyclerview.OnStartDragListener

class PlaylistAdapter(
    private var layoutType: Int,
    private var useFilename: Boolean,
) : ListAdapter<PlaylistItem, RecyclerView.ViewHolder>(DIFF_CALLBACK),
    ItemTouchHelperAdapter {

    companion object {
        const val LAYOUT_LIST = 0
        const val LAYOUT_CARD = 1
        const val LAYOUT_DRAG = 2

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PlaylistItem>() {
            override fun areItemsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    val filenameList: List<String>
        get() {
            val list: MutableList<String> = ArrayList()
            for (item in currentList) {
                if (item.type == PlaylistItem.TYPE_FILE) {
                    list.add(item.file!!.path)
                }
            }
            return list
        }

    val directoryCount: Int
        get() {
            var count = 0
            for (item in currentList) {
                if (item.type != PlaylistItem.TYPE_DIRECTORY) {
                    break
                }
                count++
            }
            return count
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
            LAYOUT_CARD -> {
                val binder = ItemPlaylistCardBinding.inflate(inflater, parent, false)
                CardViewHolder(binder)
            }
            LAYOUT_LIST, LAYOUT_DRAG -> {
                val binder = ItemPlaylistBinding.inflate(inflater, parent, false)
                ListViewHolder(binder)
            }
            else -> throw IllegalArgumentException("Wrong layout type defined $layoutType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is CardViewHolder -> holder.onBind(item)
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

    inner class CardViewHolder(
        val binder: ItemPlaylistCardBinding
    ) : RecyclerView.ViewHolder(binder.root) {
        fun onBind(item: PlaylistItem) = with(binder) {
            playlistItem = item
            root.click {
                onClick?.invoke(adapterPosition)
            }
            root.longClick {
                onLongClick?.invoke(adapterPosition)
                true
            }
            executePendingBindings()
        }
    }

    inner class ListViewHolder(
        val binder: ItemPlaylistBinding
    ) : RecyclerView.ViewHolder(binder.root), ItemTouchHelperViewHolder {
        fun onBind(item: PlaylistItem) = with(binder) {
            playlistItem = item
            isDraggable = layoutType == LAYOUT_DRAG
            fileName = useFilename
            if (layoutType == LAYOUT_DRAG) {
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
                onClick?.invoke(adapterPosition)
            }
            root.longClick {
                onLongClick?.invoke(adapterPosition)
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
fun setSrcCompat(view: AppCompatImageView, type: Int) {
    val image = when (type) {
        PlaylistItem.TYPE_DIRECTORY,
        PlaylistItem.TYPE_SPECIAL -> R.drawable.ic_folder
        PlaylistItem.TYPE_PLAYLIST -> R.drawable.ic_list
        PlaylistItem.TYPE_FILE -> R.drawable.ic_file
        else -> throw IllegalArgumentException("Image res is wrong type: $type")
    }
    view.setImageResource(image)
}
