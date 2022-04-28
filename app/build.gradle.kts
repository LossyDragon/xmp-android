plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android") version "2.41"
    id("com.google.devtools.ksp") version "1.6.20-1.0.5"
    id("kotlin-parcelize")
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.6.20"
}

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "org.helllabs.android.xmp"
        minSdk = 21
        targetSdk = 32
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
        kotlinCompilerExtensionVersion = rootProject.extra["compose_version"] as String
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

    project(":modarchive")
    project(":libxmp")

    /** Android Libs **/
    // https://developer.android.com/jetpack/androidx/releases/core
    implementation("androidx.core:core-ktx:1.7.0")
    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")

    /** Compose **/
    // https://developer.android.com/jetpack/androidx/releases/activity
    // https://developer.android.com/jetpack/androidx/releases/navigation
    val composeVersion = rootProject.extra["compose_version"]
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.compose.material3:material3:1.0.0-alpha10")
    implementation("androidx.compose.material:material-icons-core:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.compose.ui:ui-util:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.navigation:navigation-compose:2.5.0-beta01")

    /** Accompanist **/
    val accompanistVersion = rootProject.extra["accompanist_version"]
    implementation("com.google.accompanist:accompanist-insets:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")

    /** Compose Destinations **/
    val composeDest = "1.5.1-beta"
    implementation("io.github.raamcosta.compose-destinations:core:$composeDest")
    implementation("io.github.raamcosta.compose-destinations:animations-core:$composeDest")
    ksp("io.github.raamcosta.compose-destinations:ksp:$composeDest")

    /** Compose Dialogs **/
    val dialogs = "0.7.0"
    implementation("io.github.vanpra.compose-material-dialogs:core:$dialogs")

    /** Compose Preferences **/
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
    implementation("androidx.media:media:1.6.0")

    /** Logging **/
    implementation("com.jakewharton.timber:timber:5.0.1")

    /** Json **/
    implementation("com.squareup.moshi:moshi-kotlin:1.13.0")

    /** Document Files **/
    implementation("androidx.documentfile:documentfile:1.0.1")

    // https://issuetracker.google.com/issues/227767363
    debugImplementation("androidx.customview:customview-poolingcontainer:1.0.0-alpha01")
}

ktlint {
    android.set(true)
    // Ignore: wildcard imports
    disabledRules.add("no-wildcard-imports")
}
