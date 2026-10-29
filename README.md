# Android application

Pull is a Bluetooth force trainer, `io.jakobmelchard.pull`, in `apps/pull`.

`build-logic` owns Android/Compose and lint conventions; `gradle/libs.versions.toml` owns the toolchain and dependency versions.

Use JDK17 and the Android SDK:

```sh
make check
```

Root Gradle `check` includes ktlint and Python release-contract tests. Pull retains minSDK29 and targetSDK35. Version properties are scoped: `-Ppull.versionName=0.1.8 -Ppull.versionCode=108`.

Releases are built and signed here with tags `pull-vX.Y.Z`. Obtainium subscriptions use `https://github.com/JakobMelchard/android`, with a release-title filter of `pull:`. The original repository is retired.

The first migrated release was pull0.1.7/code107. Pull retains its original application ID and signing certificate. Device installation requires explicit approval.

See [MIGRATION.md](MIGRATION.md) for provenance, cutover and verification.
