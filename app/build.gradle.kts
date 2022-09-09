plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android") version Versions.hilt
    id("com.google.devtools.ksp") version Versions.ksp
    id("kotlin-parcelize")
    id("org.jlleitschuh.gradle.ktlint") version Versions.ktLint
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version Versions.kotlin
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // https://developer.android.com/jetpack/androidx/releases/compose#declaring_dependencies
        kotlinCompilerExtensionVersion = Versions.compilerExtension
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
    Libs.androidx.forEach(::implementation)

    /** Compose **/
    Libs.compose.forEach(::implementation)

    /** Accompanist **/
    Libs.accompanist.forEach(::implementation)

    /** Compose Preferences **/
    Libs.composePreferences.forEach(::implementation)

    /** Hilt (DI) **/
    Libs.daggerHilt.forEach(::implementation)
    Libs.daggerHiltKapt.forEach(::kapt)

    /** XML Parser **/
    Libs.xmlUtil.forEach(::implementation)

    /** Compose Destinations **/
    Libs.composeDestination.forEach(::implementation)
    Libs.composeDestinationKsp.forEach(::ksp)

    /** Compose Dialogs **/
    // https://github.com/vanpra/compose-material-dialogs/releases
    implementation("io.github.vanpra.compose-material-dialogs:core:0.8.1-rc")

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
