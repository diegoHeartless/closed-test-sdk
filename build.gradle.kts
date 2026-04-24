plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.jreleaser)
}

version = libs.versions.closedTestSdk.get()

jreleaser {
    dependsOnAssemble = false
    configFile.set(rootProject.layout.projectDirectory.file("jreleaser.yml"))
}

tasks.named("jreleaserDeploy") {
    dependsOn(":closed-test-sdk:publishReleasePublicationToStagingRepository")
}
