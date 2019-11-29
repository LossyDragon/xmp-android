plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    compileSdkVersion(29)

    buildToolsVersion = "29.0.2"

    defaultConfig {
        applicationId = "org.helllabs.android.xmp"
        minSdkVersion(19)
        targetSdkVersion(29)

        vectorDrawables.useSupportLibrary = true

        versionCode = 90
        versionName = "4.15.0"

        ndk {
            abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // ModArchive API Key
        buildConfigField("String", "ApiKey", project.property("ModPlug_ApiKey") as String)
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-project.txt")
        }
    }

    compileOptions {
        setSourceCompatibility(1.8)
        setTargetCompatibility(1.8)
    }

    externalNativeBuild {
        cmake {
            setVersion("3.10.2")
            setPath("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0-alpha03")
    implementation("androidx.preference:preference:1.1.0")
    implementation("androidx.media:media:1.1.0")

    implementation("com.google.android.material:material:1.2.0-alpha02")

    implementation("androidx.core:core-ktx:1.2.0-rc01")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.60")

    implementation("com.h6ah4i.android.widget.advrecyclerview:advrecyclerview:1.0.0")

    val materialDialogs = "3.1.1"
    implementation("com.afollestad.material-dialogs:core:$materialDialogs")
    implementation("com.afollestad.material-dialogs:bottomsheets:$materialDialogs")

    implementation("com.github.razir.progressbutton:progressbutton:1.0.3")

    implementation("androidx.tonyodev.fetch2:xfetch2:3.1.4")

    implementation("com.android.volley:volley:1.2.0-SNAPSHOT")
}

// Run: gradlew ktlintCheck
ktlint {
    android.set(true)
    outputColorName.set("RED")
    outputToConsole.set(true)
    // Ignore: lexicographic ordering, wildcard imports, and indentation warnings
    disabledRules.set(setOf("import-ordering", "no-wildcard-imports", "parameter-list-wrapping"))
}
