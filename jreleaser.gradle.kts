plugins {
    id("org.jreleaser") version "1.23.0"
}

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
