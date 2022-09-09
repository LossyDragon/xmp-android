object Versions {
    const val accompanist = "0.26.2-beta"
    const val compilerExtension = "1.3.1"
    const val compose = "1.2.0-alpha08"
    const val composeDestination = "1.7.19-beta"
    const val composePreferences = "0.1.4"
    const val hilt = "2.43.2"
    const val kotlin = "1.7.10"
    const val ksp = "1.7.10-1.0.6"
    const val ktLint = "11.0.0"
    const val xmlUtil = "0.84.2"
}

// Roadmap: https://developer.android.com/jetpack/androidx/compose-roadmap
object Libs {

    // https://developer.android.com/jetpack/androidx/releases/core
    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    val androidx = listOf(
        "androidx.core:core-ktx:1.9.0",
        "androidx.lifecycle:lifecycle-runtime-ktx:2.5.1",
    )

    // https://developer.android.com/jetpack/androidx/releases/activity
    // https://developer.android.com/jetpack/androidx/releases/navigation
    // https://developer.android.com/jetpack/androidx/releases/compose-material3
    // https://developer.android.com/jetpack/androidx/releases/compose#versions
    val compose = listOf(
        "androidx.activity:activity-compose:1.5.1",
        "androidx.compose.material3:material3:1.0.0-alpha15",
        "androidx.compose.material:material-icons-core:${Versions.compose}",
        "androidx.compose.material:material-icons-extended:${Versions.compose}",
        "androidx.compose.material:material:${Versions.compose}",
        "androidx.compose.ui:ui-tooling-preview:${Versions.compose}",
        "androidx.compose.ui:ui-tooling:${Versions.compose}",
        "androidx.compose.ui:ui-util:${Versions.compose}",
        "androidx.compose.ui:ui:${Versions.compose}",
        "androidx.navigation:navigation-compose:2.6.0-alpha01",
    )

    // https://github.com/google/accompanist/releases
    val accompanist = listOf(
        "com.google.accompanist:accompanist-insets:${Versions.accompanist}",
        "com.google.accompanist:accompanist-permissions:${Versions.accompanist}",
        "com.google.accompanist:accompanist-systemuicontroller:${Versions.accompanist}",
    )

    // https://github.com/Sh4dowSoul/ComposePreferences/releases
    val composePreferences = listOf(
        "com.github.Sh4dowSoul.ComposePreferences:datastore-manager:${Versions.composePreferences}",
        "com.github.Sh4dowSoul.ComposePreferences:preferences-material3:${Versions.composePreferences}",
    )

    // https://github.com/raamcosta/compose-destinations/releases
    val composeDestination = listOf(
        "io.github.raamcosta.compose-destinations:animations-core:${Versions.composeDestination}",
        "io.github.raamcosta.compose-destinations:core:${Versions.composeDestination}",
    )
    val composeDestinationKsp = listOf(
        "io.github.raamcosta.compose-destinations:ksp:${Versions.composeDestination}",
    )

    // https://github.com/google/dagger/releases
    // Hilt Nav Compose: https://developer.android.com/jetpack/androidx/releases/hilt
    val daggerHilt = listOf(
        "androidx.hilt:hilt-navigation-compose:1.0.0",
        "com.google.dagger:hilt-android:${Versions.hilt}",
    )

    val daggerHiltKapt = listOf(
        "com.google.dagger:hilt-compiler:${Versions.hilt}",
    )

    // https://github.com/pdvrieze/xmlutil/releases
    val xmlUtil = listOf(
        "io.github.pdvrieze.xmlutil:core-android:${Versions.xmlUtil}",
        "io.github.pdvrieze.xmlutil:serialization-android:${Versions.xmlUtil}",
    )


}