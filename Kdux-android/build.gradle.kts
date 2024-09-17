plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.android.library)
    id("maven-publish")
    id("kotlin-parcelize")
    signing
}

android {
    namespace = "com.example.kdux.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":Kdux"))

    testImplementation(kotlin("test"))
    testImplementation(libs.test.truth)
    testImplementation(libs.test.turbine)
    testImplementation(libs.test.kotlin.coroutines)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.robolectric)
}

val GROUP_ID: String = project.properties["group.id"].toString()
val VERSION: String = project.properties["version"].toString()
val ARTIFACT_ID: String = "Kdux-android"
val PUBLICATION_NAME = "KduxAndroid"

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
                    from(components["release"])
                    groupId = GROUP_ID
                    artifactId = ARTIFACT_ID
                    version = VERSION
                    pom {
                        name = "Kdux-android"
                        description = """
                                Kdux-android is an add-on to the Kdux library to add built-in support for the Android platform to leverage
                                its various tools and SDK offerings..
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

//                        withXml {
//                            val dependenciesNode = asNode().appendNode("dependencies")
//                            configurations.getByName("implementation") {
//                                dependencies.forEach {
//                                    val dependencyNode = dependenciesNode.appendNode("dependency")
//                                    dependencyNode.appendNode("groupId", it.group)
//                                    dependencyNode.appendNode("artifactId", it.name)
//                                    dependencyNode.appendNode("version", it.version)
//                                }
//                            }
//                        }
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