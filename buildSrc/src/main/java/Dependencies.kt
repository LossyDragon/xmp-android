object Dependencies {
    const val kotlinVersion = "1.5.10"

    // https://github.com/JLLeitschuh/ktlint-gradle/releases
    const val ktlintGradle = "10.1.0"

    // https://developer.android.com/jetpack/androidx/releases/compose
    const val composeVersion = "1.0.0-beta08"

    // https://github.com/google/dagger/releases
    const val hilt = "2.36"

    // https://developer.android.com/ndk/downloads
    const val androidNdk = "23.0.7344513-beta4"

    object Compose {
        const val activity = "androidx.activity:activity-compose:1.3.0-beta01"
        const val constraint = "androidx.constraintlayout:constraintlayout-compose:1.0.0-alpha07"
        const val foundation = "androidx.compose.foundation:foundation:$composeVersion"
        const val iconsCore = "androidx.compose.material:material-icons-core:$composeVersion"
        const val iconsExt = "androidx.compose.material:material-icons-extended:$composeVersion"
        const val livedata = "androidx.compose.runtime:runtime-livedata:$composeVersion"
        const val material = "androidx.compose.material:material:$composeVersion"
        const val tooling = "androidx.compose.ui:ui-tooling:$composeVersion"
        const val ui = "androidx.compose.ui:ui:$composeVersion"
        const val viewmodel = "androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha06"
    }

    object Accompanist {
        // https://github.com/google/accompanist/releases
        private const val accompanist = "0.11.1"
        const val insets = "com.google.accompanist:accompanist-insets:$accompanist"
        const val controller = "com.google.accompanist:accompanist-systemuicontroller:$accompanist"
    }

    object Hilt {
        const val android = "com.google.dagger:hilt-android:$hilt"
        const val compiler = "com.google.dagger:hilt-compiler:$hilt"
    }

    object Coroutines {
        private const val coroutines = "1.5.0"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines"
    }

    // TODO Replace with Compose Dialogs.
    object Dialogs {
        // https://github.com/afollestad/material-dialogs/releases
        private const val materialDialogs = "3.3.0"
        const val core = "com.afollestad.material-dialogs:core:$materialDialogs"
        const val lifecycle = "com.afollestad.material-dialogs:lifecycle:$materialDialogs"
    }

    object Xml {
        // https://github.com/Tickaroo/tikxml/releases
        const val tikXml = "0.8.13"
        const val processor = "com.tickaroo.tikxml:processor"
        const val annotation = "com.tickaroo.tikxml:annotation"
        const val retrofit = "com.tickaroo.tikxml:retrofit-converter"
    }
}
