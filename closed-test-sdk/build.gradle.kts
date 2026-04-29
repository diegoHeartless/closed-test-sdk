plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("maven-publish")
    signing
}

android {
    namespace = "io.closedtest.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SDK_VERSION", "\"${libs.versions.closedTestSdk.get()}\"")
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
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
                artifactId = "closed-test-sdk"
                version = libs.versions.closedTestSdk.get()
                from(components["release"])
                pom {
                    name.set("closed-test-sdk")
                    description.set("Closed testing proof / usage SDK for Android (ingest client).")
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

    // When publishing to staging for JReleaser → Central, signatures are created by JReleaser (all files).
    // Set ORG_GRADLE_PROJECT_skipMavenSigning=true in CI for that path. Local `publishToMavenLocal` still uses Gradle signing.
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

    // No `publishGradleModuleMetadata` on this AGP+MavenPublication combo; disable the task instead (Central / JReleaser).
    tasks.withType<org.gradle.api.publish.tasks.GenerateModuleMetadata>().configureEach {
        enabled = false
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Public API (ClosedTestOptions.okHttpClient) — must be api so apps compile against the SDK.
    api(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}
