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

Source import is complete. Automatic releases are disabled with `ANDROID_RELEASE_ENABLED=false`. Existing releases and Obtainium feeds in `JakobMelchard/fz` and `JakobMelchard/pull` remain active until a separately approved cutover. No device installation or Home-app replacement runs automatically.

Signing retains the existing Infisical inputs and maps them to the selected app's `FZ_*` or `PULL_*` helper inputs. Signing certificates and in-place upgrades still need verification before enabling releases.

See [MIGRATION.md](MIGRATION.md) for provenance, completed provisioning and remaining cutover gates.
