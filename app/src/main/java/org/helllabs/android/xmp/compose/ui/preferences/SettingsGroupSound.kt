package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSlider
import com.alorma.compose.settings.ui.SettingsSwitch
import kotlinx.collections.immutable.toPersistentList
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.SingleChoiceListDialog
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

@Composable
fun SettingsGroupSound() {
    val isAlive by PlayerService.isAlive.collectAsStateWithLifecycle()

    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_sound)) }
    ) {
        var samplingRateDialog by remember { mutableStateOf(false) }
        var samplingRate by remember { mutableIntStateOf(0) }
        val samplingRateValues = stringArrayResource(id = R.array.sampling_rate_values)
        LaunchedEffect(samplingRateDialog) {
            samplingRateValues.forEachIndexed { index, s ->
                if (PrefManager.samplingRate == s.toInt()) {
                    samplingRate = index
                }
            }
        }
        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_sampling_rate_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_sampling_rate_summary)) },
            enabled = !isAlive,
            onClick = {
                samplingRateDialog = true
            }
        )
        SingleChoiceListDialog(
            isShowing = samplingRateDialog,
            title = stringResource(id = R.string.pref_sampling_rate_title),
            icon = Icons.Filled.CheckCircle,
            list = stringArrayResource(id = R.array.sampling_rate_array).toPersistentList(),
            selectedIndex = samplingRate,
            onConfirm = {
                PrefManager.samplingRate = samplingRateValues[it].toInt()
                samplingRateDialog = false
            },
            onDismiss = {
                samplingRateDialog = false
            },
            onEmpty = {
                samplingRateDialog = false
            }
        )

        var bufferSize by remember { mutableFloatStateOf(PrefManager.bufferMs.toFloat()) }
        SettingsSlider(
            enabled = !isAlive,
            title = { Text(text = stringResource(id = R.string.pref_buffer_ms_title)) },
            subtitle = {
                Text(
                    text = stringResource(
                        id = R.string.pref_buffer_ms_dialog,
                        "${bufferSize.toInt()}ms"
                    )
                )
            },
            valueRange = 1f..1000f,
            value = bufferSize,
            onValueChange = {
                bufferSize = it
            },
            onValueChangeFinished = {
                Timber.d("Setting value to: $bufferSize")
                PrefManager.bufferMs = bufferSize.toInt()
            }
        )

        var volBoostDialog by remember { mutableStateOf(false) }
        var volBoost by remember { mutableIntStateOf(0) }
        val volBoostValues = stringArrayResource(id = R.array.vol_boost_values)
        LaunchedEffect(volBoostDialog) {
            volBoostValues.forEachIndexed { index, s ->
                if (PrefManager.volumeBoost == s.toInt()) {
                    volBoost = index
                }
            }
        }
        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_vol_boost_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_vol_boost_summary)) },
            onClick = {
                volBoostDialog = true
            }
        )
        SingleChoiceListDialog(
            isShowing = volBoostDialog,
            title = stringResource(id = R.string.pref_vol_boost_title),
            icon = Icons.Filled.CheckCircle,
            list = stringArrayResource(id = R.array.vol_boost_array).toPersistentList(),
            selectedIndex = volBoost,
            onConfirm = {
                PrefManager.volumeBoost = it + 1
                volBoostDialog = false
            },
            onDismiss = {
                volBoostDialog = false
            },
            onEmpty = {
                volBoostDialog = false
            }
        )

        var amigaMixer by remember { mutableStateOf(PrefManager.amigaMixer) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_amiga_mixer_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_amiga_mixer_summary)) },
            state = amigaMixer,
            onCheckedChange = {
                PrefManager.amigaMixer = it
                amigaMixer = it
            }
        )

        var interpolate by remember { mutableStateOf(PrefManager.interpolate) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_interpolate_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_interpolate_summary)) },
            state = interpolate,
            onCheckedChange = {
                PrefManager.interpolate = it
                interpolate = it
            }
        )

        var interpTypeDialog by remember { mutableStateOf(false) }
        var interpType by remember { mutableIntStateOf(0) }
        val interpTypeValues = stringArrayResource(id = R.array.interp_type_values)
        LaunchedEffect(interpTypeDialog) {
            interpTypeValues.forEachIndexed { index, s ->
                if (PrefManager.interpType == s.toInt()) {
                    interpType = index
                }
            }
        }
        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_interp_type_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_interp_type_summary)) },
            onClick = {
                interpTypeDialog = true
            }
        )
        SingleChoiceListDialog(
            isShowing = interpTypeDialog,
            icon = Icons.Filled.CheckCircle,
            title = stringResource(id = R.string.pref_interp_type_title),
            list = stringArrayResource(id = R.array.interp_type_array).toPersistentList(),
            selectedIndex = interpType,
            onConfirm = {
                PrefManager.interpType = it + 1
                interpTypeDialog = false
            },
            onDismiss = {
                interpTypeDialog = false
            },
            onEmpty = {
                interpTypeDialog = false
            }
        )

        var stereoMix by remember { mutableFloatStateOf(PrefManager.stereoMix.toFloat()) }
        SettingsSlider(
            title = { Text(text = stringResource(id = R.string.pref_pan_separation_title)) },
            subtitle = {
                Text(
                    text = stringResource(
                        id = R.string.pref_pan_separation_dialog,
                        "${stereoMix.toInt()}%"
                    )
                )
            },
            valueRange = 1f..100f,
            value = stereoMix,
            onValueChange = {
                stereoMix = it
            },
            onValueChangeFinished = {
                Timber.d("Setting value to: $stereoMix")
                PrefManager.stereoMix = stereoMix.toInt()
            }
        )

        var defaultPan by remember { mutableFloatStateOf(PrefManager.defaultPan.toFloat()) }
        SettingsSlider(
            title = { Text(text = stringResource(id = R.string.pref_default_pan_title)) },
            subtitle = {
                Text(
                    text = stringResource(
                        id = R.string.pref_default_pan_dialog,
                        "${defaultPan.toInt()}%"
                    )
                )
            },
            valueRange = 1f..100f,
            value = defaultPan,
            onValueChange = {
                defaultPan = it
            },
            onValueChangeFinished = {
                Timber.d("Setting value to: $defaultPan")
                PrefManager.defaultPan = defaultPan.toInt()
            }
        )

        var allSequence by remember { mutableStateOf(PrefManager.allSequences) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_all_sequences_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_all_sequences_summary)) },
            state = allSequence,
            onCheckedChange = {
                PrefManager.allSequences = it
                allSequence = it
            }
        )
    }
}
