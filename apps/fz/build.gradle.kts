plugins { id("android.application-conventions") }

android {
    namespace = "io.lilfeelz.fz"
    defaultConfig {
        applicationId = "io.lilfeelz.fz"
        versionCode = providers.gradleProperty("fz.versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("fz.versionName").orNull ?: "0.1"
    }
    lint {
        warningsAsErrors = true
        disable +=
            setOf(
                "GradleDependency",
                "AndroidGradlePluginVersion",
                "NewerVersionAvailable",
                "OldTargetApi",
                "MissingApplicationIcon"
            )
    }
}

kotlin { compilerOptions { allWarningsAsErrors = true } }

dependencies { implementation(libs.compose.foundation) }
