package org.helllabs.android.xmp.modarchive.adapter

import android.annotation.SuppressLint
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.model.Module

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ModuleArrayAdapter(
        context: Context,
        resource: Int,
        items: List<Module>
) : ArrayAdapter<Module>(context, resource, items) {

    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            val inflater =
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item_search, null)
        }

        val module = getItem(position)
        if (module != null) {
            val fmt = view!!.findViewById<TextView>(R.id.search_list_fmt)
            val line1 = view.findViewById<TextView>(R.id.search_list_line1)
            val line2 = view.findViewById<TextView>(R.id.search_list_line2)
            val size = view.findViewById<TextView>(R.id.search_list_size)

            fmt.text = module.format
            line1.text = module.songTitle
            line2.text = context.getString(R.string.module_by, module.artist)
            size.text = context.getString(R.string.module_size, (module.bytes / 1024).toString())
        }

        return view!!
    }
}
