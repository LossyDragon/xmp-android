package org.helllabs.android.xmp.compose.ui.player.viewer

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

/*
 * An attempt to make sense of Effect Types. libxmp doesn't have an official way
 *
 * Issue: https://github.com/libxmp/libxmp/issues/321
 * Reference: https://wiki.openmpt.org/Manual:_Effect_Reference
 * Reference: https://github.com/libxmp/libxmp/tree/master/docs/formats
 */

// MMD Common?

object Effects {
    // https://github.com/libxmp/libxmp/blob/master/docs/formats/669.txt
    private val effects669 = persistentMapOf(
        0x00.toByte() to "-", // NO FX
        0x60.toByte() to "a", // FX_669_PORTA_UP,
        0x61.toByte() to "b", // FX_669_PORTA_DN,
        0x62.toByte() to "c", // FX_669_TPORTA,
        0x63.toByte() to "d", // FX_669_FINETUNE,
        0x64.toByte() to "e", // FX_669_VIBRATO,
        0x7e.toByte() to "f", // FX_SPEED_CP
        0x0d.toByte() to "B", // FX_BREAK
        0x7f.toByte() to "-" // FX_PER_CANCEL
    )

    // https://github.com/libxmp/libxmp/blob/master/docs/formats/far100.doc
    private val effectsFarandole = persistentMapOf(
        0x00.toByte() to "-", // NO FX
        0x65.toByte() to "1", // FX_FAR_PORTA_UP
        0x66.toByte() to "2", // FX_FAR_PORTA_DN
        0x67.toByte() to "3", // FX_FAR_TPORTA
        0x6d.toByte() to "4", // FX_FAR_RETRIG
        0x6a.toByte() to "5", // FX_FAR_VIBDEPTH
        0x6b.toByte() to "6", // FX_FAR_VIBRATO - Cant distinguish `Sustained` or not
        0xad.toByte() to "7", // FX_F_VSLIDE_UP
        0xae.toByte() to "8", // FX_F_VSLIDE_DN
        0x6c.toByte() to "a", // FX_FAR_SLIDEVOL
        0x08.toByte() to "b", // FX_SETPAN
        0x6e.toByte() to "c", // FX_FAR_DELAY
        0x69.toByte() to "d", // FX_FAR_F_TEMPO - Can't distinguish `Up` or `Down`
        0x68.toByte() to "f", // FX_FAR_TEMPO
        0x0d.toByte() to "B" // FX_BREAK
    )

    // https://github.com/libxmp/libxmp/blob/master/docs/formats/s3m-effects.txt
    private val effectsScream3 = persistentMapOf(
        0x00.toByte() to "-", // NO FX
        0xa3.toByte() to "A", // FX_S3M_SPEED
        0x0b.toByte() to "B", // FX_JUMP
        0x0d.toByte() to "C", // FX_BREAK
        0x0a.toByte() to "D", // FX_VOLSLIDE
        0x02.toByte() to "E", // FX_PORTA_DN
        0x01.toByte() to "F", // FX_PORTA_UP
        0x03.toByte() to "G", // FX_TONEPORTA
        0x04.toByte() to "H", // FX_VIBRATO
        0x1d.toByte() to "I", // FX_TREMOR
        0xb4.toByte() to "J", // FX_S3M_ARPEGGIO
        0x06.toByte() to "K", // FX_VIBRA_VSLIDE
        0x05.toByte() to "L", // FX_TONE_VSLIDE
        0x09.toByte() to "O", // FX_OFFSET
        0x1b.toByte() to "Q", // FX_MULTI_RETRIG
        0x07.toByte() to "R", // FX_TREMOLO
        0xfe.toByte() to "S", // FX_S3M_EXTENDED
        0xab.toByte() to "T", // FX_S3M_BPM
        0xac.toByte() to "U", // FX_FINE_VIBRATO
        0x10.toByte() to "V", // FX_GLOBALVOL
        0x08.toByte() to "X" // FX_SETPAN
    )

    // https://github.com/libxmp/libxmp/blob/master/docs/formats/xm.txt
    private val effectsXM = persistentMapOf(
        0x00.toByte() to "0", // 0  Appregio
        0x01.toByte() to "1", // 1  Porta up
        0x02.toByte() to "2", // 2  Porta down
        0x03.toByte() to "3", // 3  Tone porta
        0x04.toByte() to "4", // 4  Vibrato
        0x05.toByte() to "5", // 5  Tone porta+Volume slide
        0x06.toByte() to "6", // 6  Vibrato+Volume slide
        0x07.toByte() to "7", // 7  Tremolo
        0x08.toByte() to "8", // 8  Set panning
        0x09.toByte() to "9", // 9  Sample offset
        0x0a.toByte() to "A", // A  Volume slide
        0x0b.toByte() to "B", // B  Position jump
        0x0c.toByte() to "C", // C  Set volume
        0x0d.toByte() to "D", // D  Pattern break
//        0x00.toByte() to "E", // E1 Fine porta up
//        0x00.toByte() to "E", // E2 Fine porta down
//        0x00.toByte() to "E", // E3 Set gliss control
//        0x00.toByte() to "E", // E4 Set vibrato control
//        0x00.toByte() to "E", // E5 Set finetune
//        0x00.toByte() to "E", // E6 Set loop begin/loop
//        0x00.toByte() to "E", // E7 Set tremolo control
//        0x00.toByte() to "E", // E9 Retrig note
//        0x00.toByte() to "E", // EA Fine volume slide up
//        0x00.toByte() to "E", // EB Fine volume slide down
//        0x00.toByte() to "E", // EC Note cut
        0x0e.toByte() to "E", // ED Note delay
//        0x00.toByte() to "E", // EE Pattern delay
        0x0f.toByte() to "F", // F  Set tempo/BPM
        0x10.toByte() to "G", // G  Set global volume
        0x11.toByte() to "H", // H  Global volume slide
        0x14.toByte() to "K", // K  Key off
        0x15.toByte() to "L", // L  Set envelope position
        0x19.toByte() to "P", // P  Panning slide
        0x1B.toByte() to "R", // R  Multi retrig note
//        0x00.toByte() to "T", // T  Tremor
//        0x00.toByte() to "X", // X1 Extra fine porta up
        0x21.toByte() to "X" // X2 Extra fine porta down
    )

    // https://github.com/libxmp/libxmp/blob/master/docs/formats/it-effects.txt
    private val effectsIT = persistentMapOf(
        0xff.toByte() to "-", // NO FX
        0xa3.toByte() to "A", // FX_S3M_SPEED,
        0x0b.toByte() to "B", // FX_JUMP,
        0x8e.toByte() to "C", // FX_IT_BREAK,
        0x0a.toByte() to "D", // FX_VOLSLIDE,
        0x02.toByte() to "E", // FX_PORTA_DN,
        0x01.toByte() to "F", // FX_PORTA_UP,
        0x03.toByte() to "G", // FX_TONEPORTA,
        0x04.toByte() to "H", // FX_VIBRATO,
        0x1d.toByte() to "I", // FX_TREMOR,
        0xb4.toByte() to "J", // FX_S3M_ARPEGGIO,
        0x06.toByte() to "K", // FX_VIBRA_VSLIDE,
        0x05.toByte() to "L", // FX_TONE_VSLIDE,
        0x80.toByte() to "M", // FX_TRK_VOL,
        0x81.toByte() to "N", // FX_TRK_VSLIDE,
        0x09.toByte() to "O", // FX_OFFSET,
        0x89.toByte() to "P", // FX_IT_PANSLIDE,
        0x1b.toByte() to "Q", // FX_MULTI_RETRIG,
        0x07.toByte() to "R", // FX_TREMOLO,
        0xfe.toByte() to "S", // FX_XTND,
        0x0e.toByte() to "S", // FX_EXTENDED,
        0x87.toByte() to "T", // FX_IT_BPM,
        0xac.toByte() to "U", // FX_FINE_VIBRATO,
        0x10.toByte() to "V", // FX_GLOBALVOL,
        0x11.toByte() to "W", // FX_GVOL_SLIDE,
        0x08.toByte() to "X", // FX_SETPAN,
        0x8a.toByte() to "Y", // FX_PANBRELLO,
        0xbe.toByte() to "Z", // FX_MACRO,
        0xbf.toByte() to "/", // FX_MACROSMOOTH,
    )

    // TODO
    // Using 4.00 https://github.com/libxmp/libxmp/blob/master/docs/formats/octamed4.00-effects.txt
    private val effectsOctaMed = persistentMapOf(
        0x0c.toByte() to "C" // SET VOLUME 0C
    )

    data class EffectsTable(val name: String, val table: ImmutableMap<Byte, String>)

    fun getEffectList(type: String): EffectsTable {
        val xm = "(xm|fast|protracker|m\\.k\\.)".toRegex(RegexOption.IGNORE_CASE)
        val scream = "(scream|s3m|masi)".toRegex(RegexOption.IGNORE_CASE)
        val impulse = "(IT|Impulse)".toRegex(RegexOption.IGNORE_CASE)

        return when {
            type.contains("Farandole", true) -> EffectsTable("Farandole", effectsFarandole)
            type.contains("669") -> EffectsTable("669", effects669)
            type.contains(scream) -> EffectsTable("Scream", effectsScream3)
            type.contains(xm) -> EffectsTable("XM", effectsXM)
            type.contains(impulse) -> EffectsTable("IT", effectsIT)
            type.contains("octa", true) -> EffectsTable("OctaMED", effectsOctaMed)
            else -> EffectsTable("Unknown", persistentMapOf())
        }
    }
}
