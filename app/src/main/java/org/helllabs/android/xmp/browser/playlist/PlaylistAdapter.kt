package org.helllabs.android.xmp.browser.playlist

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.recyclerview.ItemTouchHelperAdapter
import org.helllabs.android.xmp.util.recyclerview.ItemTouchHelperViewHolder
import org.helllabs.android.xmp.util.recyclerview.OnStartDragListener

class PlaylistAdapter : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>, ItemTouchHelperAdapter {

    var position = 0

    var onClick: ((adapter: PlaylistAdapter, position: Int) -> Unit)? = null
    var onLongClick: ((adapter: PlaylistAdapter, position: Int) -> Unit)? = null

    private var mDragStartListener: OnStartDragListener? = null
    private val typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val playlist: Playlist?
    private val items: MutableList<PlaylistItem>
    private var useFilename: Boolean

    private val layoutType: Int

    val filenameList: List<String>
        get() {
            val list: MutableList<String> = ArrayList()
            for (item in items) {
                if (item.type == PlaylistItem.TYPE_FILE) {
                    list.add(item.file!!.path)
                }
            }
            return list
        }

    val directoryCount: Int
        get() {
            var count = 0
            for (item in items) {
                if (item.type != PlaylistItem.TYPE_DIRECTORY) {
                    break
                }
                count++
            }
            return count
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout: Int = when (layoutType) {
            LAYOUT_CARD -> R.layout.item_playlist_card
            else -> R.layout.item_playlist
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        holder.onBind(items[pos])
    }

    constructor(
        items: MutableList<PlaylistItem>,
        useFilename: Boolean,
        layoutType: Int
    ) {
        playlist = null
        this.items = items
        this.useFilename = useFilename
        this.layoutType = layoutType

        // DraggableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true)
    }

    constructor(
        playlist: Playlist,
        useFilename: Boolean,
        layoutType: Int,
        drag: OnStartDragListener
    ) {
        this.playlist = playlist
        items = playlist.list
        this.useFilename = useFilename
        this.layoutType = layoutType
        mDragStartListener = drag

        // DraggableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].id.toLong()

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition == toPosition) {
            return false
        }

        val item = items[fromPosition]
        items.remove(item)
        items.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
        playlist?.setListChanged(true)

        return true
    }

    fun getItem(num: Int): PlaylistItem {
        return items[num]
    }

    fun getItems(): List<PlaylistItem> {
        return items
    }

    fun clear() {
        items.clear()
    }

    fun add(item: PlaylistItem) {
        items.add(item)
    }

    fun setUseFilename(useFilename: Boolean) {
        this.useFilename = useFilename
    }

    fun getFilename(location: Int): String {
        return items[location].file!!.path
    }

    fun getFile(location: Int): File {
        return items[location].file!!
    }

    fun addList(list: List<PlaylistItem>) {
        items.addAll(list)
    }

    inner class ViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView),
        ItemTouchHelperViewHolder {

        fun onBind(item: PlaylistItem) {
            val container: View = itemView.findViewById(R.id.plist_container)
            val handle: View? = itemView.findViewById(R.id.plist_handle)
            val titleText: TextView = itemView.findViewById(R.id.plist_title)
            val infoText: TextView = itemView.findViewById(R.id.plist_info)
            val image: ImageView = itemView.findViewById(R.id.plist_image)

            // Drag
            if (handle != null && handle.isVisible) {
                handle.touch { _, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        mDragStartListener?.onStartDrag(this)
                    }
                    false
                }
            }

            if (item.type == PlaylistItem.TYPE_DIRECTORY) {
                infoText.setTypeface(typeface, Typeface.ITALIC)
            } else {
                infoText.setTypeface(typeface, Typeface.NORMAL)
            }

            titleText.text = if (useFilename) item.filename else item.name
            infoText.text = item.comment
            image.setImageResource(item.imageRes)

            if (layoutType == LAYOUT_DRAG) {
                handle?.show()
            }

            container.click {
                onClick?.invoke(this@PlaylistAdapter, adapterPosition)
            }

            container.longClick {
                onLongClick?.invoke(this@PlaylistAdapter, adapterPosition)

                // Contextual menu's need to not be consumed.
                layoutType == LAYOUT_CARD
            }
        }

        override fun onItemSelected() {
            itemView.setBackgroundColor(itemView.resources.color(R.color.gray))
        }

        override fun onItemClear() {
            itemView.setBackgroundColor(0)
        }
    }

    companion object {
        const val LAYOUT_LIST = 0
        const val LAYOUT_CARD = 1
        const val LAYOUT_DRAG = 2
    }
}
