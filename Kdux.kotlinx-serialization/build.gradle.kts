plugins {
    kotlin("jvm")
    id("maven-publish")
    signing
    kotlin("plugin.serialization") version "1.9.0" apply true
}

ext {
    set("ARTIFACT_ID", "Kdux.kotlinx-serialization")
    set("PUBLICATION_NAME", "kduxKotlinxSerialization")
}

dependencies {
    implementation(project(":Kdux"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")

    testImplementation(kotlin("test"))
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC.2")
    testImplementation("io.mockk:mockk:1.13.12")
}