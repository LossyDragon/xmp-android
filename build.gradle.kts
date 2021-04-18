// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    repositories {
        google()
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.0.0-alpha14")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Dependencies.kotlinVersion}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Dependencies.hiltAndroid}")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:${Dependencies.ktlintGradle}")
    }

    allprojects {
        repositories {
            google()
            mavenCentral()
            jcenter()
            maven { url = uri("https://jitpack.io") }
        }
    }
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") version Dependencies.ktlintGradle
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}