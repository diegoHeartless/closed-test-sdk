plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.closedtest.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.closedtest.sample"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // Replace with your ingest host and publishable key from the dashboard.
        buildConfigField("String", "INGEST_BASE_URL", "\"https://ingest.example.com\"")
        buildConfigField("String", "PUBLISHABLE_KEY", "\"pk_replace_me\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
    implementation(project(":closed-test-sdk"))
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
