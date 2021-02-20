package org.helllabs.android.xmp.modarchive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.History
import org.helllabs.android.xmp.util.click

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    interface HistoryAdapterListener {
        fun onClick(id: Int)
    }

    var historyListener: HistoryAdapterListener? = null
    val format by lazy { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var historySet: List<History> = listOf()
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_search_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(historySet[position])
    }

    override fun getItemCount(): Int = historySet.size

    private fun formatDate(visitDate: Long): String {
        return format.format(Date(visitDate))
    }

    fun submitList(set: List<History>) {
        historySet = set
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val view: View
    ) : RecyclerView.ViewHolder(view) {
        fun onBind(item: History) = with(view) {
            findViewById<TextView>(R.id.search_list_fmt).text = item.format
            findViewById<TextView>(R.id.search_list_line1).text = item.songTitle
            findViewById<TextView>(R.id.search_list_line2).text = item.artist
            findViewById<TextView>(R.id.search_list_size).text = formatDate(item.visitDate)
            click { historyListener?.onClick(item.id) }
        }
    }
}
