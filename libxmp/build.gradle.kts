@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.helllabs.libxmp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 16

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
    }

    externalNativeBuild.cmake {
        path = file("src/main/jni/CMakeLists.txt")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }

    buildFeatures {
        prefab = true
    }

    dependencies {
        implementation(libs.oboe)
        implementation(libs.timber)
    }
}
