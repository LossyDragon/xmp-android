package org.helllabs.android.xmp.modarchive.adapter

import org.helllabs.android.xmp.modarchive.model.Artist

import android.content.Context
import android.widget.ArrayAdapter

class ArtistArrayAdapter(
        context: Context,
        resource: Int,
        items: List<Artist>
) : ArrayAdapter<Artist>(context, resource, items)
