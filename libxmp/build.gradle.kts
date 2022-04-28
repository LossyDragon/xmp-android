plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
}

android {
    compileSdk = 32

    // https://developer.android.com/ndk/downloads#lts-downloads
    ndkVersion = "23.1.7779620"

    defaultConfig {
        minSdk = 21
        targetSdk = 32

        externalNativeBuild.ndkBuild {
            abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            isJniDebuggable = true
            externalNativeBuild {
                externalNativeBuild.ndkBuild.cFlags("-DDEBUG=1")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        ndkBuild.path("src/main/cpp/Android.mk")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

ktlint {
    android.set(true)
    // Ignore: wildcard imports
    disabledRules.add("no-wildcard-imports")
}
