plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "io.lilfeelz.fz"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.lilfeelz.fz"
        minSdk = 29
        targetSdk = 35
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = findProperty("versionName") as String? ?: "0.1"
    }
    signingConfigs {
        create("release") {
            storeFile = System.getenv("KEYSTORE_PATH")?.let(::file)
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        }
    }
    buildTypes {
        release { signingConfig = signingConfigs.getByName("release") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    lint {
        warningsAsErrors = true
        // Renovate owns versions; fz is the Home app and never lists itself, so it needs no icon.
        disable +=
            setOf(
                "GradleDependency",
                "NewerVersionAvailable",
                "OldTargetApi",
                "MissingApplicationIcon"
            )
    }
}

kotlin {
    compilerOptions { allWarningsAsErrors = true }
}

ktlint { version = "1.8.0" }

dependencies {
    ktlintRuleset("io.nlopez.compose.rules:ktlint:0.6.8")
    implementation(platform("androidx.compose:compose-bom:2025.12.01"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.13.0")
    testImplementation("junit:junit:4.13.2")
}
