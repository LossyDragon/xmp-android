pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        jcenter() // xFetch2
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Compose preferences
    }
}
rootProject.name = "xmp android"
include(":app")
