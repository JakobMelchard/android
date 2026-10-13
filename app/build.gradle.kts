plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "io.jakobmelchard.pull"
  compileSdk = 35
  defaultConfig {
    applicationId = "io.jakobmelchard.pull"
    minSdk = 29
    targetSdk = 35
    // release.yml passes both from the release tag; defaults for local builds.
    versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
    versionName = findProperty("versionName") as String? ?: "0.1.0"
  }
  // Signed from the environment (release.yml); the same keystore signs fz and workouts.
  signingConfigs {
    create("release") {
      System.getenv("KEYSTORE_PATH")?.let { storeFile = file(it) }
      storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
      keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "fz"
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
  kotlinOptions { jvmTarget = "17" }
  buildFeatures { compose = true }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2025.04.01"))
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.activity:activity-compose:1.10.1")
  testImplementation("junit:junit:4.13.2")
}
