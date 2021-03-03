package org.helllabs.android.xmp.modarchive.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.helllabs.android.xmp.databinding.ItemSearchListBinding
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.util.click

class SearchListAdapter : ListAdapter<Module, SearchListAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Module>() {
            override fun areItemsTheSame(oldItem: Module, newItem: Module): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Module, newItem: Module): Boolean {
                return oldItem == newItem
            }
        }
    }

    var onClick: ((id: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binder = ItemSearchListBinding.inflate(inflater, parent, false)
        return ViewHolder(binder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    inner class ViewHolder(
        private val binder: ItemSearchListBinding
    ) : RecyclerView.ViewHolder(binder.root) {
        fun onBind(item: Module) = with(binder) {
            module = item
            root.click {
                onClick?.invoke(item.id!!)
            }
            executePendingBindings()
        }
    }
}
