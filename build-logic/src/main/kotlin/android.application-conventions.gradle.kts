import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

val signingPrefix = project.name.uppercase(Locale.ROOT)

android {
    compileSdk = 36
    defaultConfig {
        minSdk = 29
        targetSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    signingConfigs {
        create("release") {
            storeFile = providers.environmentVariable("${signingPrefix}_KEYSTORE_PATH").orNull?.let(::file)
            storePassword = providers.environmentVariable("${signingPrefix}_KEYSTORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("${signingPrefix}_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("${signingPrefix}_KEY_PASSWORD").orNull ?: storePassword
        }
    }
    buildTypes {
        release { signingConfig = signingConfigs.getByName("release") }
    }
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
ktlint { version = catalog.findVersion("ktlint").get().requiredVersion }

dependencies {
    ktlintRuleset(catalog.findLibrary("compose-rules").get())
    implementation(platform(catalog.findLibrary("compose-bom").get()))
    implementation(catalog.findLibrary("activity-compose").get())
    testImplementation(catalog.findLibrary("junit").get())
}
