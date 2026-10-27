# Android migration

## Release handover

The owner requested completing distribution. Signed candidates fz0.1.4/code104 and pull0.1.7/code107 retain their original IDs and certificate SHA256 d3f7764e66bd5041c2afdc71dca287f88008ab0ddb48aa5750c66270f7f429fd. No device installation was performed. The publisher verifies these properties and refuses to replace existing APK bytes. It publishes the canonical component release, then mirrors identical downloads to the original repositories so existing Obtainium URLs continue working. Development and signing have one authority.

Release jobs use RUNNER_MACOS because hosted Actions is blocked by billing. Manual dispatch can retry an existing component release without incrementing its version. The compatibility publisher gets a short-lived app token restricted to android and the selected original repository; signing credentials stay in the separate build job.

The historical preparation record below describes earlier gates; actual cutover status is recorded after execution.


## Remaining owner decisions

- Which original certificate/alias and highest distributed versionCode must each app retain, and who may verify them?
- Which test devices may receive signed upgrade tests, including fz Home/default state and pull sessions?
- Migrate Obtainium feeds explicitly, or maintain old-repository compatibility releases; how long should feeds overlap?
- Who approves first component versions, pausing original publishers and enabling destination release variables?
- When may each original repository stop development? Any later archive needs separate approval; deletion is not proposed.

## Completed source provisioning

`JakobMelchard/android` is private and contains both apps. The owner approved the names services, android and apple, repository creation, and using local checks to merge template PR77 and infra PR337 while hosted Actions is blocked by billing.

Creation used the infrastructure/template path. A saved OpenTofu plan created only the private repository, two runner variables and standard security settings: five creates, no changes or deletions. Encrypted GitHub state advanced189 to190 and was committed immediately. Infra PR337 and template PR77 are merged. The provisioner completed its source import with local Terraform validation, full hooks and scaffold checks. Housekeeping infra PR338 removed the temporary `new:true` declaration flag after a separate owner-approved local-check exception. Repository provisioning and its bookkeeping are complete.

The template render is pinned to commit1c641984047b9a9960d14b984283fdaad85b55f4. The preparation source is commitf75e0f24c6b8bd4eac3fa0a5f29f87214a649522 from the local consolidation pilot. The first scaffold commit is b0e94362df5093a238e177cc808b48ed4cfc991c. Template version tags must come from release-please; no tag was created by hand.

Automatic releases are explicitly disabled with repository variable `ANDROID_RELEASE_ENABLED=false`. The first release workflow run was skipped. Existing source repositories, releases, Obtainium feeds and installed apps remain unchanged. Source import does not switch release authority.

## Provenance and build boundaries

Initial import commit d167341bd90b8f8b19aa8ec9c42de39c2ac932d5 retains both original histories as parents:

- fz:78ce62c93017b17b77f4d3a93b16f464c2582fbc, six source/resource/test files.
- pull:9a4a415e116946830fcf510df096a6df2cfaf37e, seventeen source/resource/test files.

All23 original application files remain byte-identical. Each app owns its namespace, application ID, persistence, screenshots and changelog. Shared build logic supplies Android/Compose/lint conventions; a single wrapper and catalog supply Gradle9.8.2, AGP9.4.1, Compose compiler2.4.21, compileSDK36 and Java17. Both apps retain minSDK29 and targetSDK35. No shared domain/UI library was introduced.

services retains workouts and its related mobile shells. Workspace clients remain with workspaces. apple is reserved for standalone macOS/iOS apps; no empty repository was created. Firmware stays outside these application collections. Shared workflows/configs belong to .github, scaffolding to template and resource declarations to infra.

## Verified locally

The provisioned source passed both debug builds, fz47 and pull35 unit tests with no failures/errors/skips, root/app ktlint and Android lint. All121 generated build tasks executed. Eleven synthetic release tests and36 template tests pass. Hooks and actionlint pass. Both original histories and application IDs are retained;56 checked source-owned build/app/release files survived the overlay byte-for-byte. Hosted CI is still blocked by account billing, so no hosted green result is claimed.

## Release and signing contract

The release workflow pins shared release.yml at df18859960a81d988ca3c7498bdd6a2dc976fec9, v2.6.0. Before release-please can run, the ref must be main, `ANDROID_RELEASE_ENABLED` must be `true` and `ANDROID_RELEASE_REPOSITORY` must exactly match the repository. Enablement requires a separate publication decision.

A read-only plan validates `paths_released` and the component `releases` map before selecting signing jobs. Only released apps are built, at their component tag, with `scripts/release-apk <app> <version>`. Tags are `fz-vX.Y.Z` and `pull-vX.Y.Z`; assets are `fz-X.Y.Z.apk` and `pull-X.Y.Z.apk`. A separate writable upload job runs no checkout or build code and receives no signing credentials or OIDC.

Signing keeps the source workflows' Infisical identity, project, environment and non-recursive `/release` path. Existing `ANDROID_KEYSTORE_B64`/`ANDROID_KEYSTORE_PASSWORD`, plus optional alias/key password, map to the selected `FZ_*` or `PULL_*` helper inputs. No new GitHub signing secrets or copied keys are needed. Verify the destination OIDC binding and each app's original signing certificate/alias before enablement. No real signing key was read during source provisioning.

Versions remain independent. Initial release manifests seed fz0.1.3 and pull0.1.6. Keep major*10000+minor*100+patch versionCode with minor/patch below100; compare every next release against the highest versionCode actually distributed for that app. A shared build change checks both apps and does not imply publishing both.

## Device and Obtainium continuity

Keep `io.lilfeelz.fz` and `io.jakobmelchard.pull`, their existing private signing keys and aliases, and increasing versionCodes. After explicit device authorization, upgrade an original signed installation in place and verify fz launcher state/default Home choice and pull sessions/settings/BLE behavior. Do not uninstall or reset data. Debug/JVM/synthetic tests cannot establish signed-device continuity.

Keep both original repository URLs and historical APKs available. A new repository does not redirect either Obtainium feed. Recommend explicit feed migration with an overlap period; compatibility releases in the old repositories are an alternative requiring separately approved publishing. Compatibility APKs must be byte-identical, with one release authority rather than competing versionCodes.

Export each user's current Obtainium configuration before changing it. The destination needs per-component release filtering and exact APK filtering, for example `^fz-[0-9]+\.[0-9]+\.[0-9]+\.apk$` and corresponding pull. Inspect actual release titles when configuring title filters; do not assume titles equal tags. Verify that publishing one app does not hide the other's latest APK. Test private-repository access, version extraction and an in-place update before distributing the new feed configuration. See the [Obtainium source documentation](https://wiki.obtainium.imranr.dev/sources/).

## Cutover and rollback, not executed

1. Verify certificates, highest versionCodes, approved-device upgrades and feed overlap.
2. Before enabling the destination publisher, pause original automatic publishers under explicit approval. Preserve original downloads and the chosen compatibility-feed path.
3. Approve each first component release, verify its signed APK and feed, then observe the owner-chosen overlap period.
4. Retire pull development first after its checks pass; retain fz until launcher/Home continuity is proven. Each original remains readable and unarchived unless the owner later approves otherwise.

Before publication, rollback means retaining the original release authority and leaving the destination disabled. After publication, disable destination publishing first, then restore one approved original publisher. Any rollback build needs the same signing identity and a code above every distributed version. Preserve data; uninstall/reinstall and blind APK downgrade are not rollback procedures.
