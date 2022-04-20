// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "7.1.3" apply false
    id("com.android.library") version "7.1.3" apply false
    id("org.jetbrains.kotlin.android") version Dependencies.kotlinVersion apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

tasks.register("fetchXmp", Exec::class) {
    val libXmpDir = "${rootProject.projectDir}/app/src/main/cpp"
    val args = "rm -rf libxmp && git clone https://github.com/libxmp/libxmp.git && exit"
    val file = File(libXmpDir)
    workingDir(file)
    commandLine("bash", "-c", args)
}

// sudo apt install build-essential autoconf -y
tasks.register("buildXmp", Exec::class) {
    val libXmpDir = "${rootProject.projectDir}/app/src/main/cpp/libxmp"
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
