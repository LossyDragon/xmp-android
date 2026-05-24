@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.kotlinter)
    alias(libs.plugins.stability.analyzer)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.get()))
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

/**
 * For GitHub Actions, using 'GHA' build variant
 */
val keyProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keyProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.lossydragon.media3"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lossydragon.media3"

        /*
         * https://apilevels.com/
         */
        minSdk = 26 // Oreo
        targetSdk = 37 // Cinnamon Bun (terrible codename).

        versionCode = 6
        versionName = "1.0"

        ndk.abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

        val apiKey = project.property("modArchiveApiKey") as String
        buildConfigField("String", "API_KEY", apiKey)
    }

    signingConfigs {
        create("GHA") {
            keyAlias = keyProperties["keyAlias"].toString()
            keyPassword = keyProperties["keyPassword"].toString()
            storeFile = keyProperties["storeFile"]?.let { file(it) }
            storePassword = keyProperties["storePassword"].toString()
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isJniDebuggable = true
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("GHA") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("GHA")
            matchingFallbacks += listOf("release")
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
    implementation(libs.compose.placeholder) // TODO try out
    implementation(libs.reorderable)

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

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.bundles.ktor)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
