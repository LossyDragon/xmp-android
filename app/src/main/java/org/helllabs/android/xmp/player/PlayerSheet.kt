package org.helllabs.android.xmp.player

import android.annotation.SuppressLint
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.View
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RadioGroup.LayoutParams
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

    private val seqGroupListener: RadioGroup.OnCheckedChangeListener =
        RadioGroup.OnCheckedChangeListener { _, checkedId ->
            activity.playNewSequence(checkedId)
        }

    init {

        activity.binder.sheet.infoPane.apply {
            sidebarAllseqsSwitch.apply {
                isChecked = PrefManager.allSequences
                click { activity.toggleAllSequences() }
            }
            sidebarSequences.setOnCheckedChangeListener(seqGroupListener)
            sheetShowComment.click { showSongMessage() }
        }

        BottomSheetBehavior.from(activity.binder.sheet.controlsSheet).apply {
            activity.binder.sheet.controlsSheet.post {
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
        activity.binder.sheet.infoPane.apply {
            sidebarNumPat.text = pat.toString()
            sidebarNumIns.text = ins.toString()
            sidebarNumSmp.text = smp.toString()
            sidebarNumChn.text = chn.toString()
            sidebarAllseqsSwitch.isChecked = allSequences
        }
    }

    fun clearSequences() {
        activity.binder.sheet.infoPane.sidebarSequences.removeAllViews()
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
        activity.binder.sheet.infoPane.sidebarSequences.addView(button, num, layoutParams)
    }

    fun selectSequence(num: Int) {
        activity.binder.sheet.infoPane.sidebarSequences.apply {
            setOnCheckedChangeListener(null)
            logI("Selecting sequence $num")
            check(-1) // force redraw
            check(num)
            setOnCheckedChangeListener(seqGroupListener)
        }
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
