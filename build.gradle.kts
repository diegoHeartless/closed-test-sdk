buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        // AGP 9 built-in Kotlin: pin KGP/KSP above AGP defaults (see AGP 9 release notes).
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
    }
}

plugins {
    // JReleaser (and other tooling) expects the standard `clean` task on the root project.
    id("base")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.jreleaser) apply false
}

/** JReleaser 1.x pulls javax.activation on JDK 17+; only load for Central staging/deploy (not assemble / publishToMavenLocal). */
private val jreleaserTasksRequested: Boolean
    get() =
        gradle.startParameter.taskNames.any { task ->
            task.contains("jreleaser", ignoreCase = true) ||
                (task.contains("publish", ignoreCase = true) && task.contains("Staging", ignoreCase = true))
        }

if (jreleaserTasksRequested) {
    apply(plugin = libs.plugins.jreleaser.get().pluginId)
}

// JReleaser PGP + Gradle/AGP: avoid old bcprov on the classpath (NoSuchMethodError on BigIntegers.writeUnsignedByteArray).
subprojects {
    configurations.configureEach {
        resolutionStrategy {
            force(
                "org.bouncycastle:bcprov-jdk18on:1.84",
                "org.bouncycastle:bcutil-jdk18on:1.84",
                "org.bouncycastle:bcpg-jdk18on:1.84",
                "org.bouncycastle:bcpkix-jdk18on:1.84",
            )
        }
    }
}

configurations.configureEach {
    resolutionStrategy {
        force(
            "org.bouncycastle:bcprov-jdk18on:1.84",
            "org.bouncycastle:bcutil-jdk18on:1.84",
            "org.bouncycastle:bcpg-jdk18on:1.84",
            "org.bouncycastle:bcpkix-jdk18on:1.84",
        )
    }
}

group = "com.groundspaceteam"
version = libs.versions.closedTestSdk.get()

val jreleaserConfigFileName =
    (findProperty("jreleaserConfigFile") as String?)
        ?: System.getenv("JRELEASER_CONFIG_FILE")
        ?: "jreleaser.yml"

if (jreleaserTasksRequested) {
    jreleaser {
        dependsOnAssemble = false
        configFile.set(rootProject.layout.projectDirectory.file(jreleaserConfigFileName))
    }

    // jreleaserDeploy does not auto-stage modules — each workflow publishes its staging repo first.
    tasks.named("jreleaserDeploy") {
        // Intentionally no dependsOn; see publish-maven-central*.yml
    }
}
