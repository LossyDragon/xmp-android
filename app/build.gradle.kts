plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android") version "2.43.2"
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
    id("kotlin-parcelize")
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.6.20"
}

android {
    namespace = "org.helllabs.android.xmp"
    compileSdk = 33

    defaultConfig {
        applicationId = "org.helllabs.android.xmp"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // https://developer.android.com/jetpack/androidx/releases/compose#declaring_dependencies
        kotlinCompilerExtensionVersion = "1.3.1"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }
    }
    applicationVariants.all {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}

dependencies {

    /** Android Libs **/
    // https://developer.android.com/jetpack/androidx/releases/core
    implementation("androidx.core:core-ktx:1.9.0")
    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")

    /** Compose **/
    // https://developer.android.com/jetpack/androidx/releases/activity
    // https://developer.android.com/jetpack/androidx/releases/navigation
    val composeVersion = rootProject.extra["compose_version"]
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation("androidx.compose.material3:material3:1.0.0-alpha15")
    implementation("androidx.compose.material:material-icons-core:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.compose.ui:ui-util:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.navigation:navigation-compose:2.6.0-alpha01")

    /** Accompanist **/
    val accompanistVersion = rootProject.extra["accompanist_version"]
    implementation("com.google.accompanist:accompanist-insets:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")

    /** Compose Destinations **/
    // https://github.com/raamcosta/compose-destinations/releases
    val composeDest = "1.7.19-beta"
    implementation("io.github.raamcosta.compose-destinations:core:$composeDest")
    implementation("io.github.raamcosta.compose-destinations:animations-core:$composeDest")
    ksp("io.github.raamcosta.compose-destinations:ksp:$composeDest")

    /** Compose Dialogs **/
    // https://github.com/vanpra/compose-material-dialogs/releases
    val dialogs = "0.8.1-rc"
    implementation("io.github.vanpra.compose-material-dialogs:core:$dialogs")

    /** Compose Preferences **/
    // https://github.com/Sh4dowSoul/ComposePreferences/releases
    val preferences = "0.1.4"
    implementation("com.github.Sh4dowSoul.ComposePreferences:preferences-material3:$preferences")
    implementation("com.github.Sh4dowSoul.ComposePreferences:datastore-manager:$preferences")

    /** Hilt (DI) **/
    // Hilt Nav Compose: https://developer.android.com/jetpack/androidx/releases/hilt
    val hiltVersion = rootProject.extra["hilt_version"]
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")

    /** Media **/
    // https://developer.android.com/jetpack/androidx/releases/media
    implementation("androidx.media:media:1.6.0")

    /** Logging **/
    // https://github.com/JakeWharton/timber/releases
    implementation("com.jakewharton.timber:timber:5.0.1")

    /** Json **/
    // https://search.maven.org/artifact/com.squareup.moshi/moshi-kotlin
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")

    /** Document Files **/
    // https://developer.android.com/jetpack/androidx/releases/documentfile
    implementation("androidx.documentfile:documentfile:1.0.1")

    /** XML Parser **/
    // https://github.com/pdvrieze/xmlutil/releases
    val xmlUtil = "0.84.2"
    implementation("io.github.pdvrieze.xmlutil:core-android:$xmlUtil")
    implementation("io.github.pdvrieze.xmlutil:serialization-android:$xmlUtil")

    // https://search.maven.org/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    // https://search.maven.org/artifact/com.squareup.retrofit2/retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // https://mvnrepository.com/artifact/com.jakewharton.retrofit/retrofit2-kotlinx-serialization-converter
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")
}

ktlint {
    android.set(true)
    // Ignore: wildcard imports
    disabledRules.add("no-wildcard-imports")
}
