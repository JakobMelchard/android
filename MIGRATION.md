# Consolidation pilot and cutover gates

This is a local reviewable pilot, not a published replacement for the existing repositories.

## Provenance

Both source default-branch histories are parents of import commit d167341bd90b8f8b19aa8ec9c42de39c2ac932d5. The original application sources are moved, not rewritten. The initial import retains each original tree under apps/<name>, before build consolidation.

- fz: 78ce62c93017b17b77f4d3a93b16f464c2582fbc; 6 source/resource/test files retained byte-for-byte.
- pull: 9a4a415e116946830fcf510df096a6df2cfaf37e; 17 source/resource/test files retained byte-for-byte.

No remote named origin exists. Source remotes point to local source clones and have blocked push URLs. Source repositories, remotes, installed applications and release channels are unchanged.

## Ownership and build

Each app owns its source, namespace, application ID, persistence, screenshots and changelog. Included build-logic owns common Android application/Compose/lint setup; a single version catalog and wrapper own build dependencies. Per-app editor conventions preserve the existing code formatting. The compiler/SDK baseline follows fz; pull retains its targetSDK and minSDK. No shared domain/UI abstraction was invented.

The shared baseline is AGP9.4.1, Gradle9.8.2, Compose compiler2.4.21, Compose BOM2025.12.01, compileSDK36 and Java17. Preserve targetSDK35 and minSDK29 for both apps. Common catalog entries are versions to maintain together, not a mandate to publish both apps together.

## Verification

Both original apps passed unit tests, debug APK build, ktlint and Android lint before migration. The shared build passes the same checks for both apps. Source hash, application ID and history ancestry checks passed after relocation. Separate version inputs are tested in actual debug APK metadata. The release helper has six regression cases for component task/version/keystore isolation, explicit alias retention, missing-key and invalid-input rejection, and key cleanup on failure. These tests use synthetic keystores and stub tools only.

CI is one call of the pinned shared Android workflow at v2.5.0 with both apps' tasks, so the required `ci / ci` and `ci / pr-title` names stay. The `hooks / prek` job and prek config arrive with the template scaffold commit. Local actionlint and shellcheck run before review. Hosted CI has not run for this unpublished repository. No signing secret was fetched; no production APK signing, device installation, Home-app replacement or signed upgrade test occurred.

## Release and provisioning gates

1. Settle destination repository identity and visibility. Preserve existing access boundaries. A new GitHub repository must use the infrastructure/template new-repository provisioning path, not an ordinary workspace-created hook or an ad-hoc gh repo create.
2. Integrate this multi-module shape with the Android template rather than copying single-app scaffolds into each module. Preserve the platform's agent policy, review and hook contracts during provisioning.
3. The prepared release-please manifest keeps fz0.1.3 and pull0.1.6 separately and emits component tags. The existing shared release workflow exposes only one release's tag/version outputs; add a backward-compatible component release contract before enabling automatic publication. No release workflow is enabled in this pilot.
4. Use component-scoped signing inputs. `scripts/release-apk fz x.y.z` reads only FZ_* credentials; pull reads only PULL_*. Retain each installed application's actual keystore/alias and validate signing certificate continuity without exposing private keys. VersionCode keeps major*10000+minor*100+patch; reject minor/patch >=100 to avoid collisions.
5. Verify signed upgrade installation on explicitly approved test devices and check existing persisted launcher usage and trainer sessions. Debug builds and JVM tests do not prove device migration.
6. Preserve old repository releases and Obtainium feeds. Decide whether old feeds receive compatibility releases or users explicitly migrate feeds; verify behavior before archiving old source repositories. Do not delete old remotes/history.
7. A component-specific source change releases that component only. Shared build changes check both. Signing jobs and writable release upload jobs stay separate as in the current platform.
8. Update infra inventory, component ownership, docs and scaffold consumers only when cutover is ready. Record rollback to the original repositories/releases. No retirement is implicit in this pilot.

## Out of scope

attach remains with workspaces and its shared Swift/SSH/iPad code. Workouts and offline routine viewer stay in services pending an online/offline product contract. Content recovery has its own Paseo session. No app feature work or app retirement is part of this build consolidation.
