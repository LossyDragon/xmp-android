buildscript {
    extra.apply {
        set("kotlin_version", "1.7.10")
        // Roadmap: https://developer.android.com/jetpack/androidx/compose-roadmap
        // https://developer.android.com/jetpack/androidx/releases/compose#versions
        set("compose_version", "1.2.0-alpha08")
        // https://github.com/google/dagger/releases
        set("hilt_version", "2.43.2")
        // https://github.com/google/accompanist/releases
        set("accompanist_version", "0.26.2-beta")
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "7.3.0-rc01" apply false
    id("com.android.library") version "7.3.0-rc01" apply false
    id("com.google.dagger.hilt.android") version "2.43.2" apply false
    id("org.jetbrains.kotlin.android") version "1.7.10" apply false
}

/**
 * Tasks
 */
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

tasks.register("fetchXmp", Exec::class) {
    val libXmpDir = "${rootProject.projectDir}/libxmp/src/main/cpp"
    val args = "rm -rf libxmp && git clone https://github.com/libxmp/libxmp.git && exit"
    val file = File(libXmpDir)
    workingDir(file)
    commandLine("bash", "-c", args)
}

// sudo apt install build-essential autoconf -y
tasks.register("buildXmp", Exec::class) {
    val libXmpDir = "${rootProject.projectDir}/libxmp/src/main/cpp/libxmp"
    val args = "autoconf && ./configure && make && make check && " +
        "(cd test-dev; autoconf && ./configure && make) && exit"
    val file = File(libXmpDir)
    workingDir(file)
    commandLine("bash", "-c", args)
}

// Combined task to fetch a new copy of libxmp, then build it.
tasks.register("xmp") {
    dependsOn("fetchXmp")
    dependsOn("buildXmp").mustRunAfter("fetchXmp")
}
