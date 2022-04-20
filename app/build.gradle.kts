import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version Dependencies.kotlinVersion
    id("com.google.dagger.hilt.android") version Dependencies.hiltAndroid
    id("org.jlleitschuh.gradle.ktlint") version Dependencies.ktlintGradle
}

android {
    compileSdk = 31
    ndkVersion = Dependencies.ndkVersion

    defaultConfig {
        applicationId = "org.helllabs.android.xmp"
        minSdk = 21
        targetSdk = 29

        versionCode = 90
        versionName = "4.15.0"

        externalNativeBuild.ndkBuild {
            abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // ModArchive API Key
        // Must be in your global gradle.properties. ex: C:\Users\<name>\.gradle
        val apiKey = project.property("modArchiveApiKey") as String
        buildConfigField("String", "API_KEY", apiKey)

        // Pretty print compiled apk with version into and date
        androidComponents.onVariants { variant ->
            variant.outputs.forEach { output ->
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    val date = SimpleDateFormat("YYYYMMdd").format(Date())
                    val type = output.baseName
                    output.outputFileName.set("xmp-$type-$versionName-$versionCode-$date.apk")
                }
            }
        }
    }

    buildTypes {
        debug {
            isJniDebuggable = true
            externalNativeBuild {
                externalNativeBuild.ndkBuild.cFlags("-DDEBUG=1")
            }
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-project.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    externalNativeBuild {
        ndkBuild.path("src/main/cpp/Android.mk")
    }

    // Hush ExperimentalCoroutinesApi
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf(
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }

    // Removes: Coroutines debug artifact
    packagingOptions {
        resources {
            excludes.add("DebugProbesKt.bin")
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Dependencies.composeVersion
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", Dependencies.kotlinVersion))

    // Android Support Libs
    api(Dependencies.SupportLibs.preferenceKtx)
    implementation(Dependencies.SupportLibs.media)
    implementation(Dependencies.SupportLibs.recyclerview)

    // Compose
    debugImplementation(Dependencies.Compose.tooling)
    implementation(Dependencies.Compose.Accompanist.controller)
    implementation(Dependencies.Compose.Accompanist.insets)
    implementation(Dependencies.Compose.Accompanist.permissions)
    implementation(Dependencies.Compose.Dialogs.core)
    implementation(Dependencies.Compose.Material3.material3)
    implementation(Dependencies.Compose.activity)
    implementation(Dependencies.Compose.animation)
    implementation(Dependencies.Compose.foundation)
    implementation(Dependencies.Compose.graphics)
    implementation(Dependencies.Compose.iconsCore)
    implementation(Dependencies.Compose.iconsExt)
    implementation(Dependencies.Compose.livedata)
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Compose.navigation)
    implementation(Dependencies.Compose.toolingPreview)
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.uiUtil)

    // Dep Injection
    implementation(Dependencies.Hilt.android)
    implementation(Dependencies.Hilt.composeNav)
    kapt(Dependencies.Hilt.kaptAndroidCompiler)

    // Http & Download
    implementation(Dependencies.XFetch2.fetch)
    implementation(Dependencies.XFetch2.okHttp)
    implementation(Dependencies.SquareUp.okHttp)
    implementation(Dependencies.SquareUp.retrofit)
    implementation(Dependencies.SquareUp.serialization)
    implementation(Dependencies.XmlUtil.core)
    implementation(Dependencies.XmlUtil.serialization)

    // AIDL-like replacement
    implementation(Dependencies.EventBus.eventBus)

    // Other Libs
    implementation(Dependencies.SquareUp.moshi)

    implementation("com.github.Sh4dowSoul.ComposePreferences:preferences-material3:0.1.4")
    implementation("com.github.Sh4dowSoul.ComposePreferences:datastore-manager:0.1.4")

    // LeakCanary
    // debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")
}

ktlint {
    android.set(true)
    // Ignore: wildcard imports
    disabledRules.add("no-wildcard-imports")
}
