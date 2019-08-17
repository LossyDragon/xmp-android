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

class ModuleArrayAdapter(context: Context, resource: Int, items: List<Module>) : ArrayAdapter<Module>(context, resource, items) {

    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.search_list_item, null)
        }

        val module = getItem(position)
        if (module != null) {
            val fmt = view!!.findViewById<View>(R.id.search_list_fmt) as TextView
            val line1 = view.findViewById<View>(R.id.search_list_line1) as TextView
            val line2 = view.findViewById<View>(R.id.search_list_line2) as TextView
            val size = view.findViewById<View>(R.id.search_list_size) as TextView

            fmt.text = module.format
            line1.text = module.songTitle
            line2.text = String.format(context.getString(R.string.module_by), module.artist)
            size.text = String.format(context.getString(R.string.module_size), (module.bytes / 1024).toString())
        }

        return view!!
    }


}
