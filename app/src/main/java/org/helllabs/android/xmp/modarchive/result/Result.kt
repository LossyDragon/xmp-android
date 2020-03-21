package org.helllabs.android.xmp.modarchive.result

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.modarchive.SearchError
import org.helllabs.android.xmp.util.Crossfader

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.extension.intent

abstract class Result : AppCompatActivity() {

    private var crossfader: Crossfader? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_search_result)
        crossfader = Crossfader(this)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    protected fun setupCrossfade() {
        crossfader!!.setup(R.id.result_content, R.id.result_spinner)
    }

    protected fun crossfade() {
        crossfader!!.crossfade()
    }

    protected fun handleError(error: Throwable) {
        startActivity(
                intent(SearchError::class.java).apply {
                    putExtra(Search.ERROR, error)
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                })
        overridePendingTransition(0, 0)
    }

    protected fun handleQueryError() {
        handleError(Throwable("Bad search string. "))
    }

    protected fun xmpApplication() = application as XmpApplication
}
