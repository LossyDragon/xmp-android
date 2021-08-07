import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("org.jlleitschuh.gradle.ktlint")
}

// https://developer.android.com/ndk/downloads
android {
    compileSdk = 30
    ndkVersion = "23.0.7123448-beta1"

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
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    // Removes: Coroutines debug artifact
    packagingOptions {
        resources {
            excludes.add("DebugProbesKt.bin")
        }
    }

    // View binding
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", Dependencies.kotlinVersion))

    // Android Support Libs
    implementation(Dependencies.SupportLibs.appCompat)
    implementation(Dependencies.SupportLibs.cardView)
    implementation(Dependencies.SupportLibs.constraintLayout)
    implementation(Dependencies.SupportLibs.material)
    implementation(Dependencies.SupportLibs.media)
    implementation(Dependencies.SupportLibs.preferenceKtx)
    implementation(Dependencies.SupportLibs.recyclerview)
    implementation(Dependencies.SupportLibs.swipeRefreshLayout)

    // AIDL-like replacement
    implementation(Dependencies.EventBus.eventBus)

    // Dep Injection
    implementation(Dependencies.Hilt.android)
    implementation(Dependencies.Hilt.viewModel)
    kapt(Dependencies.Hilt.compiler)

    // Coroutines
    implementation(Dependencies.Coroutines.android)
    implementation(Dependencies.Coroutines.core)

    // Retrofit XML Parsing
    val tikXmlVersion = Dependencies.TikXml.version
    implementation(Dependencies.TikXml.annotation) { version { strictly(tikXmlVersion) } }
    implementation(Dependencies.TikXml.converter) { version { strictly(tikXmlVersion) } }
    kapt(Dependencies.TikXml.processor) { version { strictly(tikXmlVersion) } }

    // Material Dialogs
    implementation(Dependencies.MaterialDialogs.core)
    implementation(Dependencies.MaterialDialogs.input)
    implementation(Dependencies.MaterialDialogs.lifecycle)

    // Http & Download
    implementation(Dependencies.XFetch2.fetch)
    implementation(Dependencies.XFetch2.okHttp)
    implementation(Dependencies.SquareUp.okHttp)
    implementation(Dependencies.SquareUp.retrofit)

    // Other Libs
    implementation(Dependencies.SquareUp.moshi)
    implementation(Dependencies.ProgressButton.progressbutton)

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
        workingDir(File("../app/src/main/cpp"))
        commandLine("bash", "-c", args)
    }

    val buildXmp by registering(Exec::class) {
        val args = "autoconf && ./configure && make && make check && " +
            "(cd test-dev; autoconf && ./configure && make) && exit"
        workingDir(File("../app/src/main/cpp/libxmp"))
        commandLine("bash", "-c", args)
    }

    // Combined task to fetch a new copy of libxmp, then build it.
    register("xmp") {
        dependsOn(fetchXmp)
        dependsOn(buildXmp).mustRunAfter(fetchXmp)
    }
}
