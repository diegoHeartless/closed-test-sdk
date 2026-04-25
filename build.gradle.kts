plugins {
    // JReleaser (and other tooling) expects the standard `clean` task on the root project.
    id("base")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.jreleaser)
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

jreleaser {
    dependsOnAssemble = false
    configFile.set(rootProject.layout.projectDirectory.file("jreleaser.yml"))
}

tasks.named("jreleaserDeploy") {
    dependsOn(":closed-test-sdk:publishReleasePublicationToStagingRepository")
}
