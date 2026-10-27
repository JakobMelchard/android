# Android migration

The private `JakobMelchard/android` repository owns fz and pull development, shared Gradle configuration and signed builds. The release handover is executed. `ANDROID_RELEASE_ENABLED=true` and `ANDROID_RELEASE_REPOSITORY=JakobMelchard/android`; the original repositories' release workflows are disabled.

## Releases and existing installations

| App | Canonical tag | Version code | Existing Obtainium URL |
|---|---|---|---|
| fz | fz-v0.1.4 | 104 | https://github.com/JakobMelchard/fz |
| pull | pull-v0.1.7 | 107 | https://github.com/JakobMelchard/pull |

The canonical repository and both compatibility feeds serve the same signed APK bytes. Existing Obtainium subscriptions keep their URLs and private-repository credentials. Historical downloads and original repositories remain available and unarchived. Neither original workflow builds new releases.

Both apps retain certificate SHA256 `d3f7764e66bd5041c2afdc71dca287f88008ab0ddb48aa5750c66270f7f429fd`. Their IDs remain `io.lilfeelz.fz` and `io.jakobmelchard.pull`; the previous distributed version codes were103 and106. All four published APKs were downloaded and compared to the signed candidates:

- fz SHA256: `78de0edc71a78f7110555e48114c0d64d315f6843def2283cc8e27f29b423607`
- pull SHA256: `5cf2cc2cff3e1367c8bd419a2201203ec2699555cf233865b92388c7160f7956`

No physical device was installed, reset or uninstalled. Actual device-upgrade behavior, including fz Home selection and pull BLE sessions, still needs an explicitly authorized device test. Certificate, identity and increasing-version checks establish package compatibility; they do not replace that test.

## Release operation

Release-please manages independent component versions and creates draft release PRs. Tags and APK names include the app. Signing uses the existing Infisical non-recursive `/release` inputs, mapped only to the selected FZ or PULL helper. Keys are decoded into temporary files and removed after the build.

Trusted main release jobs use the configured Mac runner. The enabled release controller passed with Infisical OIDC and an app token. Hosted CI remains blocked by billing; the owner approved local checks for this migration, and the first APKs were signed locally with the same checked build helpers. The controller's successful run is [40559087127](https://github.com/JakobMelchard/android/actions/runs/40559087127).

`scripts/publish-apk` verifies the original certificate, app ID, version and increasing feed version, then uploads the canonical asset and the identical compatibility asset. Existing assets are immutable. Publishing gets a short-lived GitHub App token restricted to android and the selected compatibility repository; the signing job is separate. Manual workflow dispatch can retry an existing component tag.

## Source and infrastructure

Initial import `d167341bd90b8f8b19aa8ec9c42de39c2ac932d5` preserves both original histories as parents: fz `78ce62c93017b17b77f4d3a93b16f464c2582fbc`, pull `9a4a415e116946830fcf510df096a6df2cfaf37e`. Original application source/resource/test files were byte-preserved. Each app owns its logic, resources, changelog and data. The shared wrapper, dependency catalog and convention build own Gradle9.8.2, AGP9.4.1, Kotlin/Compose compiler2.4.21, compileSDK36 and Java17. Both retain minSDK29 and targetSDK35.

Repository creation used infrastructure declarations and a saved Android-only plan: five creates, zero changes/deletions, immediately committed encrypted state. Template and bookkeeping changes are merged. Apple clients now live in `JakobMelchard/apple`, including the workspace iOS app and its embedded keyboard. Shared CI/configuration remains in .github and template; platform collections own their app builds.

Local checks passed: both debug builds, fz47/pull35 unit tests, ktlint and Android lint, 17 release contract tests, actionlint and hooks. Both real release APKs passed apksigner and aapt metadata verification. Review findings were fixed and all threads resolved before merge.

## Rollback

Disable canonical publication first. Preserve all published downloads. A replacement build must retain the certificate and use a version code above every distributed version; uninstalling or forcing an APK downgrade is not a rollback. Restoring an original builder or changing a physical device needs owner approval.
