// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    val kotlinVersion = "1.3.70"

    repositories {
        google()
        jcenter()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.0.0-beta03")
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath("org.jlleitschuh.gradle:ktlint-gradle:9.1.1")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven {
            // Volley Snapshot
            url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local/")
        }
    }
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "9.1.1"
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
