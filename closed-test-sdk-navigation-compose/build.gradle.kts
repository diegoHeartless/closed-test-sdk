plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    signing
}

android {
    namespace = "io.closedtest.sdk.navigation"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "staging"
                url = uri(layout.buildDirectory.dir("staging-deploy"))
            }
        }
        publications {
            create<MavenPublication>("release") {
                groupId = "com.groundspaceteam"
                artifactId = "closed-test-sdk-navigation-compose"
                version = libs.versions.closedTestSdk.get()
                from(components["release"])
                pom {
                    name.set("closed-test-sdk-navigation-compose")
                    description.set(
                        "Optional Jetpack Navigation Compose screen tracking for closed-test-sdk (ClosedTest.trackScreen).",
                    )
                    val pomUrl = (findProperty("POM_URL") as String?)
                        ?: "https://github.com/diegoHeartless/closed-test-sdk"
                    url.set(pomUrl)
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set((findProperty("POM_DEVELOPER_ID") as String?) ?: "diegoHeartless")
                            name.set((findProperty("POM_DEVELOPER_NAME") as String?) ?: "Ground Space Team")
                        }
                    }
                    scm {
                        val base = (findProperty("POM_SCM_URL") as String?) ?: pomUrl
                        url.set(base)
                        connection.set(
                            (findProperty("POM_SCM_CONNECTION") as String?)
                                ?: "scm:git:git://github.com/diegoHeartless/closed-test-sdk.git",
                        )
                        developerConnection.set(
                            (findProperty("POM_SCM_DEVELOPER_CONNECTION") as String?)
                                ?: "scm:git:ssh://git@github.com/diegoHeartless/closed-test-sdk.git",
                        )
                    }
                }
            }
        }
    }

    val signingKey = findProperty("signing.key") as String?
    val skipMavenSigning = findProperty("skipMavenSigning")?.toString().equals("true", ignoreCase = true)
    if (!skipMavenSigning && !signingKey.isNullOrBlank()) {
        signing {
            useInMemoryPgpKeys(
                findProperty("signing.keyId") as String?,
                signingKey,
                findProperty("signing.password") as String?,
            )
            sign(publishing.publications["release"])
        }
    }

    tasks.withType<org.gradle.api.publish.tasks.GenerateModuleMetadata>().configureEach {
        enabled = false
    }
}

dependencies {
    api(project(":closed-test-sdk"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
}
