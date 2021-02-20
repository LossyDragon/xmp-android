package org.helllabs.android.xmp.modarchive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.helllabs.android.xmp.R
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

    interface ArtistAdapterListener {
        fun onClick(id: Int)
    }

    var artistAdapterListener: ArtistAdapterListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_single, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    inner class ViewHolder(
        private val view: View
    ) : RecyclerView.ViewHolder(view) {
        fun onBind(item: Item) = with(view) {
            findViewById<TextView>(R.id.singleItem).text = item.alias
            itemView.click {
                artistAdapterListener?.onClick(item.id!!)
            }
        }
    }
}
