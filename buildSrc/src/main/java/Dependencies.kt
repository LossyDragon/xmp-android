object Dependencies {

    const val kotlinVersion = "1.5.21"

    // https://developer.android.com/ndk/downloads#lts-downloads
    const val ndkVersion = "23.0.7599858"

    // https://github.com/JLLeitschuh/ktlint-gradle/releases
    const val ktlintGradle = "10.2.0"

    // https://github.com/google/dagger/releases
    const val hiltAndroid = "2.40"

    // https://github.com/google/accompanist/releases
    object Accompanist {
        private const val version = "0.21.0-beta"
        const val insets = "com.google.accompanist:accompanist-insets:$version"
        const val controller = "com.google.accompanist:accompanist-systemuicontroller:$version"
        const val permissions = "com.google.accompanist:accompanist-permissions:$version"
    }

    // https://mvnrepository.com/artifact/androidx.media/media
    // https://mvnrepository.com/artifact/androidx.preference/preference-ktx
    // https://mvnrepository.com/artifact/androidx.recyclerview/recyclerview
    object SupportLibs {
        const val media = "androidx.media:media:1.4.2"
        const val preferenceKtx = "androidx.preference:preference-ktx:1.1.1"
        const val recyclerview = "androidx.recyclerview:recyclerview:1.2.1"
    }

    // Feature roadmap: https://developer.android.com/jetpack/androidx/compose-roadmap
    // Versions: https://developer.android.com/jetpack/androidx/releases/compose#versions
    object Compose {
        private const val version = "1.1.0-alpha06" // "1.0.2"
        const val activity = "androidx.activity:activity-compose:1.3.1"
        const val animation = "androidx.compose.animation:animation:$version"
        const val graphics = "androidx.compose.animation:animation-graphics:$version"
        const val constraint = "androidx.constraintlayout:constraintlayout-compose:1.0.0-beta02"
        const val foundation = "androidx.compose.foundation:foundation:$version"
        const val iconsCore = "androidx.compose.material:material-icons-core:$version"
        const val iconsExtended = "androidx.compose.material:material-icons-extended:$version"
        const val lifecycle = "androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha07"
        const val livedata = "androidx.compose.runtime:runtime-livedata:$version"
        const val material = "androidx.compose.material:material:$version"
        const val material3 = "androidx.compose.material3:material3:1.0.0-alpha01"
        const val tooling = "androidx.compose.ui:ui-tooling:$version"
        const val ui = "androidx.compose.ui:ui:$version"
    }

    // https://mvnrepository.com/artifact/androidx.hilt/hilt-lifecycle-viewmodel
    object Hilt {
        const val android = "com.google.dagger:hilt-android:$hiltAndroid"
        const val compiler = "com.google.dagger:hilt-compiler:$hiltAndroid"
        const val viewModel = "androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03"
    }

    // https://github.com/Kotlin/kotlinx.coroutines/releases
    object Coroutines {
        private const val version = "1.5.2"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
    }

    // https://github.com/greenrobot/EventBus/releases
    object EventBus {
        const val eventBus = "org.greenrobot:eventbus:3.2.0"
    }

    // https://github.com/Tickaroo/tikxml/releases
    object TikXml {
        const val version = "0.8.13"
        const val annotation = "com.tickaroo.tikxml:annotation"
        const val converter = "com.tickaroo.tikxml:retrofit-converter"
        const val processor = "com.tickaroo.tikxml:processor"
    }

    // https://github.com/afollestad/material-dialogs/releases
    object MaterialDialogs {
        private const val version = "3.3.0"
        const val core = "com.afollestad.material-dialogs:core:$version"
        const val input = "com.afollestad.material-dialogs:input:$version"
        const val lifecycle = "com.afollestad.material-dialogs:lifecycle:$version"
    }

    // https://github.com/tonyofrancis/Fetch/releases
    object XFetch2 {
        private const val version = "3.1.6"
        const val fetch = "androidx.tonyodev.fetch2:xfetch2:$version"
        const val okHttp = "androidx.tonyodev.fetch2okhttp:xfetch2okhttp:$version"
    }

    // https://search.maven.org/artifact/com.squareup.okhttp3/okhttp
    // https://search.maven.org/artifact/com.squareup.retrofit2/retrofit
    // https://search.maven.org/artifact/com.squareup.moshi/moshi-kotlin/
    object SquareUp {
        const val okHttp = "com.squareup.okhttp3:okhttp:4.9.2"
        const val retrofit = "com.squareup.retrofit2:retrofit:2.9.0"
        const val moshi = "com.squareup.moshi:moshi-kotlin:1.12.0"
    }
}
