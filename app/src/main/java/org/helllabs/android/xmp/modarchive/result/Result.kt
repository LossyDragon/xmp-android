package org.helllabs.android.xmp.modarchive.result

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.modarchive.SearchError
import org.helllabs.android.xmp.util.Crossfader

abstract class Result : AppCompatActivity() {

    private var crossfader: Crossfader? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.search_result_title)
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

    protected fun handleError(error: Throwable?) {
        val intent = Intent(this, SearchError::class.java)
        intent.putExtra(Search.Companion.ERROR, error)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    protected fun handleQueryError() {
        handleError(Throwable("Bad search string. "))
    }

    companion object {
        var apiKey = BuildConfig.API_KEY
    }
}
