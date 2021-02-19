package org.helllabs.android.xmp.player

import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.logI

class Sidebar(private val activity: PlayerActivity) {

    private val numPatText: TextView
    private val numInsText: TextView
    private val numSmpText: TextView
    private val numChnText: TextView
    private val allSequencesButton: ImageButton
    private val seqGroup: RadioGroup
    private val seqGroupListener: RadioGroup.OnCheckedChangeListener

    init {
        val contentView = activity.findViewById<LinearLayout>(R.id.content_view)
        activity.layoutInflater.inflate(R.layout.player, contentView, true)
        val sidebarView = activity.findViewById<LinearLayout>(R.id.sidebar_view)
        activity.layoutInflater.inflate(R.layout.player_sidebar, sidebarView, true)
        numPatText = activity.findViewById(R.id.sidebar_num_pat)
        numInsText = activity.findViewById(R.id.sidebar_num_ins)
        numSmpText = activity.findViewById(R.id.sidebar_num_smp)
        numChnText = activity.findViewById(R.id.sidebar_num_chn)
        allSequencesButton = activity.findViewById(R.id.sidebar_allseqs_button)
        allSequencesButton.setOnClickListener {
            allSequencesButton.setImageResource(
                if (activity.toggleAllSequences())
                    R.drawable.sub_on
                else
                    R.drawable.sub_off
            )
        }
        allSequencesButton.setImageResource(
            if (activity.allSequences) R.drawable.sub_on else R.drawable.sub_off
        )
        seqGroup = activity.findViewById<View>(R.id.sidebar_sequences) as RadioGroup
        seqGroupListener = RadioGroup.OnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            logE("Selection changed to sequence $checkedId")
            activity.playNewSequence(checkedId)
        }
        seqGroup.setOnCheckedChangeListener(seqGroupListener)
    }

    fun setDetails(numPat: Int, numIns: Int, numSmp: Int, numChn: Int, allSequences: Boolean) {
        numPatText.text = numPat.toString()
        numInsText.text = numIns.toString()
        numSmpText.text = numSmp.toString()
        numChnText.text = numChn.toString()
        allSequencesButton.setImageResource(
            if (allSequences)
                R.drawable.sub_on
            else
                R.drawable.sub_off
        )
    }

    fun clearSequences() {
        seqGroup.removeAllViews()
    }

    fun addSequence(num: Int, duration: Int) {
        // final RadioButton button = new RadioButton(activity);
        // Can't get it styled this way, see http://stackoverflow.com/questions/3142067/android-set-style-in-code
        val button = activity.layoutInflater.inflate(R.layout.sequence_item, null) as RadioButton
        val text = if (num == 0) "main song" else "subsong $num"
        button.text = String.format("%2d:%02d (%s)", duration / 60000, duration / 1000 % 60, text)
        button.id = num
        seqGroup.addView(
            button,
            num,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    fun selectSequence(num: Int) {
        seqGroup.setOnCheckedChangeListener(null)
        logI("Select sequence $num")
        seqGroup.check(-1) // force redraw
        seqGroup.check(num)
        seqGroup.setOnCheckedChangeListener(seqGroupListener)
    }

    // private void commentClick() {
    // 	final String comment = Xmp.getComment();
    // 	if (comment != null) {
    // 		final Intent intent = new Intent(activity, CommentActivity.class);
    // 		intent.putExtra("comment", comment);
    // 		activity.startActivity(intent);
    // 	}
    //
    // }
}
