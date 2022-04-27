plugins {
    id("com.android.library")
    id("com.google.dagger.hilt.android") version "2.41"
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    kotlin("plugin.serialization") version "1.6.20"
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 21
        targetSdk = 32

        consumerProguardFiles("consumer-rules.pro")
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
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
}

dependencies {

    // https://developer.android.com/jetpack/androidx/releases/core
    implementation("androidx.core:core-ktx:1.7.0")

    // https://github.com/google/dagger/releases
    val hiltAndroid = "2.41"
    implementation("com.google.dagger:hilt-android:$hiltAndroid")
    implementation("com.google.dagger:hilt-compiler:$hiltAndroid")

    // https://github.com/pdvrieze/xmlutil/releases
    val xmlUtil = "0.84.1"
    implementation("io.github.pdvrieze.xmlutil:core-android:$xmlUtil")
    implementation("io.github.pdvrieze.xmlutil:serialization-android:$xmlUtil")

    // https://search.maven.org/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // https://search.maven.org/artifact/com.squareup.retrofit2/retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // https://search.maven.org/artifact/com.squareup.moshi/moshi-kotlin/
    implementation("com.squareup.moshi:moshi-kotlin:1.13.0")

    // https://mvnrepository.com/artifact/com.jakewharton.retrofit/retrofit2-kotlinx-serialization-converter
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")
}

ktlint {
    android.set(true)
    // Ignore: wildcard imports
    disabledRules.add("no-wildcard-imports")
}
