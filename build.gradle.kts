import org.apache.tools.ant.taskdefs.Java

val NOTHING = "nothing"

plugins {
    alias(libs.plugins.android) apply false
    alias(libs.plugins.android.library) apply false
    id("maven-publish")
    signing
}

val GROUP_ID: String = project.properties["group.id"].toString()
val VERSION: String = project.properties["version"].toString()

group = GROUP_ID
version = VERSION

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }

    group = GROUP_ID
    version = VERSION

    tasks.withType<Test> {
        useJUnit()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}
