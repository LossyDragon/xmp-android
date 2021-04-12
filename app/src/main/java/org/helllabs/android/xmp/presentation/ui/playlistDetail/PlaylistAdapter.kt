package org.helllabs.android.xmp.presentation.ui.playlistDetail

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.presentation.utils.playlist.PlaylistUtils
import org.helllabs.android.xmp.presentation.utils.recyclerview.ItemTouchHelperAdapter
import org.helllabs.android.xmp.presentation.utils.recyclerview.ItemTouchHelperViewHolder
import org.helllabs.android.xmp.presentation.utils.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.util.click
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.longClick
import org.helllabs.android.xmp.util.touch

class PlaylistAdapter(
    var currentList: List<PlaylistItem>,
    private var useFilename: Boolean
) :
    RecyclerView.Adapter<PlaylistAdapter.ListViewHolder>(),
    ItemTouchHelperAdapter {

    var dragListener: OnStartDragListener? = null
    var onClick: ((position: Int) -> Unit)? = null
    var onLongClick: ((position: Int) -> Unit)? = null

    // Temp list to handle drag & drop
    private lateinit var tempList: MutableList<PlaylistItem>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_playlist, parent, false)

        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.onBind(currentList[position])
    }

    override fun getItemCount(): Int = currentList.size

    override fun getItemId(position: Int): Long = currentList[position].id.toLong()

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(tempList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PlaylistItem>) {
        currentList = list
        notifyDataSetChanged()
    }

    fun getFilename(location: Int): String = currentList[location].file!!.path

    fun setUseFilename(useFilename: Boolean) {
        this.useFilename = useFilename
    }

    inner class ListViewHolder(
        val view: View
    ) : RecyclerView.ViewHolder(view), ItemTouchHelperViewHolder {
        val layout: LinearLayout = view.findViewById(R.id.itemLayout)

        fun onBind(item: PlaylistItem) = with(view) {
            val title = if (useFilename) item.filename else item.name
            findViewById<TextView>(R.id.itemTitle).text = title
            findViewById<TextView>(R.id.itemInfo).text = item.comment
            findViewById<AppCompatImageView>(R.id.handle).let { handle ->
                handle.touch { _, event ->
                    logD("Touch: ${event.actionMasked}")
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        dragListener?.onStartDrag(this@ListViewHolder)
                    }
                    true // Continue to consume the touch event.
                }
            }
            layout.click {
                onClick?.invoke(adapterPosition)
            }
            layout.longClick {
                onLongClick?.invoke(adapterPosition)
                true
            }
        }

        override fun onItemSelected() {
            layout.setBackgroundColor(Color.LTGRAY)
            tempList = currentList.toMutableList()
        }

        override fun onItemClear() {
            PlaylistUtils.renumberIds(tempList)
            layout.setBackgroundColor(0)
            dragListener?.onStopDrag(tempList)
        }
    }
}
