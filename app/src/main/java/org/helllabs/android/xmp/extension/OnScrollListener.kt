package org.helllabs.android.xmp.extension

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton as ExFab

class OnScrollListener(val view: ExFab) : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy > 0 && view.visibility == View.VISIBLE) {
            view.hide()
        } else if (dy < 0 && view.visibility != View.VISIBLE) {
            view.show()
        }
    }
}
