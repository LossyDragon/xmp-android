import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    compileSdk = 30
    ndkVersion = Dependencies.androidNdk

    defaultConfig {
        applicationId = "org.helllabs.android.xmp"
        minSdk = 21
        targetSdk = 30

        versionCode = 96
        versionName = "4.15.0"

        externalNativeBuild.ndkBuild {
            abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // ModArchive API Key
        // Must be in your global gradle.properties. ex: C:\Users\<name>\.gradle
        val apiKey = project.property("modArchiveApiKey")
        buildConfigField("String", "API_KEY", apiKey as String)

        // Pretty print compiled apk with: release type, version name, version code, and date.
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
            externalNativeBuild.ndkBuild.cFlags("-DDEBUG=1")
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

    // Hush: ExperimentalCoroutinesApi
    // Hush: ExperimentalFoundationApi
    kotlinOptions {
        jvmTarget = "11"
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
    implementation(Dependencies.Compose.activity)
    implementation(Dependencies.Compose.constraint)
    implementation(Dependencies.Compose.foundation)
    implementation(Dependencies.Compose.iconsCore)
    implementation(Dependencies.Compose.iconsExt)
    implementation(Dependencies.Compose.livedata)
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Compose.tooling)
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.viewmodel)

    implementation(Dependencies.Accompanist.insets)
    implementation(Dependencies.Accompanist.controller)

    /************************
     * Android Support Libs *
     ************************/
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.media:media:1.3.1")

    /*************************
     * AIDL-like replacement *
     *************************/
    implementation("org.greenrobot:eventbus:3.2.0")

    /*****************
     * Dep Injection *
     *****************/
    implementation(Dependencies.Hilt.android)
    kapt(Dependencies.Hilt.compiler)

    /**************
     * Coroutines *
     **************/
    implementation(Dependencies.Coroutines.android)
    implementation(Dependencies.Coroutines.core)

    /*******************
     * Http & Download *
     *******************/
    implementation("androidx.tonyodev.fetch2:xfetch2:3.1.6")
    implementation("androidx.tonyodev.fetch2okhttp:xfetch2okhttp:3.1.6")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    /************************
     * Retrofit XML Parsing *
     ************************/
    implementation(Dependencies.Xml.annotation) { version { strictly(Dependencies.Xml.tikXml) } }
    implementation(Dependencies.Xml.retrofit) { version { strictly(Dependencies.Xml.tikXml) } }
    kapt(Dependencies.Xml.processor) { version { strictly(Dependencies.Xml.tikXml) } }

    /****************
     * JSON Adapter *
     ****************/
    implementation("com.squareup.moshi:moshi-kotlin:1.12.0")

    /***********
     * Dialogs *
     ***********/
    implementation(Dependencies.Dialogs.core)
    implementation(Dependencies.Dialogs.lifecycle)

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
