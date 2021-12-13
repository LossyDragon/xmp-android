object Dependencies {

    const val kotlinVersion = "1.6.0"

    // Feature roadmap: https://developer.android.com/jetpack/androidx/compose-roadmap
    // Versions: https://developer.android.com/jetpack/androidx/releases/compose#versions
    const val composeVersion = "1.1.0-beta04"

    // https://developer.android.com/ndk/downloads#lts-downloads
    const val ndkVersion = "23.1.7779620"

    // https://github.com/JLLeitschuh/ktlint-gradle/releases
    const val ktlintGradle = "10.2.0"

    // https://github.com/google/dagger/releases
    const val hiltAndroid = "2.40.5"

    // https://mvnrepository.com/artifact/androidx.media/media
    // https://mvnrepository.com/artifact/androidx.preference/preference-ktx
    // https://mvnrepository.com/artifact/androidx.recyclerview/recyclerview
    object SupportLibs {
        const val media = "androidx.media:media:1.5.0-beta01"
        const val preferenceKtx = "androidx.preference:preference-ktx:1.2.0-beta01"
        const val recyclerview = "androidx.recyclerview:recyclerview:1.3.0-alpha01"
    }

    // https://mvnrepository.com/artifact/com.google.android.material/material
    object Material {
        private const val version = "1.6.0-alpha01"
        const val materialComponents = "com.google.android.material:material:$version"
    }

    // https://mvnrepository.com/artifact/androidx.activity/activity-compose
    // https://mvnrepository.com/artifact/androidx.navigation/navigation-compose
    object Compose {
        const val activity = "androidx.activity:activity-compose:1.4.0"
        const val animation = "androidx.compose.animation:animation:$composeVersion"
        const val foundation = "androidx.compose.foundation:foundation:$composeVersion"
        const val graphics = "androidx.compose.animation:animation-graphics:$composeVersion"
        const val iconsCore = "androidx.compose.material:material-icons-core:$composeVersion"
        const val iconsExtended = "androidx.compose.material:material-icons-extended:$composeVersion"
        const val livedata = "androidx.compose.runtime:runtime-livedata:$composeVersion"
        const val material = "androidx.compose.material:material:$composeVersion"
        const val navigation = "androidx.navigation:navigation-compose:2.4.0-beta02"
        const val tooling = "androidx.compose.ui:ui-tooling:$composeVersion"
        const val ui = "androidx.compose.ui:ui:$composeVersion"
        const val uiUtil = "androidx.compose.ui:ui-util:$composeVersion"

        // https://github.com/google/accompanist/releases
        object Accompanist {
            private const val version = "0.21.4-beta"
            const val insets = "com.google.accompanist:accompanist-insets:$version"
            const val controller = "com.google.accompanist:accompanist-systemuicontroller:$version"
            const val permissions = "com.google.accompanist:accompanist-permissions:$version"
        }

        // https://github.com/vanpra/compose-material-dialogs/releases
        object Dialogs {
            private const val version = "0.6.1"
            const val core = "io.github.vanpra.compose-material-dialogs:core:$version"
        }

        // https://developer.android.com/jetpack/androidx/releases/compose-material3
        object Material3 {
            private const val version = "1.0.0-alpha02"
            const val material3 = "androidx.compose.material3:material3:$version"
        }
    }

    // https://mvnrepository.com/artifact/androidx.hilt/hilt-navigation-compose
    object Hilt {
        const val android = "com.google.dagger:hilt-android:$hiltAndroid"
        const val kaptAndroidCompiler = "com.google.dagger:hilt-android-compiler:$hiltAndroid"
        const val composeNav = "androidx.hilt:hilt-navigation-compose:1.0.0-beta01"
    }

    // https://github.com/greenrobot/EventBus/releases
    object EventBus {
        const val eventBus = "org.greenrobot:eventbus:3.2.0"
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
        const val okHttp = "com.squareup.okhttp3:okhttp:4.9.3"
        const val retrofit = "com.squareup.retrofit2:retrofit:2.9.0"
        const val moshi = "com.squareup.moshi:moshi-kotlin:1.12.0"
    }
}