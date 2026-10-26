# Native Android applications

Local consolidation pilot for fz (Home launcher) and pull (BLE force trainer).
Both application histories are retained. Existing repositories and releases remain authoritative until an explicit publication migration.

`apps/fz` and `apps/pull` own their application code, identities and data. `build-logic` owns the common Android/Compose defaults. `gradle/libs.versions.toml` owns the tested toolchain and dependency versions. No shared domain/UI library is introduced.

Use JDK17 and the Android SDK. `make check APP=fz` and `make check APP=pull` run the respective tests, debug build and lint. No install or Home-app replacement runs automatically.

Version inputs are scoped: `-Pfz.versionName=0.1.0 -Pfz.versionCode=100`, or corresponding `pull.*` properties. Signing inputs are separately named `FZ_KEYSTORE_PATH`, `FZ_KEYSTORE_PASSWORD`, `FZ_KEY_ALIAS`, optional `FZ_KEY_PASSWORD`, and corresponding `PULL_*` values. Existing signing identities must be retained; this pilot reads no keystores or credentials and publishes no releases.

App IDs remain `io.lilfeelz.fz` and `io.jakobmelchard.pull`; minSDK29 and targetSDK35 stay unchanged. Compile SDK36 and the fz build baseline are shared. Each app retains separate version progression and releases. Existing Obtainium feeds and historical downloads must continue through a verified transition before old repositories can retire.

Source/hash provenance and outstanding release migration steps are recorded in `MIGRATION.md`.
