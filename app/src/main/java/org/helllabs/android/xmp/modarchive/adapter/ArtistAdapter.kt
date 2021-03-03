package org.helllabs.android.xmp.modarchive.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.helllabs.android.xmp.databinding.ItemSingleBinding
import org.helllabs.android.xmp.model.Item
import org.helllabs.android.xmp.util.click

class ArtistAdapter : ListAdapter<Item, ArtistAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem == newItem
            }
        }
    }

    var onClick: ((id: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binder = ItemSingleBinding.inflate(inflater, parent, false)
        return ViewHolder(binder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    inner class ViewHolder(
        private val binder: ItemSingleBinding
    ) : RecyclerView.ViewHolder(binder.root) {
        fun onBind(item: Item) = with(binder) {
            itemText = item.alias
            itemView.click {
                onClick?.invoke(item.id!!)
            }
            executePendingBindings()
        }
    }
}
