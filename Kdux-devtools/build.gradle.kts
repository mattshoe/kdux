plugins {
    kotlin("jvm")
}

group = "org.mattshoe.shoebox"
version = "1.0.8"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}