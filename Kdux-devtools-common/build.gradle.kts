plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.0" apply true
}

group = "org.mattshoe.shoebox"
version = "1.0.9"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

kotlin {
    jvmToolchain(17)
}