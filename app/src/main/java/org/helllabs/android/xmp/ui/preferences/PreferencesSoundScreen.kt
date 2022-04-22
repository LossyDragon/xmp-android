package org.helllabs.android.xmp.ui.preferences

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.google.accompanist.insets.navigationBarsPadding
import de.schnettler.datastore.compose.material3.PreferenceScreen
import de.schnettler.datastore.compose.material3.model.Preference
import kotlin.math.roundToInt
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.NavScreens
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.util.PrefManager
import org.helllabs.android.xmp.util.PrefManager.dataStore

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun PreferencesSoundScreen(
    navController: NavController,
    onBackPressedCallback: OnBackPressedDispatcher
) {
    val context = LocalContext.current
    val dataStore = context.dataStore

    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    val onBackPressed = {
        navController.popBackStack(route = NavScreens.Settings.route, inclusive = false)
    }

    val callback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
    }

    DisposableEffect(onBackPressedCallback) {
        onBackPressedCallback.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        topBar = {
            XmpAppBar3(
                title = { Text(text = stringResource(id = R.string.pref_category_sound)) },
                scrollBehavior = scrollBehavior,
                onNavIconPressed = { onBackPressed() }
            )
        },
    ) { innerPadding ->
        PreferenceScreen(
            items = listOf(
                Preference.PreferenceGroup(
                    title = stringResource(id = R.string.pref_category_mixer_control),
                    enabled = true,
                    preferenceItems = listOf(
                        Preference.PreferenceItem.ListPreference(
                            enabled = true,
                            entries = PrefManager.prefSamplingRate,
                            icon = {},
                            request = PrefManager.samplingRateRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_sampling_rate_summary),
                            title = stringResource(id = R.string.pref_sampling_rate_title),
                        ),
                        Preference.PreferenceItem.SeekBarPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.bufferSizeRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_buffer_ms_summary),
                            title = stringResource(id = R.string.pref_buffer_ms_title),
                            steps = 10,
                            valueRepresentation = { value -> "${value.roundToInt()} %" },
                            valueRange = 0f..1000f,
                        ),
                        Preference.PreferenceItem.ListPreference(
                            enabled = true,
                            entries = PrefManager.prefVolumeBoost,
                            icon = {},
                            request = PrefManager.volumeBoostRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_vol_boost_summary),
                            title = stringResource(id = R.string.pref_vol_boost_title),
                        ),
                        Preference.PreferenceItem.SwitchPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.amigaMixerRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_amiga_mixer_summary),
                            title = stringResource(id = R.string.pref_amiga_mixer_title),
                        ),
                        Preference.PreferenceItem.SwitchPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.interpolationRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_interpolate_summary),
                            title = stringResource(id = R.string.pref_interpolate_title),
                        ),
                        Preference.PreferenceItem.ListPreference(
                            enabled = true,
                            entries = PrefManager.prefInterpolationType,
                            icon = {},
                            request = PrefManager.interpolationTypeRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_interp_type_summary),
                            title = stringResource(id = R.string.pref_interp_type_title),
                        ),
                        Preference.PreferenceItem.SeekBarPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.stereoSeparationRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_pan_separation_summary),
                            title = stringResource(id = R.string.pref_pan_separation_title),
                            steps = 2,
                            valueRepresentation = { value -> "${value.roundToInt()} %" },
                            valueRange = 0f..100f,
                        ),
                        Preference.PreferenceItem.SeekBarPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.defaultPanRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_default_pan_summary),
                            title = stringResource(id = R.string.pref_default_pan_title),
                            steps = 2,
                            valueRepresentation = { value -> "${value.roundToInt()} %" },
                            valueRange = 0f..100f,
                        )
                    )
                ),
                Preference.PreferenceGroup(
                    title = stringResource(id = R.string.pref_category_player_control),
                    enabled = true,
                    preferenceItems = listOf(
                        Preference.PreferenceItem.SwitchPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.hiddenPatternsRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_all_sequences_summary),
                            title = stringResource(id = R.string.pref_all_sequences_title),
                        )
                    )
                )
            ),
            dataStore = dataStore,
            contentPadding = innerPadding,
        )
    }
}
