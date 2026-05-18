@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.kotlinter)
    alias(libs.plugins.stability.analyzer)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        jvmTarget.set(JvmTarget.JVM_17)
    }
}


android {
    namespace = "com.lossydragon.media3"


    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lossydragon.media3"

        minSdk = 26
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        ndk.abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

        val apiKey = project.property("modArchiveApiKey") as String
        buildConfigField("String", "API_KEY", apiKey)
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
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    splits {
        abi {
            // isEnable = true
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(project(":libxmp"))

    // Compose
    debugImplementation(libs.compose.ui.tooling.preview)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.kotlinx.immutable)
    implementation(libs.materialKolor)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)

    // Navigation 3
    implementation(libs.bundles.nav3)

    // Media 3
    implementation(libs.media3.common)
    implementation(libs.media3.session)

    // Logging
    implementation(libs.timber)

    implementation("com.anggrayudi:storage:2.2.0")
    implementation("com.anggrayudi:storage-compose:2.2.0")
    implementation(libs.bundles.ktor)
    implementation(libs.datastore.preferences)
}
