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
        if (gradle.startParameter.taskNames.any { task -> task.contains("jreleaser", ignoreCase = true) }) {
            classpath("org.jreleaser:org.jreleaser.gradle.plugin:${libs.versions.jreleaser.get()}")
        }
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
}

/** JReleaser 1.x pulls javax.activation on JDK 17+; only load for jreleaser* tasks (not assemble / staging publish). */
private val jreleaserTasksRequested: Boolean
    get() = gradle.startParameter.taskNames.any { task -> task.contains("jreleaser", ignoreCase = true) }

if (jreleaserTasksRequested) {
    apply(from = rootProject.file("gradle/jreleaser-publish.gradle"))
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
