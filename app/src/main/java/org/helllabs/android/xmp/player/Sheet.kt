package org.helllabs.android.xmp.player

import android.annotation.SuppressLint
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.Log

import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.layout_bottom_sheet.*
import kotlinx.android.synthetic.main.layout_player_controls.*

class Sheet(private val activity: PlayerActivity) {
    private val seqGroupListener: RadioGroup.OnCheckedChangeListener

    private var sheet: BottomSheetBehavior<*>
    var isDragLocked: Boolean = false
    var isDragStaying: Boolean = false
    var state: Int? = null

    init {
        activity.apply {
            layoutInflater.inflate(R.layout.layout_player, content_view, true)
            layoutInflater.inflate(R.layout.layout_player_controls, sidebar_view, true)

            sidebar_allseqs_switch.apply {
                isChecked = allSequences
                setOnClickListener {
                    isChecked = toggleAllSequences()
                }
            }

            seqGroupListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
                playNewSequence(checkedId)
            }

            sidebar_sequences.setOnCheckedChangeListener(seqGroupListener)
        }

        sheet = BottomSheetBehavior.from(activity.player_sheet)
        sheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Not used
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING && isDragLocked) {
                    if (isDragStaying) {
                        sheet.state = state!!
                    } else {
                        sheet.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }
        })
    }

    fun setDragLock(dragLock: Boolean, stayLock: Boolean) {
        isDragLocked = dragLock
        isDragStaying = stayLock

        if (isDragLocked && !isDragStaying && sheet.state != BottomSheetBehavior.STATE_COLLAPSED)
            sheet.state = BottomSheetBehavior.STATE_COLLAPSED
        else
            state = sheet.state
    }

    fun setDetails(numPat: Int, numIns: Int, numSmp: Int, numChn: Int, allSequences: Boolean) {
        activity.apply {
            sidebar_num_pat.text = numPat.toString()
            sidebar_num_ins.text = numIns.toString()
            sidebar_num_smp.text = numSmp.toString()
            sidebar_num_chn.text = numChn.toString()
            sidebar_allseqs_switch.isChecked = allSequences
        }
    }

    fun clearSequences() = activity.sidebar_sequences.removeAllViews()

    @SuppressLint("InflateParams")
    fun addSequence(num: Int, duration: Int) {
        // Can't get it styled this way, see http://stackoverflow.com/questions/3142067/android-set-style-in-code
        val button = activity.layoutInflater.inflate(R.layout.item_sequence, null) as RadioButton

        val main = activity.getString(R.string.sheet_main_song)
        val sub = activity.getString(R.string.sheet_sub_song)

        val text = if (num == 0) main else sub + num
        button.text = String.format("%2d:%02d (%s)", duration / 60000, duration / 1000 % 60, text)
        button.id = num

        val layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        activity.sidebar_sequences.addView(button, num, layoutParams)
    }

    fun selectSequence(num: Int) {
        activity.apply {
            sidebar_sequences.setOnCheckedChangeListener(null)

            Log.i(TAG, "Select sequence $num")
            sidebar_sequences.check(-1) // force redraw
            sidebar_sequences.check(num)
            sidebar_sequences.setOnCheckedChangeListener(seqGroupListener)
        }
    }

    companion object {
        private val TAG = Sheet::class.java.simpleName
    }
}
