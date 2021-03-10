package org.helllabs.android.xmp.modarchive.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.helllabs.android.xmp.BR
import org.helllabs.android.xmp.model.Item
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.util.click

class ModAdapter<T : Any, VB : ViewDataBinding>(
    diffUtil: DiffUtil.ItemCallback<T>,
    @LayoutRes val layoutId: Int,
    private val onClick: (T) -> Unit
) : ListAdapter<T, ModAdapter.ListViewHolder<VB>>(diffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder<VB> {
        val inflater = LayoutInflater.from(parent.context)
        return ListViewHolder<VB>(
            DataBindingUtil.inflate(
                inflater,
                layoutId,
                parent,
                false
            )
        ).apply {
            binder.root.click {
                onClick.invoke(getItem(adapterPosition))
            }
        }
    }

    override fun onBindViewHolder(holder: ListViewHolder<VB>, position: Int) {
        holder.onBind(getItem(position))
    }

    open class ListViewHolder<VB : ViewDataBinding>(val binder: VB) :
        RecyclerView.ViewHolder(binder.root) {
        fun onBind(item: Any) = with(binder) {
            setVariable(BR.item, item)
            executePendingBindings()
        }
    }
}

class ArtistDiffUtil : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
        oldItem == newItem
}

class SearchDiffUtil : DiffUtil.ItemCallback<Module>() {
    override fun areItemsTheSame(oldItem: Module, newItem: Module): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Module, newItem: Module): Boolean =
        oldItem == newItem
}
