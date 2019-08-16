package org.helllabs.android.xmp.preferences

/* The following code was written by Matthew Wiggins
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import org.helllabs.android.xmp.R


class SeekBarPreference(private val mContext: Context, attrs: AttributeSet) : DialogPreference(mContext, attrs), SeekBar.OnSeekBarChangeListener {
    private var mSeekBar: SeekBar? = null
    private var mValueText: TextView? = null

    private val mDialogMessage: String?
    private val mSuffix: String?
    private val mDefault: Int
    var max: Int = 0
    private var mValue: Int = 0

    var progress: Int
        get() = mValue
        set(progress) {
            mValue = progress
            if (mSeekBar != null) {
                mSeekBar!!.progress = progress
            }
        }

    init {

        val styledAttrs = mContext.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference)
        mDialogMessage = styledAttrs.getString(R.styleable.SeekBarPreference_android_dialogMessage)
        mSuffix = styledAttrs.getString(R.styleable.SeekBarPreference_android_text)
        mDefault = styledAttrs.getInt(R.styleable.SeekBarPreference_android_defaultValue, 0)
        max = styledAttrs.getInt(R.styleable.SeekBarPreference_android_max, 100)
        styledAttrs.recycle()
    }

    override fun onCreateDialogView(): View {
        val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        val layout = LinearLayout(mContext)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(6, 6, 6, 6)

        val splashText = TextView(mContext)
        if (mDialogMessage != null) {
            splashText.text = mDialogMessage
        }
        layout.addView(splashText)

        mValueText = TextView(mContext)
        mValueText!!.gravity = Gravity.CENTER_HORIZONTAL
        mValueText!!.textSize = 32f
        layout.addView(mValueText, params)

        mSeekBar = SeekBar(mContext)
        mSeekBar!!.setOnSeekBarChangeListener(this)
        layout.addView(mSeekBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        if (shouldPersist()) {
            mValue = getPersistedInt(mDefault)
        }

        mSeekBar!!.max = max
        mSeekBar!!.progress = mValue

        return layout
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        mSeekBar!!.max = max
        mSeekBar!!.progress = mValue
    }

    override fun onSetInitialValue(restore: Boolean, defaultValue: Any) {
        super.onSetInitialValue(restore, defaultValue)
        mValue = if (restore) {
            if (shouldPersist()) getPersistedInt(mDefault) else 0
        } else {
            defaultValue as Int
        }
    }

    override fun onProgressChanged(seek: SeekBar, value: Int, fromTouch: Boolean) {
        val str = value.toString()
        mValueText!!.text = if (mSuffix == null) str else str + mSuffix
        if (shouldPersist()) {
            persistInt(value)
        }
        callChangeListener(value)
    }

    override fun onStartTrackingTouch(seek: SeekBar) {
        // do nothing
    }

    override fun onStopTrackingTouch(seek: SeekBar) {
        // do nothing
    }
}