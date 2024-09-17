plugins {
    kotlin("jvm")
    id("maven-publish")
    signing
}

dependencies {
    implementation(project(":Kdux"))
    implementation(project(":Kdux-devtools-common"))
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization)

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.client.websockets)

    testImplementation(kotlin("test"))
    testImplementation(libs.test.truth)
    testImplementation(libs.test.turbine)
    testImplementation(libs.test.kotlin.coroutines)
    testImplementation(libs.test.mockk)
    testImplementation(libs.gson)
}

val GROUP_ID: String = project.properties["group.id"].toString()
val VERSION: String = project.properties["version"].toString()
val ARTIFACT_ID: String = "Kdux-devtools"
val PUBLICATION_NAME = "KduxDevTools"

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
                        name = "Kdux-devtools"
                        description = """
                                Kdux-devtools enables live inspection and time-travel debugging for Kdux Stores.
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