/**
 * Applied only for Maven Central staging / jreleaserDeploy (see root build.gradle.kts).
 * Version must match gradle/libs.versions.toml → jreleaser.
 */
plugins {
    id("org.jreleaser") version "1.25.0"
}

val jreleaserConfigFileName =
    (findProperty("jreleaserConfigFile") as String?)
        ?: System.getenv("JRELEASER_CONFIG_FILE")
        ?: "jreleaser.yml"

jreleaser {
    dependsOnAssemble.set(false)
    configFile.set(rootProject.layout.projectDirectory.file(jreleaserConfigFileName))
}

// jreleaserDeploy does not auto-stage modules — each workflow publishes its staging repo first.
tasks.named("jreleaserDeploy") {
    // Intentionally no dependsOn; see publish-maven-central*.yml
}
