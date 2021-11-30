package org.helllabs.android.xmp.ui.playlist_selected

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.ui.components.ItemList
import org.helllabs.android.xmp.ui.playlist_selected.recyclerview.ItemTouchHelperAdapter
import org.helllabs.android.xmp.ui.playlist_selected.recyclerview.ItemTouchHelperViewHolder
import org.helllabs.android.xmp.ui.playlist_selected.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.util.*

class PlaylistAdapter(
    var currentList: MutableList<PlaylistItem>,
    private var useFilename: Boolean
) : RecyclerView.Adapter<PlaylistAdapter.ComposedViewHolder>(),
    ItemTouchHelperAdapter {

    var dragListener: OnStartDragListener? = null
    var onClick: ((position: Int) -> Unit)? = null
    var onLongClick: ((position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposedViewHolder {
        return ComposedViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: ComposedViewHolder, position: Int) {
        holder.onBind(currentList[position])
    }

    override fun onViewRecycled(holder: ComposedViewHolder) {
        holder.composeView.disposeComposition()
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

    // https://developer.android.com/jetpack/compose/interop/compose-in-existing-ui#compose-recyclerview
    inner class ComposedViewHolder(
        val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView), ItemTouchHelperViewHolder {

        init {
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
        }

        fun onBind(item: PlaylistItem) {
            composeView.setContent {
                Surface {
                    ItemList(
                        item = item,
                        isDraggable = true,
                        onDrag = { value ->
                            if (value) {
                                dragListener?.onStartDrag(this@ComposedViewHolder)
                            }
                        },
                        onClick = {
                            onClick?.invoke(bindingAdapterPosition)
                        },
                        onLongClick = {
                            onLongClick?.invoke(bindingAdapterPosition)
                        }
                    )
                }
            }
        }

        override fun onItemSelected() {
        }

        override fun onItemClear() {
            dragListener?.onStopDrag(currentList.toList())
        }
    }
}
