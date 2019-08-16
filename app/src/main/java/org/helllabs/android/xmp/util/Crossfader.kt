package org.helllabs.android.xmp.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.Activity
import android.view.View

class Crossfader(private val activity: Activity) {
    private val animationDuration: Int
    private var contentView: View? = null
    private var progressView: View? = null


    init {
        this.animationDuration = activity.resources.getInteger(android.R.integer.config_shortAnimTime)
    }

    fun setup(contentRes: Int, spinnerRes: Int) {
        contentView = activity.findViewById(contentRes)
        progressView = activity.findViewById(spinnerRes)
        contentView!!.visibility = View.GONE
    }

    @TargetApi(12)
    fun crossfade() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {

            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            contentView!!.alpha = 0f
            contentView!!.visibility = View.VISIBLE

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view.
            contentView!!.animate()
                    .alpha(1f)
                    .setDuration(animationDuration.toLong())
                    .setListener(null)

            // Animate the loading view to 0% opacity. After the animation ends,
            // set its visibility to GONE as an optimization step (it won't
            // participate in layout passes, etc.)
            progressView!!.animate()
                    .alpha(0f)
                    .setDuration(animationDuration.toLong())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            progressView!!.visibility = View.GONE
                        }
                    })
        } else {
            progressView!!.visibility = View.GONE
            contentView!!.visibility = View.VISIBLE
        }
    }

}
