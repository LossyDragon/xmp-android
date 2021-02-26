package org.helllabs.android.xmp.browser.playlist

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.recyclerview.ItemTouchHelperAdapter
import org.helllabs.android.xmp.util.recyclerview.ItemTouchHelperViewHolder
import org.helllabs.android.xmp.util.recyclerview.OnStartDragListener

class PlaylistAdapter(
    private var layoutType: Int,
    private var useFilename: Boolean,
) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>(),
    ItemTouchHelperAdapter {

    companion object {
        const val LAYOUT_LIST = 0
        const val LAYOUT_CARD = 1
        const val LAYOUT_DRAG = 2
    }

    val filenameList: List<String>
        get() {
            val list: MutableList<String> = ArrayList()
            for (item in playlist) {
                if (item.type == PlaylistItem.TYPE_FILE) {
                    list.add(item.file!!.path)
                }
            }
            return list
        }

    val directoryCount: Int
        get() {
            var count = 0
            for (item in playlist) {
                if (item.type != PlaylistItem.TYPE_DIRECTORY) {
                    break
                }
                count++
            }
            return count
        }

    var playlist: MutableList<PlaylistItem> = LinkedList()
        private set

    var isDraggable: Boolean = false
        private set

    var dragListener: OnStartDragListener? = null
    private val typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    var onClick: ((position: Int) -> Unit)? = null
    var onLongClick: ((position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val layout: Int = when (layoutType) {
            LAYOUT_CARD -> R.layout.item_playlist_card
            LAYOUT_DRAG, LAYOUT_LIST -> R.layout.item_playlist
            else -> throw IllegalArgumentException("Wrong layout type defined $layoutType")
        }

        return ViewHolder(inflater.inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(playlist[position])
    }

    override fun getItemId(position: Int): Long = playlist[position].id.toLong()

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(playlist, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)

        return true
    }

    override fun getItemCount(): Int = playlist.size

    fun onSwap(list: List<PlaylistItem>?) {

        // Seems to stop flickering on resume.
        val newList: MutableList<PlaylistItem> =
            if (list == null) mutableListOf() else LinkedList(list)

        val result = DiffUtil.calculateDiff(PlaylistDiffUtil(newList))
        playlist = newList
        result.dispatchUpdatesTo(this)
    }

    fun getItems(): List<PlaylistItem> = playlist

    fun getFilename(location: Int): String = playlist[location].file!!.path

    fun getFile(location: Int): File = playlist[location].file!!

    fun setUseFilename(useFilename: Boolean) {
        this.useFilename = useFilename
    }

    inner class ViewHolder(
        val view: View
    ) : RecyclerView.ViewHolder(view),
        ItemTouchHelperViewHolder {

        private val container: View = view.findViewById(R.id.plist_container)
        private val handle: View? = view.findViewById(R.id.plist_handle)
        private val titleText: TextView = view.findViewById(R.id.plist_title)
        private val infoText: TextView = view.findViewById(R.id.plist_info)
        private val image: ImageView = view.findViewById(R.id.plist_image)

        fun onBind(item: PlaylistItem) = with(view) {

            // Drag
            if (layoutType == LAYOUT_DRAG) {
                handle?.let { handle ->
                    handle.show()
                    handle.touch { _, event ->
                        logD("Touch: ${event.actionMasked}")
                        isDraggable =
                            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                                dragListener?.onStartDrag(this@ViewHolder)
                                true
                            } else {
                                false
                            }
                        true // Continue to consume the touch event.
                    }
                }
            }

            if (item.type == PlaylistItem.TYPE_DIRECTORY) {
                infoText.setTypeface(typeface, Typeface.ITALIC)
            } else {
                infoText.setTypeface(typeface, Typeface.NORMAL)
            }

            infoText.text = item.comment
            titleText.text = if (useFilename) item.filename else item.name
            image.setImageResource(item.imageRes)

            container.click {
                onClick?.invoke(adapterPosition)
            }

            container.longClick {
                onLongClick?.invoke(adapterPosition)
                true
            }
        }

        override fun onItemSelected() {
            view.setBackgroundColor(Color.LTGRAY)
            dragListener?.disableSwipe(true)
        }

        override fun onItemClear() {
            PlaylistUtils.renumberIds(playlist)
            view.setBackgroundColor(0)

            dragListener?.onStopDrag(playlist)
            dragListener?.disableSwipe(false)
        }
    }

    inner class PlaylistDiffUtil(val list: MutableList<PlaylistItem>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            list[newItemPosition].id == playlist[oldItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            list[newItemPosition] == playlist[oldItemPosition]

        override fun getOldListSize() = playlist.size

        override fun getNewListSize() = list.size
    }
}
