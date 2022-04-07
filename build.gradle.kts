// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") } // ktlint-gradle
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.1.2")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Dependencies.hiltAndroid}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Dependencies.kotlinVersion}")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Dependencies.kotlinVersion}")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:${Dependencies.ktlintGradle}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter() // xFetch2
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") } //xmlutil
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
