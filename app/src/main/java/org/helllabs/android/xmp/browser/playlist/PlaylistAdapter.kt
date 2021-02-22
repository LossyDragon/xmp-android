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
import org.helllabs.android.xmp.util.color
import org.helllabs.android.xmp.util.recyclerview.ItemTouchHelperAdapter
import org.helllabs.android.xmp.util.recyclerview.ItemTouchHelperViewHolder
import org.helllabs.android.xmp.util.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.util.show
import org.helllabs.android.xmp.util.touch

class PlaylistAdapter : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>, ItemTouchHelperAdapter {

    var position = 0

    private var mDragStartListener: OnStartDragListener? = null
    private val typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val playlist: Playlist?
    private val items: MutableList<PlaylistItem>
    private var useFilename: Boolean
    private var onItemClickListener: OnItemClickListener? = null
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

    interface OnItemClickListener {
        fun onItemClick(adapter: PlaylistAdapter, view: View, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout: Int = when (layoutType) {
            LAYOUT_CARD -> R.layout.item_playlist_card
            else -> R.layout.item_playlist
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        val holder = ViewHolder(view, this)
        holder.setOnItemClickListener(onItemClickListener)
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val item = items[pos]
        val imageRes = item.imageRes
        val type = item.type
        if (type == PlaylistItem.TYPE_DIRECTORY) {
            holder.infoText.setTypeface(typeface, Typeface.ITALIC)
        } else {
            holder.infoText.setTypeface(typeface, Typeface.NORMAL)
        }
        holder.titleText.text = if (useFilename) item.filename else item.name
        holder.infoText.text = item.comment
        if (imageRes > 0) {
            holder.image.setImageResource(imageRes)
            holder.image.visibility = View.VISIBLE
        } else {
            holder.image.visibility = View.GONE
        }
        if (layoutType == LAYOUT_DRAG) {
            holder.handle?.show()
            // holder.handle?.setBackgroundColor(context.resources.color(R.color.drag_handle_color))
            // holder.image.setAlpha(0.5f);
        }

        // See http://stackoverflow.com/questions/26466877/how-to-create-context-menu-for-recyclerview
        holder.itemView.setOnLongClickListener {
            position = holder.adapterPosition
            false
        }
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

    override fun onViewRecycled(holder: ViewHolder) {
        holder.itemView.setOnLongClickListener(null)
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].id.toLong()
    }

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

    inner class ViewHolder(itemView: View, private val playlistAdapter: PlaylistAdapter) :
        RecyclerView.ViewHolder(itemView),
        ItemTouchHelperViewHolder,
        View.OnClickListener {

        val container: View = itemView.findViewById(R.id.plist_container)
        val handle: View? = itemView.findViewById(R.id.plist_handle)
        val titleText: TextView = itemView.findViewById(R.id.plist_title)
        val infoText: TextView = itemView.findViewById(R.id.plist_info)
        val image: ImageView = itemView.findViewById(R.id.plist_image)
        private var onItemClickListener: OnItemClickListener? = null

        init {
            itemView.setOnClickListener(this)
            if (handle != null && handle.isVisible) {
                handle.touch { _, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        mDragStartListener?.onStartDrag(this)
                    }
                    false
                }
            }
        }

        fun setOnItemClickListener(listener: OnItemClickListener?) {
            onItemClickListener = listener
        }

        override fun onClick(view: View) {
            onItemClickListener?.onItemClick(playlistAdapter, view, adapterPosition)
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
