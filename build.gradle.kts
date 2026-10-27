plugins {
    base
    alias(libs.plugins.ktlint)
}

ktlint { version.set(libs.versions.ktlint) }

val releaseScriptTests by tasks.registering(Exec::class) {
    commandLine("python3", "-m", "unittest", "discover", "-s", "scripts", "-p", "test_*.py")
}

tasks.named("check") {
    dependsOn(releaseScriptTests, "ktlintCheck", ":apps:fz:ktlintCheck", ":apps:pull:ktlintCheck")
}
