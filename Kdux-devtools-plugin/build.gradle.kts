plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.0.0-RC1"
  id("org.jetbrains.intellij") version "1.17.3"
  id("org.jetbrains.compose") version "1.6.11"
  kotlin("plugin.serialization") version "1.9.0" apply true
}

group = "org.mattshoe.shoebox"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  google()
}

dependencies {
  implementation(compose.desktop.currentOs)
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
  implementation("io.ktor:ktor-server-core:2.3.12")
  implementation("io.ktor:ktor-server-netty:2.3.12")
  implementation("io.ktor:ktor-server-websockets:2.3.12")
  implementation("ch.qos.logback:logback-classic:1.5.6")
  implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
  implementation("io.github.oleksandrbalan:lazytable:1.8.0")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2023.2.6")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf("android", "java"))
}

sourceSets {
  main {
    resources {
//      srcDirs("src/main/resources")
    }
  }
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("242.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}

tasks {
  runIde {
    ideDir.set(file("/Applications/Android Studio.app/Contents"))  // Path to Android Studio installation
  }

  test {
    useJUnit()  // Run tests for the plugin
  }
}