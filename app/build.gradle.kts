@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.support.KotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.kotlin)
    alias(libs.plugins.gradle.kotlinter)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compiler)
    alias(libs.plugins.stability.analyzer)
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
    namespace = "org.helllabs.android.xmp"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.helllabs.android.xmp"

        /*
         * https://apilevels.com/
         */
        minSdk = 24 // Android 7 - Nougat
        targetSdk = 36 // Android 15 Vanilla Ice Cream

        versionCode = 124
        versionName = "5.0-SNAPSHOT"

        ndk.abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        externalNativeBuild.cmake {
            cppFlags += listOf("-std=c++17")
            arguments += listOf(
                "-DCMAKE_BUILD_TYPE=Release", // DEBUG
                "-DBUILD_SHARED=OFF",
                "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                "-DANDROID_STL=c++_shared"
            )
        }

        // ModArchive API Key
        // Must be in your global gradle.properties. ex: C:\Users\<name>\.gradle
        val apiKey = project.property("modArchiveApiKey") as String
        buildConfigField("String", "API_KEY", apiKey)
    }

    buildFeatures {
        compose = true
        buildConfig = true
        prefab = true
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
        }
    }

    splits {
        abi {
            // isEnable = true
            isUniversalApk = true
        }
    }

    // ./gradlew updateLintBaseline
    lint {
        baseline = file("lint-baseline.xml")
    }

    externalNativeBuild.cmake {
        path = file("src/main/jni/CMakeLists.txt")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-XXLanguage:+ExplicitBackingFields")
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    debugImplementation(libs.leakcanary.android)
    debugImplementation(libs.compose.ui.tooling.preview)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.compose.utils)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)

    implementation(libs.bundles.ktor)
    implementation(libs.bundles.nav3)
    implementation(libs.datastore.preferences)
    implementation(libs.dfc)
    implementation(libs.kotlinx.immutable)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.media)
    implementation(libs.oboe)
    implementation(libs.reorderable)
    implementation(libs.timber)
}
