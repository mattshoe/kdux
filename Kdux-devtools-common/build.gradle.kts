plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}

group = "org.mattshoe.shoebox"
version = "1.0.10"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlin.coroutines)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

kotlin {
    jvmToolchain(17)
}