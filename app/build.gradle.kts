import com.android.build.gradle.internal.api.BaseVariantOutputImpl

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
        minSdkVersion(21)
        targetSdkVersion(29)

        vectorDrawables.useSupportLibrary = true

        versionCode = 90
        versionName = "4.15.0"

        // https://stackoverflow.com/a/58035977
        applicationVariants.forEach { variant ->
            variant.outputs
                    .map { it as BaseVariantOutputImpl }
                    .forEach { output ->
                        output.outputFileName = output.outputFileName
                                .replace("app-", "xmp-")
                                .replace(".apk", "-${variant.versionName}." +
                                        "${variant.versionCode}.apk")
                    }
        }

        // ModArchive API Key
        // Must be in your `Global` gradle.properties! ex: C:\Users\<name>\.gradle
        buildConfigField("String", "ApiKey", project.property("ModPlug_ApiKey") as String)

        externalNativeBuild {
            cmake {
                version = "3.10.2"
                abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
            isJniDebuggable = true
            versionNameSuffix = "-dev"
        }
        getByName("release") {
            isMinifyEnabled = true
            isDebuggable = false
            isShrinkResources = true
            isZipAlignEnabled = true
            isJniDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-project.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    externalNativeBuild {
        cmake {
            setPath("src/main/cpp/CMakeLists.txt")
        }
    }
    ndkVersion = "21.0.6113669"
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta4")
    implementation("androidx.recyclerview:recyclerview:1.2.0-alpha02")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0-beta01")
    implementation("androidx.preference:preference:1.1.0")
    implementation("androidx.media:media:1.1.0")
    implementation("androidx.core:core-ktx:1.2.0")

    implementation("com.google.android.material:material:1.2.0-alpha05")

    implementation("androidx.core:core-ktx:1.3.0-beta01")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.71")

    implementation("com.h6ah4i.android.widget.advrecyclerview:advrecyclerview:1.0.0")

    val materialDialogs = "3.3.0"
    implementation("com.afollestad.material-dialogs:core:$materialDialogs")
    implementation("com.afollestad.material-dialogs:input:$materialDialogs")
    implementation("com.afollestad.material-dialogs:bottomsheets:$materialDialogs")

    implementation("com.github.razir.progressbutton:progressbutton:1.0.3")

    implementation("androidx.tonyodev.fetch2:xfetch2:3.1.4")

    // implementation("org.greenrobot:eventbus:3.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.4.1")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.2")
}

// Run: gradlew ktlintCheck
ktlint {
    android.set(true)
    outputToConsole.set(true)
    // Ignore: lexicographic ordering, wildcard imports, and indentation warnings
    disabledRules.set(setOf("import-ordering", "no-wildcard-imports", "parameter-list-wrapping"))
}
