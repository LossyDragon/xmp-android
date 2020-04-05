// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    val kotlinVersion = "1.3.71"

    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.1.0-alpha04")
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath("org.jlleitschuh.gradle:ktlint-gradle:9.1.1")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "9.1.1"
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
