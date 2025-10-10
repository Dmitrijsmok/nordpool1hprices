plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 34
    namespace = "com.example.nordpool1hprices"

    defaultConfig {
        applicationId = "com.example.nordpool1hprices"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // match Compose version
    }
}

dependencies {
    // Core + Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui:1.5.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.3")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.3")


    // ✅ Use only one version of Material3
    implementation("androidx.compose.material3:material3:1.3.0")

    // Material Components (for XML themes)
    implementation("com.google.android.material:material:1.11.0")

    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // CSV parsing
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.3")

    // Charts (MPAndroidChart)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
