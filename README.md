# Android applications

Native Android apps sharing one Gradle build, dependency catalog and build conventions.

- `apps/fz`: fuzzy-search Home launcher, `io.lilfeelz.fz`.
- `apps/pull`: Bluetooth force trainer, `io.jakobmelchard.pull`.
- `build-logic`: Android/Compose and lint conventions.
- `gradle/libs.versions.toml`: shared toolchain and dependency versions.

Use JDK17 and the Android SDK:

```sh
make check APP=fz
make check APP=pull
```

Root Gradle `check` includes ktlint and Python release-contract tests. Each app keeps its own application code, version, signing identity and data. Both retain minSDK29 and targetSDK35. Version properties are scoped, for example `-Pfz.versionName=0.1.3 -Pfz.versionCode=103` and corresponding `pull.*` properties.

Releases are built and signed here. Component tags are `fz-vX.Y.Z` and `pull-vX.Y.Z`. Existing Obtainium subscriptions stay on `JakobMelchard/fz` and `JakobMelchard/pull`: those repositories receive the identical verified APK and no longer build releases themselves.

The first migrated releases are fz0.1.4/code104 and pull0.1.7/code107. Both retain the original application IDs and signing certificate. No device installation or Home-app replacement runs automatically.

See [MIGRATION.md](MIGRATION.md) for provenance, executed cutover and verification.
