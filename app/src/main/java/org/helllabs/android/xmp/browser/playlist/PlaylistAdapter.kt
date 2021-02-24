package org.helllabs.android.xmp.browser.playlist

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.recyclerview.ItemTouchHelperAdapter
import org.helllabs.android.xmp.util.recyclerview.ItemTouchHelperViewHolder
import org.helllabs.android.xmp.util.recyclerview.OnStartDragListener

class PlaylistAdapter(
    private var layoutType: Int,
    private var useFilename: Boolean,
) : ListAdapter<PlaylistItem, PlaylistAdapter.ViewHolder>(DIFF_CALLBACK),
    ItemTouchHelperAdapter {

    companion object {
        const val LAYOUT_LIST = 0
        const val LAYOUT_CARD = 1
        const val LAYOUT_DRAG = 2

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PlaylistItem>() {
            override fun areItemsTheSame(oldItem: PlaylistItem, newItem: PlaylistItem): Boolean {
                return oldItem.id == newItem.id && oldItem.name == newItem.name
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

    private val typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    var onClick: ((position: Int) -> Unit)? = null
    var onLongClick: ((position: Int) -> Unit)? = null

    init {
        setHasStableIds(true)
    }

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
        holder.onBind(currentList[position])
    }

    override fun getItemId(position: Int): Long = currentList[position].id.toLong()

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        // Temp it first to avoid crashing.
        val tempList = currentList
        Collections.swap(tempList, fromPosition, toPosition)
        submitList(tempList)

        return true
    }

    fun getItems(): List<PlaylistItem> = currentList

    fun getFilename(location: Int): String = currentList[location].file!!.path

    fun getFile(location: Int): File = currentList[location].file!!

    fun setUseFilename(useFilename: Boolean) {
        this.useFilename = useFilename
    }

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView),
        ItemTouchHelperViewHolder {
        fun onBind(item: PlaylistItem) = with(itemView) {

            val container: View = findViewById(R.id.plist_container)
            val handle: View? = findViewById(R.id.plist_handle)
            val titleText: TextView = findViewById(R.id.plist_title)
            val infoText: TextView = findViewById(R.id.plist_info)
            val image: ImageView = findViewById(R.id.plist_image)

            // Drag
            if (handle != null && handle.isVisible) {
                handle.touch { _, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        dragListener?.onStartDrag(this@ViewHolder)
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
                onClick?.invoke(adapterPosition)
            }

            container.longClick {
                onLongClick?.invoke(adapterPosition)
                true
            }
        }

        override fun onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY)
        }

        override fun onItemClear() {
            itemView.setBackgroundColor(0)
        }
    }
}
