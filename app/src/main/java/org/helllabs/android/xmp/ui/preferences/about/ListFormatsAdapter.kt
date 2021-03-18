package org.helllabs.android.xmp.ui.preferences.about

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.helllabs.android.xmp.databinding.ItemSingleBinding
import org.helllabs.android.xmp.util.longClick

class ListFormatsAdapter : ListAdapter<String, ListFormatsAdapter.ListViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }

    var onLongClick: ((item: String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binder = ItemSingleBinding.inflate(inflater, parent, false)
        return ListViewHolder(binder)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    inner class ListViewHolder(
        private val binder: ItemSingleBinding
    ) : RecyclerView.ViewHolder(binder.root) {
        fun onBind(item: String) = with(binder) {
            string = item
            binder.root.longClick {
                onLongClick?.invoke(item)
                true
            }
            executePendingBindings()
        }
    }
}
