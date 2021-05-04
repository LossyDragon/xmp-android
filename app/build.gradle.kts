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
    ndkVersion = "23.0.7196353-beta2"

    defaultConfig {
        applicationId = "org.helllabs.android.xmp"
        minSdk = 21
        targetSdk = 30

        versionCode = 92
        versionName = "4.15.0"

        externalNativeBuild.ndkBuild {
            abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // ModArchive API Key
        // Must be in your global gradle.properties. ex: C:\Users\<name>\.gradle
        val apiKey = project.property("modArchiveApiKey")
        buildConfigField("String", "API_KEY", apiKey as String)

        // Pretty print compiled apk with: release type, version name, version code, and date.
        applicationVariants.all {
            outputs.forEach { output ->
                if (output is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                    val date = SimpleDateFormat("YYYYMMdd").format(Date())
                    val type = buildType.name
                    output.outputFileName = "xmp-$type-$versionName-$versionCode-$date.apk"
                }
            }
        }
    }

    buildTypes {
        debug {
            isJniDebuggable = true
            externalNativeBuild.ndkBuild.cFlags("-DDEBUG=1")
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

    // Hush: ExperimentalCoroutinesApi
    // Hush: ExperimentalFoundationApi
    kotlinOptions {
        jvmTarget = "1.8"
        useIR = true
        freeCompilerArgs = listOf(
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }

    externalNativeBuild.ndkBuild.path("src/main/cpp/Android.mk")

    // Removes: Coroutines debug artifact
    packagingOptions.resources.excludes.add("DebugProbesKt.bin")

    buildFeatures.compose = true

    composeOptions {
        kotlinCompilerExtensionVersion = Dependencies.composeVersion
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", Dependencies.kotlinVersion))

    /*******************
     * Jetpack Compose *
     *******************/
    val composeVersion = Dependencies.composeVersion
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.material:material-icons-core:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.runtime:runtime-livedata:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.activity:activity-compose:1.3.0-alpha07")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.0-alpha05")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha04")

    val accompanist = "0.7.1"
    implementation("com.google.accompanist:accompanist-insets:$accompanist")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanist")

    /************************
     * Android Support Libs *
     ************************/
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.media:media:1.3.0")

    /*************************
     * AIDL-like replacement *
     *************************/
    implementation("org.greenrobot:eventbus:3.2.0")

    /*****************
     * Dep Injection *
     *****************/
    val hiltVersion = Dependencies.hiltAndroid
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")

    /**************
     * Coroutines *
     **************/
    val coroutines = "1.4.3"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines")

    /*******************
     * Http & Download *
     *******************/
    implementation("androidx.tonyodev.fetch2:xfetch2:3.1.6")
    implementation("androidx.tonyodev.fetch2okhttp:xfetch2okhttp:3.1.6")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    /************************
     * Retrofit XML Parsing *
     ************************/
    val tikXml = "0.8.13"
    implementation("com.tickaroo.tikxml:annotation") { version { strictly(tikXml) } }
    implementation("com.tickaroo.tikxml:retrofit-converter") { version { strictly(tikXml) } }
    kapt("com.tickaroo.tikxml:processor") { version { strictly(tikXml) } }

    /****************
     * JSON Adapter *
     ****************/
    implementation("com.squareup.moshi:moshi-kotlin:1.11.0")

    /***********
     * Dialogs *
     ***********/
    val materialDialogs = "3.3.0"
    implementation("com.afollestad.material-dialogs:core:$materialDialogs")
    implementation("com.afollestad.material-dialogs:lifecycle:$materialDialogs")

    /**************
     * LeakCanary *
     **************/
    // debugImplementation("com.squareup.leakcanary:leakcanary-android:2.6")
}

/******************
 * ktLint options *
 ******************/
ktlint {
    android.set(true)
    // ignoreFailures.set(true)
    // Ignore: wildcard imports
    disabledRules.add("no-wildcard-imports")
}

/****************
 * Gradle Tasks *
 ****************/
tasks {
    // Register: ktlintFormat
    // Register: xmp

    val fetchXmp by registering(Exec::class) {
        val args = "rm -rf libxmp && git clone https://github.com/libxmp/libxmp.git && exit"
        workingDir = File("../app/src/main/cpp")
        commandLine("bash", "-c", args)
    }

    val buildXmp by registering(Exec::class) {
        val args = "autoconf && ./configure && make && make check && " +
            "(cd test-dev; autoconf && ./configure && make) && exit"
        workingDir = File("../app/src/main/cpp/libxmp")
        commandLine("bash", "-c", args)
    }

    // Combined task to fetch a new copy of libxmp, then build it.
    register("xmp") {
        dependsOn(fetchXmp)
        dependsOn(buildXmp).mustRunAfter(fetchXmp)
    }
}
