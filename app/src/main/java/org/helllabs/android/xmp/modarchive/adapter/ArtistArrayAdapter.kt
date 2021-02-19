package org.helllabs.android.xmp.modarchive.adapter

import android.content.Context
import android.widget.ArrayAdapter
import org.helllabs.android.xmp.modarchive.model.Artist

class ArtistArrayAdapter(
    context: Context,
    resource: Int,
    items: List<Artist?>?
) : ArrayAdapter<Artist?>(context, resource, items!!)