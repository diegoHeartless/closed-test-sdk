plugins {
    // JReleaser (and other tooling) expects the standard `clean` task on the root project.
    id("base")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.jreleaser) apply false
}

// When ProofFlow includes this repo via includeBuild, skip release tooling on the composite classpath.
if (gradle.parent == null) {
    apply(plugin = "org.jreleaser")

    val jreleaserConfigFileName =
        (findProperty("jreleaserConfigFile") as String?)
            ?: System.getenv("JRELEASER_CONFIG_FILE")
            ?: "jreleaser.yml"

    jreleaser {
        dependsOnAssemble = false
        configFile.set(rootProject.layout.projectDirectory.file(jreleaserConfigFileName))
    }

    tasks.named("jreleaserDeploy") {
        // Intentionally no dependsOn; see publish-maven-central*.yml
    }
}

// JReleaser PGP + Gradle/AGP: avoid old bcprov on the classpath (NoSuchMethodError on BigIntegers.writeUnsignedByteArray).
subprojects {
    configurations.configureEach {
        resolutionStrategy {
            force(
                "org.bouncycastle:bcprov-jdk18on:1.78.1",
                "org.bouncycastle:bcutil-jdk18on:1.78.1",
                "org.bouncycastle:bcpg-jdk18on:1.78.1",
                "org.bouncycastle:bcpkix-jdk18on:1.78.1",
            )
        }
    }
}

configurations.configureEach {
    resolutionStrategy {
        force(
            "org.bouncycastle:bcprov-jdk18on:1.78.1",
            "org.bouncycastle:bcutil-jdk18on:1.78.1",
            "org.bouncycastle:bcpg-jdk18on:1.78.1",
            "org.bouncycastle:bcpkix-jdk18on:1.78.1",
        )
    }
}

group = "com.groundspaceteam"
version = libs.versions.closedTestSdk.get()
