plugins { id("android.application-conventions") }

android {
  namespace = "io.jakobmelchard.pull"
  defaultConfig {
    applicationId = "io.jakobmelchard.pull"
    versionCode = providers.gradleProperty("pull.versionCode").orNull?.toInt() ?: 1
    versionName = providers.gradleProperty("pull.versionName").orNull ?: "0.1.0"
  }
}

dependencies {
  implementation(libs.compose.material3)
  implementation(libs.compose.icons)
  testImplementation(libs.json)
}
