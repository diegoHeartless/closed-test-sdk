import org.jreleaser.gradle.plugin.JReleaserExtension

val jreleaserConfigFileName =
    (findProperty("jreleaserConfigFile") as String?)
        ?: System.getenv("JRELEASER_CONFIG_FILE")
        ?: "jreleaser.yml"

configure<JReleaserExtension> {
    dependsOnAssemble = false
    configFile.set(rootProject.layout.projectDirectory.file(jreleaserConfigFileName))
}

tasks.named("jreleaserDeploy") {
    // Intentionally no dependsOn; see publish-maven-central*.yml
}
