package org.helllabs.android.xmp.ui.screens.preferences

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
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.components.SingleChoiceListDialog
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun SettingsGroupSound() {
    val isAlive by PlayerService.isAlive.collectAsStateWithLifecycle()
    val prefManager: PrefManager = koinInject()
    val scope = rememberCoroutineScope()

    // Collect preferences as state
    val samplingRateValue by prefManager.samplingRateFlow().collectAsStateWithLifecycle(
        initialValue = 44100
    )
    val bufferMsValue by prefManager.bufferMsFlow().collectAsStateWithLifecycle(initialValue = 400)
    val volumeBoostValue by prefManager.volumeBoostFlow().collectAsStateWithLifecycle(
        initialValue = 1
    )
    val amigaMixerValue by prefManager.amigaMixerFlow().collectAsStateWithLifecycle(
        initialValue = false
    )
    val interpolateValue by prefManager.interpolateFlow().collectAsStateWithLifecycle(
        initialValue = true
    )
    val interpTypeValue by prefManager.interpTypeFlow().collectAsStateWithLifecycle(
        initialValue = 1
    )
    val stereoMixValue by prefManager.stereoMixFlow().collectAsStateWithLifecycle(
        initialValue = 100
    )
    val defaultPanValue by prefManager.defaultPanFlow().collectAsStateWithLifecycle(
        initialValue = 50
    )
    val allSequencesValue by prefManager.allSequencesFlow().collectAsStateWithLifecycle(
        initialValue = false
    )

    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_sound)) }
    ) {
        // Sampling Rate
        var samplingRateDialog by remember { mutableStateOf(false) }
        val samplingRateValues = stringArrayResource(id = R.array.sampling_rate_values)
        val samplingRate = remember(samplingRateValue) {
            samplingRateValues.indexOfFirst { it.toInt() == samplingRateValue }.coerceAtLeast(0)
        }

        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_sampling_rate_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_sampling_rate_summary)) },
            enabled = !isAlive,
            onClick = { samplingRateDialog = true }
        )
        SingleChoiceListDialog(
            isShowing = samplingRateDialog,
            title = stringResource(id = R.string.pref_sampling_rate_title),
            icon = Icons.Filled.CheckCircle,
            textList = stringArrayResource(id = R.array.sampling_rate_array).toPersistentList(),
            selectedIndex = samplingRate,
            onConfirm = {
                scope.launch {
                    prefManager.setSamplingRate(samplingRateValues[it].toInt())
                    samplingRateDialog = false
                }
            },
            onDismiss = { samplingRateDialog = false },
            onEmpty = { samplingRateDialog = false }
        )

        // Buffer Size
        var bufferSize by remember { mutableFloatStateOf(bufferMsValue.toFloat()) }
        LaunchedEffect(bufferMsValue) {
            bufferSize = bufferMsValue.toFloat()
        }
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
            onValueChange = { bufferSize = it },
            onValueChangeFinished = {
                scope.launch {
                    Timber.d("Setting buffer size to: ${bufferSize.toInt()}")
                    prefManager.setBufferMs(bufferSize.toInt())
                }
            }
        )

        // Volume Boost
        var volBoostDialog by remember { mutableStateOf(false) }
        val volBoostValues = stringArrayResource(id = R.array.vol_boost_values)
        val volBoost = remember(volumeBoostValue) {
            volBoostValues.indexOfFirst { it.toInt() == volumeBoostValue }.coerceAtLeast(0)
        }

        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_vol_boost_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_vol_boost_summary)) },
            onClick = { volBoostDialog = true }
        )
        SingleChoiceListDialog(
            isShowing = volBoostDialog,
            title = stringResource(id = R.string.pref_vol_boost_title),
            icon = Icons.Filled.CheckCircle,
            textList = stringArrayResource(id = R.array.vol_boost_array).toPersistentList(),
            selectedIndex = volBoost,
            onConfirm = {
                scope.launch {
                    prefManager.setVolumeBoost(volBoostValues[it].toInt())
                    volBoostDialog = false
                }
            },
            onDismiss = { volBoostDialog = false },
            onEmpty = { volBoostDialog = false }
        )

        // Amiga Mixer
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_amiga_mixer_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_amiga_mixer_summary)) },
            state = amigaMixerValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setAmigaMixer(it)
                }
            }
        )

        // Interpolate
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_interpolate_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_interpolate_summary)) },
            state = interpolateValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setInterpolate(it)
                }
            }
        )

        // Interpolation Type
        var interpTypeDialog by remember { mutableStateOf(false) }
        val interpTypeValues = stringArrayResource(id = R.array.interp_type_values)
        val interpType = remember(interpTypeValue) {
            interpTypeValues.indexOfFirst { it.toInt() == interpTypeValue }.coerceAtLeast(0)
        }

        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_interp_type_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_interp_type_summary)) },
            onClick = { interpTypeDialog = true }
        )
        SingleChoiceListDialog(
            isShowing = interpTypeDialog,
            icon = Icons.Filled.CheckCircle,
            title = stringResource(id = R.string.pref_interp_type_title),
            textList = stringArrayResource(id = R.array.interp_type_array).toPersistentList(),
            selectedIndex = interpType,
            onConfirm = {
                scope.launch {
                    prefManager.setInterpType(interpTypeValues[it].toInt())
                    interpTypeDialog = false
                }
            },
            onDismiss = { interpTypeDialog = false },
            onEmpty = { interpTypeDialog = false }
        )

        // Stereo Mix
        var stereoMix by remember { mutableFloatStateOf(stereoMixValue.toFloat()) }
        LaunchedEffect(stereoMixValue) {
            stereoMix = stereoMixValue.toFloat()
        }
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
            onValueChange = { stereoMix = it },
            onValueChangeFinished = {
                scope.launch {
                    Timber.d("Setting stereo mix to: ${stereoMix.toInt()}")
                    prefManager.setStereoMix(stereoMix.toInt())
                }
            }
        )

        // Default Pan
        var defaultPan by remember { mutableFloatStateOf(defaultPanValue.toFloat()) }
        LaunchedEffect(defaultPanValue) {
            defaultPan = defaultPanValue.toFloat()
        }
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
            onValueChange = { defaultPan = it },
            onValueChangeFinished = {
                scope.launch {
                    Timber.d("Setting default pan to: ${defaultPan.toInt()}")
                    prefManager.setDefaultPan(defaultPan.toInt())
                }
            }
        )

        // All Sequences
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_all_sequences_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_all_sequences_summary)) },
            state = allSequencesValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setAllSequences(it)
                }
            }
        )
    }
}
