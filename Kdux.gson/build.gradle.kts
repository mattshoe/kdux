plugins {
    kotlin("jvm")
    id("maven-publish")
    signing
    kotlin("plugin.serialization") version "1.9.0" apply true
}

ext {
    set("ARTIFACT_ID", "Kdux.gson")
    set("PUBLICATION_NAME", "kduxGson")
}

dependencies {
    implementation(project(":Kdux"))
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation(kotlin("test"))
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC.2")
    testImplementation("io.mockk:mockk:1.13.12")
}