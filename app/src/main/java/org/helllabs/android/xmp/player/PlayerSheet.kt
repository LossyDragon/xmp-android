package org.helllabs.android.xmp.player

import android.annotation.SuppressLint
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RadioGroup.LayoutParams
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.click
import org.helllabs.android.xmp.util.color
import org.helllabs.android.xmp.util.logI
import org.helllabs.android.xmp.util.toast

class PlayerSheet(private val activity: PlayerActivity) {

    private val seqGroupListener: RadioGroup.OnCheckedChangeListener
    var peekState: Int? = null

    private val numPat: TextView = activity.findViewById(R.id.sidebar_num_pat)
    private val numIns: TextView = activity.findViewById(R.id.sidebar_num_ins)
    private val numSmp: TextView = activity.findViewById(R.id.sidebar_num_smp)
    private val numChn: TextView = activity.findViewById(R.id.sidebar_num_chn)
    private val seqSwitch: SwitchCompat = activity.findViewById(R.id.sidebar_allseqs_switch)
    private val sequences: RadioGroup = activity.findViewById(R.id.sidebar_sequences)
    private val sheet: LinearLayout = activity.findViewById(R.id.controlsSheet)
    private val commentButton: AppCompatImageButton = activity.findViewById(R.id.sheet_show_comment)

    // Can't get it styled this way, see http://stackoverflow.com/questions/3142067/android-set-style-in-code
    // val button = activity.layoutInflater.inflate(R.layout.item_sequence, null) as RadioButton

    init {
        seqSwitch.apply {
            isChecked = PrefManager.allSequences
            click { activity.toggleAllSequences() }
        }

        seqGroupListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
            activity.playNewSequence(checkedId)
        }
        sequences.setOnCheckedChangeListener(seqGroupListener)

        commentButton.click { showSongMessage() }

        BottomSheetBehavior.from(sheet).apply {
            peekState = BottomSheetBehavior.STATE_COLLAPSED

            sheet.post {
                val sheetPeekHeight = activity.findViewById<View>(R.id.player_sheet).height

                // Set the peek height dynamically.
                peekHeight = sheetPeekHeight

                // Set the bottom margin of the viewerLayout as well
                val playerLayout = activity.findViewById<FrameLayout>(R.id.player_layout)
                val viewerParams = playerLayout.layoutParams as CoordinatorLayout.LayoutParams
                viewerParams.setMargins(0, 0, 0, sheetPeekHeight)
                playerLayout.layoutParams = viewerParams
            }
        }
    }

    fun setDetails(pat: Int, ins: Int, smp: Int, chn: Int, allSequences: Boolean) {
        numPat.text = pat.toString()
        numIns.text = ins.toString()
        numSmp.text = smp.toString()
        numChn.text = chn.toString()
        seqSwitch.isChecked = allSequences
    }

    fun clearSequences() {
        sequences.removeAllViews()
    }

    @SuppressLint("InflateParams")
    fun addSequence(num: Int, duration: Int) {
        val main = activity.getString(R.string.sheet_main_song)
        val sub = activity.getString(R.string.sheet_sub_song, num)
        val text = if (num == 0) main else sub

        val button = RadioButton(activity)
        button.text = String.format("%2d:%02d (%s)", duration / 60000, duration / 1000 % 60, text)
        button.id = num
        button.setTextColor(activity.resources.color(R.color.lightgray))
        button.setTextSize(COMPLEX_UNIT_SP, 16f)

        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, 10, 0, 10)
        sequences.addView(button, num, layoutParams)
    }

    fun selectSequence(num: Int) {
        sequences.setOnCheckedChangeListener(null)
        logI("Selecting sequence $num")
        sequences.check(-1) // force redraw
        sequences.check(num)
        sequences.setOnCheckedChangeListener(seqGroupListener)
    }

    private fun showSongMessage() {
        val message = Xmp.getComment()
        if (message.isNullOrEmpty()) {
            activity.toast(R.string.msg_no_song_info)
            return
        }
        MaterialDialog(activity).show {
            title(R.string.dialog_title_song_message)
            message(text = message)
            positiveButton(R.string.ok)
        }
    }
}
