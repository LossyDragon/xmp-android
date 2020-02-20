package org.helllabs.android.xmp.browser.playlist

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.extension.*
import org.helllabs.android.xmp.util.Log
import java.io.File
import java.util.*

class PlaylistAdapter :
        RecyclerView.Adapter<PlaylistAdapter.ViewHolder>,
        DraggableItemAdapter<PlaylistAdapter.ViewHolder> {

    constructor(
            context: Context,
            items: MutableList<PlaylistItem>,
            useFilename: Boolean,
            layoutType: Int
    ) {
        this.playlist = null
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
        this.items = playlist.list.toMutableList()
        this.context = context
        this.useFilename = useFilename
        this.layoutType = layoutType

        // DraggableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true)
    }

    var clickListener: OnItemClickListener? = null

    val items: MutableList<PlaylistItem>
    var position: Int = 0

    private val typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val playlist: Playlist?
    private val context: Context
    private var useFilename: Boolean = false
    private val layoutType: Int

    val filenameList: List<String>
        get() {
            val list = ArrayList<String>()
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
        val layout: Int =
                when (layoutType) {
                    LAYOUT_CARD -> R.layout.item_playlist_card
                    LAYOUT_DRAG -> R.layout.item_playlist_drag
                    else -> R.layout.item_playlist
                }

        val itemView: View = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(itemView, this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items[position].let { holder.onBind(it) }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].id.toLong()

    fun getItem(num: Int): PlaylistItem = items[num]

    fun getFilename(location: Int): String = items[location].file!!.path

    fun getFile(location: Int): File? = items[location].file

    fun clear() {
        items.clear()
        Log.i(TAG, "Playlist list cleared")
    }

    fun add(item: PlaylistItem) {
        items.add(item)
    }

    fun setUseFilename(useFilename: Boolean) {
        this.useFilename = useFilename
    }

    fun addList(list: List<PlaylistItem>) {
        items.addAll(list)
    }

    //region [region] Advanced RecyclerView
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
        // final int offsetY = containerView.getTop() + (int) (ViewCompat.getTranslationY(containerView) + 0.5f);

        return hitTest(dragHandleView!!, x - offsetX, y /*- offsetY*/)
    }

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange? {
        // no drag-sortable range specified
        return null
    }

    override fun onItemDragStarted(position: Int) {
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean {
        return false
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        playlist?.writeMovedList(playlistItem = items)
    }
    //endregion

    inner class ViewHolder(
            itemView: View,
            val adapter: PlaylistAdapter
    ) : AbstractDraggableItemViewHolder(itemView) {

        val container: View = itemView.findViewById(R.id.plist_container)
        val handle: FrameLayout? = itemView.findViewById(R.id.plist_handle)
        private val titleText: TextView = itemView.findViewById(R.id.plist_title)
        private val infoText: TextView = itemView.findViewById(R.id.plist_info)
        private val image: ImageView = itemView.findViewById(R.id.plist_image)

        fun onBind(item: PlaylistItem) {

            if (item.type == PlaylistItem.TYPE_DIRECTORY) {
                infoText.italic()
            } else {
                infoText.normal()
            }

            titleText.text = if (useFilename) item.filename else item.name
            infoText.text = item.comment

            if (item.imageRes > 0) {
                image.setImageResource(item.imageRes)
                image.show()
            } else {
                image.hide()
            }

            itemView.click {
                clickListener?.onItemClick(adapter, it, adapterPosition)
            }

            itemView.longClick {
                clickListener?.onItemLongClick(adapter, it, adapterPosition)
                true
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(adapter: PlaylistAdapter, view: View, position: Int)
        fun onItemLongClick(adapter: PlaylistAdapter, view: View, position: Int)
    }

    companion object {
        const val LAYOUT_LIST = 0
        const val LAYOUT_CARD = 1
        const val LAYOUT_DRAG = 2

        private val TAG = PlaylistAdapter::class.java.simpleName

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
