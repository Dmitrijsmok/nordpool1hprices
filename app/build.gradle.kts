import org.gradle.api.tasks.Copy
import java.io.File


plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.nordpool1hprices"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.nordpool1hprices"
        minSdk = 24
        targetSdk = 34
        versionCode = 8
        versionName = "1.8.5"
    }

    // Build commands:
    // ./gradlew clean assembleRelease publishReleaseApk
    // ./gradlew clean assembleRelease publishAndPushUpdate

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("RELEASE_STORE_FILE")
                ?: project.findProperty("RELEASE_STORE_FILE")
                ?: "nordpool1hprices.jks")
            storePassword = System.getenv("RELEASE_STORE_PASSWORD")?.toString()
                ?: project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")?.toString()
                ?: project.findProperty("RELEASE_KEY_ALIAS")?.toString()
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")?.toString()
                ?: project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui:1.5.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.3")

    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("com.google.android.material:material:1.11.0")

    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.browser:browser:1.8.0")
    // Jetpack DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}

// ==============================
// 📦 Task: Copy release APK
// ==============================
tasks.register<Copy>("publishReleaseApk") {
    group = "publishing"
    description = "Copies and renames the signed release APK with versionName."

    dependsOn("assembleRelease")

    val versionName = android.defaultConfig.versionName ?: "unknown"

    // ✅ Source inside app build
    val releaseApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk")

    // ✅ Destination is sibling repo (outside project folder)
    val destinationDir = layout.projectDirectory.dir("../../nordpool1hprices-updates")

    from(releaseApk)
    into(destinationDir)
    rename { "nordPool1hPrices-v$versionName.apk" }

    doFirst {
        println("📦 Preparing to copy signed release APK...")
        println("🔹 Source: ${releaseApk.get().asFile.absolutePath}")
        println("🔹 Destination: ${destinationDir.asFile.absolutePath}")
    }

    doLast {
        println("✅ APK renamed and copied successfully:")
        println("   ${destinationDir.asFile.absolutePath}/nordPool1hPrices-v$versionName.apk")
    }
}

// ==============================
// 🔧 Task: Generate update.json and push
// ==============================
tasks.register("publishAndPushUpdate") {
    group = "distribution"
    description = "Generates update.json and pushes it to GitLab."

    dependsOn("publishReleaseApk")

    doLast {
        val username = "dmitrijsmok1"
        val versionName = android.defaultConfig.versionName ?: "unknown"

        // ✅ Point to the external updates repo
        val updateDir = File("${project.rootDir}/../nordpool1hprices-updates")
        val apkFile = File(updateDir, "nordPool1hPrices-v$versionName.apk")

        if (!apkFile.exists()) {
            throw GradleException("❌ APK not found at ${apkFile.absolutePath}. Did publishReleaseApk run correctly?")
        }

        val updateJson = """
            {
              "latestVersion": "$versionName",
              "changelog": "New update",
              "apkUrl": "https://gitlab.com/$username/nordpool1hprices-updates/-/raw/main/${apkFile.name}"
            }
        """.trimIndent()

        val outputFile = File(updateDir, "update.json")
        outputFile.writeText(updateJson)
        println("📝 Created update.json for version $versionName at ${outputFile.absolutePath}")

        // --- Git push sequence ---
        exec {
            workingDir(updateDir)
            commandLine("git", "add", ".")
        }
        exec {
            workingDir(updateDir)
            commandLine("git", "commit", "-m", "Release v$versionName")
            isIgnoreExitValue = true
        }
        exec {
            workingDir(updateDir)
            commandLine("git", "push", "origin", "main")
        }

        println("🚀 Update pushed to GitLab successfully.")
    }
}