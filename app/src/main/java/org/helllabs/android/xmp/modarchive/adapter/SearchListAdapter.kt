package org.helllabs.android.xmp.modarchive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.helllabs.android.xmp.R
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

    interface SearchListListener {
        fun onClick(id: Int)
    }

    var searchListListener: SearchListListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_search_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    inner class ViewHolder(
        val view: View
    ) : RecyclerView.ViewHolder(view) {
        fun onBind(item: Module) = with(view) {
            val artist = context.getString(R.string.by_artist, item.getArtist())
            val bytes = resources.getString(R.string.size_kb, (item.bytes?.div(1024) ?: 0))
            findViewById<TextView>(R.id.search_list_fmt).text = item.format
            findViewById<TextView>(R.id.search_list_line1).text = item.getSongTitle()
            findViewById<TextView>(R.id.search_list_line2).text = artist
            findViewById<TextView>(R.id.search_list_size).text = bytes
            itemView.click {
                searchListListener?.onClick(item.id!!)
            }
        }
    }
}
