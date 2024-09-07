pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
        kotlin("jvm") version "2.0.10-RC2" apply false
    }
}

rootProject.name = "Kdux.Project"
include(":Kdux")
include(":Kdux-kotlinx-serialization")
include(":Kdux-gson")
include(":Kdux-android")
