package org.helllabs.android.xmp.player

import android.annotation.SuppressLint
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.Log

import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior

class Sheet(private val activity: PlayerActivity) {
    private val numPatText: TextView
    private val numInsText: TextView
    private val numSmpText: TextView
    private val numChnText: TextView
    private val allSequencesSwitch: Switch
    private val seqGroup: RadioGroup
    private val seqGroupListener: RadioGroup.OnCheckedChangeListener

    private var sheet: BottomSheetBehavior<*>
    var isDragLocked: Boolean = false

    init {
        val contentView: LinearLayout = activity.findViewById(R.id.content_view)
        activity.layoutInflater.inflate(R.layout.layout_player, contentView, true)

        val sidebarView: LinearLayout = activity.findViewById(R.id.sidebar_view)
        activity.layoutInflater.inflate(R.layout.layout_player_controls, sidebarView, true)

        numPatText = activity.findViewById(R.id.sidebar_num_pat)
        numInsText = activity.findViewById(R.id.sidebar_num_ins)
        numSmpText = activity.findViewById(R.id.sidebar_num_smp)
        numChnText = activity.findViewById(R.id.sidebar_num_chn)
        allSequencesSwitch = activity.findViewById(R.id.sidebar_allseqs_switch)
        seqGroup = activity.findViewById(R.id.sidebar_sequences)

        allSequencesSwitch.isChecked = activity.allSequences
        allSequencesSwitch.setOnClickListener {
            allSequencesSwitch.isChecked = activity.toggleAllSequences()
        }

        seqGroupListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
            activity.playNewSequence(checkedId)
        }
        seqGroup.setOnCheckedChangeListener(seqGroupListener)

        // TODO: Give indication the bottom sheet is present.
        sheet = BottomSheetBehavior.from(activity.findViewById<LinearLayout>(R.id.player_sheet))
        sheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Not used
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING && isDragLocked)
                    sheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        })
    }

    fun setDragLock(boolean: Boolean) {
        isDragLocked = boolean

        if (isDragLocked && sheet.state != BottomSheetBehavior.STATE_COLLAPSED)
            sheet.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun setDetails(numPat: Int, numIns: Int, numSmp: Int, numChn: Int, allSequences: Boolean) {
        numPatText.text = numPat.toString()
        numInsText.text = numIns.toString()
        numSmpText.text = numSmp.toString()
        numChnText.text = numChn.toString()
        allSequencesSwitch.isChecked = allSequences
    }

    fun clearSequences() = seqGroup.removeAllViews()

    @SuppressLint("InflateParams")
    fun addSequence(num: Int, duration: Int) {
        // final RadioButton button = new RadioButton(activity);
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
        seqGroup.addView(button, num, layoutParams)
    }

    fun selectSequence(num: Int) {
        seqGroup.setOnCheckedChangeListener(null)

        Log.i(TAG, "Select sequence $num")
        seqGroup.check(-1) // force redraw
        seqGroup.check(num)
        seqGroup.setOnCheckedChangeListener(seqGroupListener)
    }

    companion object {
        private val TAG = Sheet::class.java.simpleName
    }
}
