plugins {
  id("java")
  alias(libs.plugins.intellij)
  alias(libs.plugins.compose)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  id("org.jetbrains.kotlin.jvm")
}

group = project.properties["group.id"].toString()
version = project.properties["pluginVersion"].toString()

repositories {
  mavenCentral()
  google()
}

dependencies {
  implementation(project(":Kdux-devtools-common"))
  implementation(compose.desktop.currentOs)
  implementation(libs.kotlin.serialization)
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.websockets)
  implementation(libs.ktor.serialization)
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2023.2.6")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf("android", "java"))
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
    untilBuild.set("243.*")
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

tasks.register("prepareKotlinBuildScriptModel") {}
