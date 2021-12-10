import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlinx-serialization")
    id("dagger.hilt.android.plugin")
    id("org.jlleitschuh.gradle.ktlint")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    externalNativeBuild {
        ndkBuild.path("src/main/cpp/Android.mk")
    }

    // Hush ExperimentalCoroutinesApi
    kotlinOptions {
        jvmTarget = "1.8"
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
        kotlinCompilerExtensionVersion = Dependencies.Compose.version
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", Dependencies.kotlinVersion))

    // Android Support Libs
    api(Dependencies.SupportLibs.preferenceKtx)
    implementation(Dependencies.SupportLibs.media)
    implementation(Dependencies.SupportLibs.recyclerview)

    implementation(Dependencies.Material.materialComponents)

    implementation(Dependencies.Compose.Accompanist.controller)
    implementation(Dependencies.Compose.Accompanist.insets)
    implementation(Dependencies.Compose.Accompanist.permissions)
    implementation(Dependencies.Compose.Material3.material3)
    implementation(Dependencies.Compose.activity)
    implementation(Dependencies.Compose.animation)
    implementation(Dependencies.Compose.foundation)
    implementation(Dependencies.Compose.graphics)
    implementation(Dependencies.Compose.iconsCore)
    implementation(Dependencies.Compose.iconsExtended)
    implementation(Dependencies.Compose.livedata)
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Compose.navigation)
    implementation(Dependencies.Compose.tooling)
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.uiUtil)

    // AIDL-like replacement
    implementation(Dependencies.EventBus.eventBus)

    // Dep Injection
    implementation(Dependencies.Hilt.android)
    implementation(Dependencies.Hilt.composeNav)
    kapt(Dependencies.Hilt.kaptAndroidCompiler)

    // Material Dialogs
    implementation(Dependencies.Compose.Dialogs.core)

    // Http & Download
    implementation(Dependencies.XFetch2.fetch)
    implementation(Dependencies.XFetch2.okHttp)
    implementation(Dependencies.SquareUp.okHttp)
    implementation(Dependencies.SquareUp.retrofit)
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")
    implementation("io.github.pdvrieze.xmlutil:core-android:0.84.0-RC2-SNAPSHOT")
    implementation("io.github.pdvrieze.xmlutil:serialization-android:0.84.0-RC2-SNAPSHOT")

    // Other Libs
    implementation(Dependencies.SquareUp.moshi)

    // LeakCanary
    // debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")
}

ktlint {
    android.set(true)
    // Ignore: wildcard imports
    disabledRules.add("no-wildcard-imports")
}

tasks {
    // Register manually: ktlintCheck, ktlintFormat, xmp

    val fetchXmp by registering(Exec::class) {
        val args = "rm -rf libxmp && git clone https://github.com/libxmp/libxmp.git && exit"
        val file = File("../app/src/main/cpp")
        workingDir(file)
        commandLine("bash", "-c", args)
    }

    // sudo apt install build-essential autoconf -y
    val buildXmp by registering(Exec::class) {
        val args = "autoconf && ./configure && make && make check && " +
            "(cd test-dev; autoconf && ./configure && make) && exit"
        val file = File("../app/src/main/cpp/libxmp")
        workingDir(file)
        commandLine("bash", "-c", args)
    }

    // Combined task to fetch a new copy of libxmp, then build it.
    register("xmp") {
        dependsOn(fetchXmp)
        dependsOn(buildXmp).mustRunAfter(fetchXmp)
    }
}
