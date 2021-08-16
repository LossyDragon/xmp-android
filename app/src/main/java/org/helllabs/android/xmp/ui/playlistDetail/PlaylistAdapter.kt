package org.helllabs.android.xmp.ui.playlistDetail

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
import org.helllabs.android.xmp.ui.playlistDetail.recyclerview.ItemTouchHelperAdapter
import org.helllabs.android.xmp.ui.playlistDetail.recyclerview.ItemTouchHelperViewHolder
import org.helllabs.android.xmp.ui.playlistDetail.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.util.*

class PlaylistAdapter(
    var currentList: MutableList<PlaylistItem>,
    private var useFilename: Boolean
) :
    RecyclerView.Adapter<PlaylistAdapter.ListViewHolder>(),
    ItemTouchHelperAdapter {

    var dragListener: OnStartDragListener? = null
    var onClick: ((position: Int) -> Unit)? = null
    var onLongClick: ((position: Int) -> Unit)? = null

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
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                currentList[i] = currentList.set(i + 1, currentList[i])
            }
        } else {
            for (i in fromPosition..toPosition + 1) {
                currentList[i] = currentList.set(i - 1, currentList[i])
            }
        }

        logD("onItemMove(from=$fromPosition, to=$toPosition)")
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update() {
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
            val title = if (useFilename) item.file!!.name else item.name
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
                onClick?.invoke(bindingAdapterPosition)
            }
            layout.longClick {
                onLongClick?.invoke(bindingAdapterPosition)
                true
            }
        }

        override fun onItemSelected() {
            layout.setBackgroundColor(Color.LTGRAY)
        }

        override fun onItemClear() {
            layout.setBackgroundColor(0)
            dragListener?.onStopDrag(currentList.toList())
        }
    }
}
