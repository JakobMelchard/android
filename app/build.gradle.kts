plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "io.lilfeelz.fz"
  compileSdk = 35
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
      keyAlias = "fz"
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
  implementation(platform("androidx.compose:compose-bom:2025.02.00"))
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.activity:activity-compose:1.10.0")
  testImplementation("junit:junit:4.13.2")
}
