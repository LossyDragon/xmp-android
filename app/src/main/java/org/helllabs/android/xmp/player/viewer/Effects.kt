package org.helllabs.android.xmp.player.viewer

import org.helllabs.android.xmp.util.logD

// Trash attempt for an Effects list
// Far from perfect... Some effects can't be displayed due to libxmp architecture.
// See: https://github.com/libxmp/libxmp/issues/321
// Reference:
// https://wiki.openmpt.org/Manual:_Effect_Reference
// libxmp/docs/formats

data class EffectList(
    val key: Int,
    val effect: String
)

object Effects {
    private val effects669: List<EffectList>
        get() {
            logD("669 Effects")
            return listOf(
                EffectList(96, "A"), // FX_669_PORTA_UP
                EffectList(97, "B"), // FX_669_PORTA_DN
                EffectList(98, "C"), // FX_669_TPORTA
                EffectList(99, "D"), // FX_669_FINETUNE
                EffectList(100, "E"), // FX_669_VIBRATO
                EffectList(126, "F"), // FX_SPEED_CP
            )
        }

    private val effectsFarandole: List<EffectList>
        get() {
            logD("Farandole Effects")
            return listOf(
                EffectList(249, "1"), // FX_FAR_PORTA_UP
                EffectList(248, "2"), // FX_FAR_PORTA_DN
                EffectList(122, "3"), // FX_PER_TPORTA
                EffectList(251, "4"), // FX_FAR_RETRIG
                EffectList(254, "5"), // FX_FAR_SETVIBRATO
                EffectList(4, "6"), // FX_VIBRATO
                EffectList(256, "7"), // FX_FAR_VSLIDE_UP
                EffectList(252, "8"), // FX_FAR_VSLIDE_DN
                EffectList(123, "9"), // FX_PER_VIBRATO
                EffectList(250, "C"), // FX_FAR_DELAY
                EffectList(15, "F"), // FX_SPEED
            )
        }

    private val effectsImagoOrpheus: List<EffectList>
        get() {
            logD("Imago Orpheus Effects")
            return listOf(
                EffectList(1, "1"), // FX_S3M_SPEED
                EffectList(2, "2"), // FX_S3M_BPM
                EffectList(3, "3"), // FX_TONEPORTA
                EffectList(4, "4"), // FX_TONE_VSLIDE
                EffectList(5, "5"), // FX_VIBRATO
                EffectList(6, "6"), // FX_VIBRA_VSLIDE
                EffectList(7, "7"), // FX_FINE_VIBRATO
                EffectList(8, "8"), // FX_TREMOLO
                EffectList(9, "9"), // FX_S3M_ARPEGGIO
                EffectList(10, "A"), // FX_SETPAN
                EffectList(11, "B"), // FX_PANSLIDE
                EffectList(12, "C"), // FX_VOLSET
                EffectList(13, "D"), // FX_VOLSLIDE
                EffectList(14, "E"), // FX_F_VSLIDE
                EffectList(15, "F"), // FX_FINETUNE
                EffectList(16, "G"), // FX_NSLIDE_UP
                EffectList(17, "H"), // FX_NSLIDE_DN
                EffectList(18, "I"), // FX_PORTA_UP
                EffectList(19, "J"), // FX_PORTA_DN
                EffectList(20, "K"), // FX_IMF_FPORTA_UP
                EffectList(21, "L"), // FX_IMF_FPORTA_DN
                EffectList(22, "M"), // FX_FLT_CUTOFF
                EffectList(23, "N"), // FX_FLT_RESN
                EffectList(24, "O"), // FX_OFFSET
                EffectList(25, "P"), // NONE /* fine offset */
                EffectList(26, "Q"), // FX_KEYOFF
                EffectList(27, "R"), // FX_MULTI_RETRIG
                EffectList(28, "S"), // FX_TREMOR
                EffectList(29, "T"), // FX_JUMP
                EffectList(30, "U"), // FX_BREAK
                EffectList(31, "V"), // FX_GLOBALVOL
                EffectList(32, "W"), // FX_GVOL_SLIDE
                EffectList(33, "X"), // FX_EXTENDED
                EffectList(34, "Y"), // FX_CHORUS
                EffectList(35, "Z"), // FX_REVERB
            )
        }

    private val effectsScream3: List<EffectList>
        get() {
            logD("Sream 3 Effects")
            return listOf(
                EffectList(163, "A"), // FX_S3M_SPEED
                EffectList(11, "B"), // FX_JUMP
                EffectList(13, "C"), // FX_BREAK
                EffectList(10, "D"), // FX_VOLSLIDE
                EffectList(2, "E"), // FX_PORTA_DN
                EffectList(1, "F"), // FX_PORTA_UP
                EffectList(3, "G"), // FX_TONEPORTA
                EffectList(4, "H"), // FX_VIBRATO
                EffectList(29, "I"), // FX_TREMOR
                EffectList(180, "J"), // FX_S3M_ARPEGGIO
                EffectList(6, "K"), // FX_VIBRA_VSLIDE
                EffectList(5, "L"), // FX_TONE_VSLIDE
                EffectList(9, "O"), // FX_OFFSET
                EffectList(27, "Q"), // FX_MULTI_RETRIG
                EffectList(7, "R"), // FX_TREMOLO
                EffectList(254, "S"), // FX_S3M_EXTENDED
                EffectList(171, "T"), // FX_S3M_BPM
                EffectList(172, "U"), // FX_FINE_VIBRATO
                EffectList(16, "V"), // FX_GLOBALVOL
                EffectList(8, "X"), // FX_SETPAN
                EffectList(141, "X"), // FX_SURROUND ("XA4")
                EffectList(14, "S"), // FX_SURROUND
                // S8x ??
            )
        }

    private val effectsImpulse: List<EffectList>
        get() {
            logD("Impulse Effects")
            return listOf(
                EffectList(163, "A"), // FX_S3M_SPEED
                EffectList(11, "B"), // FX_JUMP
                EffectList(142, "C"), // FX_IT_BREAK
                EffectList(10, "D"), // FX_VOLSLIDE
                EffectList(2, "E"), // FX_PORTA_DN
                EffectList(1, "F"), // FX_PORTA_UP
                EffectList(3, "G"), // FX_TONEPORTA
                EffectList(4, "H"), // FX_VIBRATO
                EffectList(29, "I"), // FX_TREMOR
                EffectList(180, "J"), // FX_S3M_ARPEGGIO
                EffectList(6, "K"), // FX_VIBRA_VSLIDE
                EffectList(5, "L"), // FX_TONE_VSLIDE
                EffectList(128, "M"), // FX_TRK_VOL
                EffectList(129, "N"), // FX_TRK_VSLIDE
                EffectList(9, "O"), // FX_OFFSET
                EffectList(137, "P"), // FX_IT_PANSLIDE
                EffectList(27, "Q"), // FX_MULTI_RETRIG
                EffectList(7, "R"), // FX_TREMOLO
                EffectList(254, "S"), // FX_XTND
                EffectList(135, "T"), // FX_IT_BPM
                EffectList(172, "U"), // FX_FINE_VIBRATO
                EffectList(16, "V"), // FX_GLOBALVOL
                EffectList(17, "W"), // FX_GVOL_SLIDE
                EffectList(8, "X"), // FX_SETPAN
                EffectList(138, "Y"), // FX_PANBRELLO
                EffectList(132, "Z"), // FX_FLT_CUTOFF
                EffectList(141, "S"), // FX_SURROUND ("S9x")
                EffectList(136, "S"), // FX_IT_ROWDELAY ("SEx")
                EffectList(14, "S"), // Pattern Delay ("SDx")
                EffectList(192, "c"), // FX_VSLIDE_UP_2
                EffectList(193, "d"), // FX_VSLIDE_DN_2
                EffectList(194, "a"), // FX_F_VSLIDE_UP_2
                EffectList(195, "b"), // FX_F_VSLIDE_DN_2
            )
        }

    private val effectsLiquid: List<EffectList>
        get() {
            // Would need testing
            logD("Liquid Effects")
            return listOf(
                EffectList(0, "A"), // FX_ARPEGGIO
                EffectList(171, "B"), // FX_S3M_BPM
                EffectList(13, "C"), // FX_BREAK
                EffectList(2, "D"), // FX_PORTA_DN
                EffectList(172, "F"), // FX_FINE_VIBRATO
                EffectList(11, "J"), // FX_JUMP
                EffectList(10, "L"), // FX_VOLSLIDE
                EffectList(14, "M"), // FX_EXTENDED
                EffectList(3, "N"), // FX_TONEPORTA
                EffectList(9, "O"), // FX_OFFSET
                EffectList(163, "S"), // FX_S3M_SPEED
                EffectList(7, "T"), // FX_TREMOLO
                EffectList(1, "U"), // FX_PORTA_UP
                EffectList(4, "V"), // FX_VIBRATO
                EffectList(5, "X"), // FX_TONE_VSLIDE
                EffectList(6, "Y"), // FX_VIBRA_VSLIDE
                // Extended effects?
            )
        }

    private val effectsOktalyzer: List<EffectList>
        get() {
            // Note: Based on docs (and pictures), effects are decimal, will sub with MOD style.
            // Would need testing
            logD("Oktalyzer Effects")
            return listOf(
                EffectList(1, "1"), // FX_PORTA_UP
                EffectList(2, "2"), // FX_PORTA_DN
                EffectList(112, "0"), // FX_OKT_ARP3
                EffectList(113, "0"), // FX_OKT_ARP4
                EffectList(114, "0"), // FX_OKT_ARP5
                EffectList(115, "6"), // FX_NSLIDE2_DN ?
                EffectList(116, "5"), // FX_NSLIDE2_UP ?
                EffectList(156, "6"), // FX_NSLIDE_DN ?
                EffectList(11, "B"), // FX_JUMP
                EffectList(15, "F"), // FX_SPEED
                EffectList(157, "5"), // FX_NSLIDE_UP ?
                EffectList(12, "C"), // FX_VOLSET
                EffectList(10, "A"), // FX_VOLSLIDE
                EffectList(174, "E"), // FX_F_VSLIDE_DN
                EffectList(17, "E"), // FX_F_VSLIDE_UP
                EffectList(0, "0"), // FX_ARPEGGIO
            )
        }

    private val effectsScream2: List<EffectList>
        get() {
            logD("Scream 2 Effects")
            return listOf(
                EffectList(15, "A"), // FX_SPEED
                EffectList(11, "B"), // FX_JUMP
                EffectList(13, "C"), // FX_BREAK
                EffectList(10, "D"), // FX_VOLSLIDE
                EffectList(2, "E"), // FX_PORTA_DN
                EffectList(1, "F"), // FX_PORTA_UP
                EffectList(3, "G"), // FX_TONEPORTA
                EffectList(4, "H"), // FX_VIBRATO
                EffectList(29, "I"), // FX_TREMOR
                EffectList(0, "J"), // FX_ARPEGGIO
            )
        }

    private val effectsProTracker: List<EffectList>
        get() {
            logD("ProTracker Effects")
            return listOf(
                EffectList(0, "0"), // FX_ARPEGGIO
                EffectList(1, "1"), // FX_PORTA_UP
                EffectList(2, "2"), // FX_PORTA_DN
                EffectList(3, "3"), // FX_TONEPORTA
                EffectList(4, "4"), // FX_VIBRATO
                EffectList(5, "5"), // FX_TONE_VSLIDE
                EffectList(6, "6"), // FX_VIBRA_VSLIDE
                EffectList(7, "7"), // FX_TREMOLO
                EffectList(8, "8"), // ?? Set Panning ??
                EffectList(9, "9"), // FX_OFFSET
                EffectList(10, "A"), // FX_VOLSLIDE
                EffectList(11, "B"), // FX_JUMP
                EffectList(12, "C"), // FX_VOLSET
                EffectList(13, "D"), // FX_BREAK
                EffectList(14, "E"), // FX_EXTENDED
                EffectList(15, "F"), // FX_SPEED
                EffectList(16, "G"), // FX_GLOBALVOL
                EffectList(27, "Q"), // FX_MULTI_RETRIG
                EffectList(181, "P"), // FX_PANSL_NOMEM
                EffectList(17, "H"), // FX_GVOL_SLIDE
                EffectList(21, "L"), // FX_ENVPOS
                EffectList(164, "c"), // FX_VOLSLIDE_2 (up down use same define?)
                EffectList(33, "X"), // FX_XF_PORTA
                EffectList(20, "K"), // FX_KEYOFF
                EffectList(25, "P"), // FX_PANSLIDE
                EffectList(29, "T"), // FX_TREMOR
                EffectList(146, "4"), // FX_VIBRATO2
            )
        }

    fun getEffectList(type: String): List<EffectList> {
        with(type) {
            return when {
                contains("669") -> effects669
                contains("Farandole", true) -> effectsFarandole
                contains("Imago Orpheus", true) -> effectsImagoOrpheus
                contains("S3M", true) -> effectsScream3
                contains("IT", true) -> effectsImpulse
                contains("LIQ", true) -> effectsLiquid
                contains("Oktalyzer", true) -> effectsOktalyzer
                contains("STX", true) -> effectsScream2
                else -> effectsProTracker // Most likely PTK based.
            }
        }
    }
}
