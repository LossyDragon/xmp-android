package org.helllabs.android.xmp.player.viewer

// Trash attempt for an Effects list
// See: https://github.com/libxmp/libxmp/issues/321

/* Has an FX array */
// TODO: liq_load
// TODO: no_load has "something" of an FX list
// TODO: okt_load
// TODO: stx_load

// TODO: Arpeggio still ignored as its key: 0

/* Still Missing */
// MilkyTracker XM 1.04 | 164 -> Kmuland__Strobe_-_CEL-II.xm or db_keygen.xm or fade_2_grey_visage.xm
// OpenMPT 1.28 IT 2.16 | 193 -> dusthillguy_-_usingarubbishcomputerfor3weeks.it
// FastTracker v2.00 XM 1.04 | 33 -> fade_2_grey_visage.xm or desert_tales_1-timestopper.xm

data class EffectList(
    val key: Int,
    val effect: String
)

object Effects {
    private val sixSixNineEffects = listOf(
        EffectList(96, "A"), // FX_669_PORTA_UP
        EffectList(97, "B"), // FX_669_PORTA_DN
        EffectList(98, "C"), // FX_669_TPORTA
        EffectList(99, "D"), // FX_669_FINETUNE
        EffectList(100, "E"), // FX_669_VIBRATO
        EffectList(126, "F") // FX_SPEED_CP
    )

    private val farEffects = listOf(
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
        EffectList(15, "F") // FX_SPEED
    )

    private val proTrackerEffects = listOf(
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
        EffectList(16, "G"), // FX_GLOBALVOL (effects.c)
        EffectList(27, "Q"), // FX_MULTI_RETRIG (effects.c)
        EffectList(181, "P"), // FX_PANSL_NOMEM (effects.c)
        EffectList(17, "H"), // FX_GVOL_SLIDE (effects.c)
        EffectList(21, "L") // FX_ENVPOS (effects.c)
    )

    private val imagoOrpheusEffects = listOf(
        EffectList(1, "1"), // FX_S3M_SPEED,
        EffectList(2, "2"), // FX_S3M_BPM,
        EffectList(3, "3"), // FX_TONEPORTA,
        EffectList(4, "4"), // FX_TONE_VSLIDE,
        EffectList(5, "5"), // FX_VIBRATO,
        EffectList(6, "6"), // FX_VIBRA_VSLIDE,
        EffectList(7, "7"), // FX_FINE_VIBRATO,
        EffectList(8, "8"), // FX_TREMOLO,
        EffectList(9, "9"), // FX_S3M_ARPEGGIO,
        EffectList(10, "A"), // FX_SETPAN,
        EffectList(11, "B"), // FX_PANSLIDE,
        EffectList(12, "C"), // FX_VOLSET,
        EffectList(13, "D"), // FX_VOLSLIDE,
        EffectList(14, "E"), // FX_F_VSLIDE,
        EffectList(15, "F"), // FX_FINETUNE,
        EffectList(16, "G"), // FX_NSLIDE_UP,
        EffectList(17, "H"), // FX_NSLIDE_DN,
        EffectList(18, "I"), // FX_PORTA_UP,
        EffectList(19, "J"), // FX_PORTA_DN,
        EffectList(20, "K"), // FX_IMF_FPORTA_UP,
        EffectList(21, "L"), // FX_IMF_FPORTA_DN,
        EffectList(22, "M"), // FX_FLT_CUTOFF,
        EffectList(23, "N"), // FX_FLT_RESN,
        EffectList(24, "O"), // FX_OFFSET,
        EffectList(25, "P"), // NONE /* fine offset */,
        EffectList(26, "Q"), // FX_KEYOFF,
        EffectList(27, "R"), // FX_MULTI_RETRIG,
        EffectList(28, "S"), // FX_TREMOR,
        EffectList(29, "T"), // FX_JUMP,
        EffectList(30, "U"), // FX_BREAK,
        EffectList(31, "V"), // FX_GLOBALVOL,
        EffectList(32, "W"), // FX_GVOL_SLIDE,
        EffectList(33, "X"), // FX_EXTENDED,
        EffectList(34, "Y"), // FX_CHORUS,
        EffectList(35, "Z") // FX_REVERB
    )

    private val impulseEffects = listOf(
        EffectList(163, "A"), // FX_S3M_SPEED,
        EffectList(11, "B"), // FX_JUMP,
        EffectList(142, "C"), // FX_IT_BREAK,
        EffectList(10, "D"), // FX_VOLSLIDE,
        EffectList(2, "E"), // FX_PORTA_DN,
        EffectList(1, "F"), // FX_PORTA_UP,
        EffectList(3, "G"), // FX_TONEPORTA,
        EffectList(4, "H"), // FX_VIBRATO,
        EffectList(29, "I"), // FX_TREMOR,
        EffectList(180, "J"), // FX_S3M_ARPEGGIO,
        EffectList(6, "K"), // FX_VIBRA_VSLIDE,
        EffectList(5, "L"), // FX_TONE_VSLIDE,
        EffectList(128, "M"), // FX_TRK_VOL,
        EffectList(129, "N"), // FX_TRK_VSLIDE,
        EffectList(9, "O"), // FX_OFFSET,
        EffectList(137, "P"), // FX_IT_PANSLIDE,
        EffectList(27, "Q"), // FX_MULTI_RETRIG,
        EffectList(7, "R"), // FX_TREMOLO,
        EffectList(254, "S"), // FX_XTND,
        EffectList(135, "T"), // FX_IT_BPM,
        EffectList(172, "U"), // FX_FINE_VIBRATO,
        EffectList(16, "V"), // FX_GLOBALVOL,
        EffectList(14, "W"), // FX_GVOL_SLIDE,
        EffectList(8, "X"), // FX_SETPAN,
        EffectList(138, "Y"), // FX_PANBRELLO,
        EffectList(132, "Z"), // FX_FLT_CUTOFF,
        EffectList(141, "S"), // FX_SURROUND ("S9x")
        EffectList(136, "S"), // FX_IT_ROWDELAY ("SEx")
        EffectList(192, "d") // FX_VSLIDE_DN_2
    )

    private val s3mEffects = listOf(
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
    )

    fun getEffectList(type: String): List<EffectList> {
        with(type) {
            return when {
                contains("669") -> sixSixNineEffects
                contains("Farandole", true) -> farEffects
                contains("Imago Orpheus", true) -> imagoOrpheusEffects
                contains("S3M", true) -> s3mEffects
                contains("IT", true) -> impulseEffects
                else -> proTrackerEffects // Most likely PTK based.
            }
        }
    }
}
