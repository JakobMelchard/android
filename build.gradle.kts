plugins {
    base
    alias(libs.plugins.ktlint)
}

ktlint { version.set(libs.versions.ktlint) }
