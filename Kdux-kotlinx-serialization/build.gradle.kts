plugins {
    kotlin("jvm")
    id("maven-publish")
    signing
    kotlin("plugin.serialization") version "1.9.0" apply true
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

val GROUP_ID: String = project.properties["group.id"].toString()
val VERSION: String = project.properties["version"].toString()
val ARTIFACT_ID: String = "Kdux-kotlinx-serialization"
val PUBLICATION_NAME = "KduxKotlinxSerialization"

kotlin {
    jvmToolchain(17)
}

plugins.withId("java") {
    java {
        withJavadocJar()
        withSourcesJar()
    }
}

afterEvaluate {
    plugins.withId("maven-publish") {
        publishing {
            publications {
                repositories {
                    maven {
                        name = "Nexus"
                        url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

                        credentials {
                            username = System.getenv("OSSRH_USERNAME") ?: ""
                            password = System.getenv("OSSRH_PASSWORD") ?: ""
                        }
                    }
                    mavenLocal()
                }

                create<MavenPublication>(PUBLICATION_NAME) {
                    from(components["java"])
                    groupId = GROUP_ID
                    artifactId = ARTIFACT_ID
                    version = VERSION
                    pom {
                        name = "Kdux"
                        description = """
                                Kdux is a Kotlin-based, platform-agnostic state management library that implements the Redux pattern, 
                                providing structured concurrency with built-in coroutine support. It is designed to integrate seamlessly 
                                with any Kotlin project, particularly excelling in Android applications using MVI architecture.
                            """.trimIndent()
                        url = "https://github.com/mattshoe/kdux"
                        properties = mapOf(
                            "myProp" to "value"
                        )
                        packaging = "aar"
                        inceptionYear = "2024"
                        licenses {
                            license {
                                name = "The Apache License, Version 2.0"
                                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                            }
                        }
                        developers {
                            developer {
                                id = "mattshoe"
                                name = "Matthew Shoemaker"
                                email = "mattshoe81@gmail.com"
                            }
                        }
                        scm {
                            connection = "scm:git:git@github.com:mattshoe/kdux.git"
                            developerConnection = "scm:git:git@github.com:mattshoe/kdux.git"
                            url = "https://github.com/mattshoe/kdux"
                        }
                    }
                }


                signing {
                    val signingKey = providers.environmentVariable("GPG_SIGNING_KEY")
                    val signingPassphrase = providers.environmentVariable("GPG_SIGNING_PASSPHRASE")
                    if (signingKey.isPresent && signingPassphrase.isPresent) {
                        useInMemoryPgpKeys(signingKey.get(), signingPassphrase.get())
                        sign(publishing.publications[PUBLICATION_NAME])
                    }
                }
            }
        }
    }

    tasks.register<Zip>("generateZip") {
        val publishTask = tasks.named(
            "publish${PUBLICATION_NAME.replaceFirstChar { it.uppercaseChar() }}PublicationToMavenLocalRepository",
            PublishToMavenRepository::class.java
        )
        from(publishTask.map { it.repository.url })
        archiveFileName.set("${PUBLICATION_NAME}_${VERSION}.zip")
    }
}