package org.helllabs.android.xmp.browser.playlist

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder
import java.io.File
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.show

class PlaylistAdapter :
    RecyclerView.Adapter<PlaylistAdapter.ViewHolder>,
    DraggableItemAdapter<PlaylistAdapter.ViewHolder> {

    private val typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val playlist: Playlist?
    private val items: MutableList<PlaylistItem>
    private val context: Context
    private var useFilename: Boolean
    var position = 0
    private var onItemClickListener: OnItemClickListener? = null
    private val layoutType: Int

    interface OnItemClickListener {
        fun onItemClick(adapter: PlaylistAdapter, view: View, position: Int)
    }

    class ViewHolder(itemView: View, adapter: PlaylistAdapter) :
        AbstractDraggableItemViewHolder(itemView),
        View.OnClickListener {
        val container: View
        val handle: View?
        val titleText: TextView
        val infoText: TextView
        val image: ImageView
        private var onItemClickListener: OnItemClickListener? = null
        private val adapter: PlaylistAdapter

        init {
            itemView.setOnClickListener(this)
            container = itemView.findViewById(R.id.plist_container)
            handle = itemView.findViewById(R.id.plist_handle)
            titleText = itemView.findViewById<View>(R.id.plist_title) as TextView
            infoText = itemView.findViewById<View>(R.id.plist_info) as TextView
            image = itemView.findViewById<View>(R.id.plist_image) as ImageView
            this.adapter = adapter
        }

        fun setOnItemClickListener(listener: OnItemClickListener?) {
            onItemClickListener = listener
        }

        override fun onClick(view: View) {
            if (onItemClickListener != null) {
                onItemClickListener!!.onItemClick(adapter, view, adapterPosition)
            }
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout: Int = when (layoutType) {
            LAYOUT_CARD -> R.layout.item_playlist_card
            else -> R.layout.playlist_item
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

        // Advanced RecyclerView
        // set background resource (target view ID: container)
        /*
        final int dragState = holder.getDragStateFlags();

        if (((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_UPDATED) != 0)) {
            final int bgResId;

            if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_ACTIVE) != 0) {
                bgResId = R.drawable.bg_item_dragging_active_state;
            } else if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_DRAGGING) != 0) {
                bgResId = R.drawable.bg_item_dragging_state;
            } else {
                bgResId = R.drawable.bg_item_normal_state;
            }

            holder.container.setBackgroundResource(bgResId);
        }*/
    }

    constructor(
        context: Context,
        items: MutableList<PlaylistItem>,
        useFilename: Boolean,
        layoutType: Int
    ) {
        playlist = null
        this.items = items
        this.context = context
        this.useFilename = useFilename
        this.layoutType = layoutType

        // DraggableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true)
    }

    constructor(context: Context, playlist: Playlist, useFilename: Boolean, layoutType: Int) {
        this.playlist = playlist
        items = playlist.list
        this.context = context
        this.useFilename = useFilename
        this.layoutType = layoutType

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

    // public List<PlaylistItem> getItems() {
    // 	return items;
    // }
    fun getFilename(location: Int): String {
        return items[location].file!!.path
    }

    fun getFile(location: Int): File {
        return items[location].file!!
    }

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

    fun addList(list: List<PlaylistItem>) {
        items.addAll(list)
    }

    // Advanced RecyclerView
    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        // Log.d(TAG, "onMoveItem(fromPosition = " + fromPosition + ", toPosition = " + toPosition + ")");
        if (fromPosition == toPosition) {
            return
        }
        val item = items[fromPosition]
        items.remove(item)
        items.add(toPosition, item)
        // playlist.setListChanged(true);
        notifyItemMoved(fromPosition, toPosition)
        playlist?.setListChanged(true)
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean {
        // x, y --- relative from the itemView's top-left
        val containerView = holder.container
        val dragHandleView = holder.handle
        val offsetX = containerView.left + (containerView.translationX + 0.5f).toInt()
        return hitTest(dragHandleView!!, x - offsetX, y)
    }

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange? {
        // no drag-sortable range specified
        return null
    }

    companion object {
        const val LAYOUT_LIST = 0
        const val LAYOUT_CARD = 1
        const val LAYOUT_DRAG = 2

        private fun hitTest(v: View, x: Int, y: Int): Boolean {
            val tx = (v.translationX + 0.5f).toInt()
            val ty = (v.translationY + 0.5f).toInt()
            val left = v.left + tx
            val right = v.right + tx
            val top = v.top + ty
            val bottom = v.bottom + ty
            return x in left..right && y >= top && y <= bottom
        }
    }
}
