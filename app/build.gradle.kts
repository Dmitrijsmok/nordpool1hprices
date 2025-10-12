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
        versionCode = 4
        versionName = "1.8"
    }
    buildFeatures {
        buildConfig = true
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
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("RELEASE_STORE_FILE") ?: project.findProperty("RELEASE_STORE_FILE") ?: "nordpool1hprices.jks")
            storePassword = System.getenv("RELEASE_STORE_PASSWORD")?.toString() ?: project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")?.toString() ?: project.findProperty("RELEASE_KEY_ALIAS")?.toString()
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")?.toString() ?: project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
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

}

// === Auto-publish helper (signed release) ===
tasks.register<Copy>("publishReleaseApk") {
    group = "publishing"
    description = "Copies and renames the signed release APK with versionName."

    // Ensure the release build happens first
    dependsOn("assembleRelease")

    // Read versionName from Gradle Android config
    val versionName = android.defaultConfig.versionName ?: "unknown"

    // Source: signed APK location
    val releaseApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk")

    // Destination: your custom update folder
    val destinationDir = layout.projectDirectory.dir("../../nordpool1hprices-updates")

    from(releaseApk)
    into(destinationDir)

    // Rename to include version
    rename { "nordPool1hPrices-v$versionName.apk" }

    doFirst {
        println("📦 Preparing to copy signed release APK...")
        println("🔹 Source: ${releaseApk.get().asFile.absolutePath}")
        println("🔹 Destination: ${destinationDir.asFile.absolutePath}")
    }

    doLast {
        println("✅ APK renamed and copied successfully:")
        println("   ${destinationDir.asFile.absolutePath}/app-release-v$versionName.apk")
    }
}

tasks.register("publishAndPushUpdate") {
    group = "publishing"
    description = "Publishes signed APK, updates JSON, and pushes to GitLab."

    dependsOn("publishReleaseApk")

    doLast {
        val versionName = android.defaultConfig.versionName ?: "unknown"
        val updateDir = file("${project.rootDir}/../nordpool1hprices-updates")

        // 1️⃣ Generate update.json dynamically
        val updateJson = """
            {
              "latestVersion": "$versionName",
              "changelog": "Updated via automated Gradle publish task.",
              "apkUrl": "https://gitlab.com/<your-username>/nordpool1hprices-updates/-/raw/main/app-release-v$versionName.apk"
            }
        """.trimIndent()

        File(updateDir, "update.json").writeText(updateJson)
        println("📝 Created update.json for version $versionName")

        // 2️⃣ Commit & push to GitLab
        exec {
            workingDir(updateDir)
            commandLine("git", "add", ".")
        }
        exec {
            workingDir(updateDir)
            commandLine("git", "commit", "-m", "Release v$versionName")
            isIgnoreExitValue = true // avoid failure if nothing to commit
        }
        exec {
            workingDir(updateDir)
            commandLine("git", "push", "origin", "main")
        }

        println("🚀 Update pushed to GitLab successfully.")
    }
}

// Helper extension to access android block easily
fun org.gradle.api.Project.androidAppExtension() =
    extensions.getByName("android") as com.android.build.gradle.internal.dsl.BaseAppModuleExtension
//build app
//    ./gradlew clean assembleRelease publishReleaseApk
//commit and push automatically
//    ./gradlew clean assembleRelease publishAndPushUpdate