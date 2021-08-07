object Dependencies {

    const val kotlinVersion = "1.5.21"

    // https://github.com/JLLeitschuh/ktlint-gradle/releases
    const val ktlintGradle = "10.1.0"

    // https://github.com/google/dagger/releases
    const val hiltAndroid = "2.38.1"

    object SupportLibs {
        const val appCompat = "androidx.appcompat:appcompat:1.3.1"
        const val cardView = "androidx.cardview:cardview:1.0.0"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.1.0"
        const val material = "com.google.android.material:material:1.4.0"
        const val media = "androidx.media:media:1.4.1"
        const val preferenceKtx = "androidx.preference:preference-ktx:1.1.1"
        const val recyclerview = "androidx.recyclerview:recyclerview:1.2.1"
        const val swipeRefreshLayout = "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"
    }

    // https://mvnrepository.com/artifact/androidx.hilt/hilt-lifecycle-viewmodel
    object Hilt {
        const val android = "com.google.dagger:hilt-android:$hiltAndroid"
        const val compiler = "com.google.dagger:hilt-compiler:$hiltAndroid"
        const val viewModel = "androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03"
    }

    // https://github.com/Kotlin/kotlinx.coroutines/releases
    object Coroutines {
        private const val version = "1.5.1"
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
        const val okHttp = "com.squareup.okhttp3:okhttp:4.9.1"
        const val retrofit = "com.squareup.retrofit2:retrofit:2.9.0"
        const val moshi = "com.squareup.moshi:moshi-kotlin:1.12.0"
    }

    // https://repo1.maven.org/maven2/com/github/razir/progressbutton/progressbutton/
    object ProgressButton {
        const val progressbutton = "com.github.razir.progressbutton:progressbutton:2.1.0"
    }
}
