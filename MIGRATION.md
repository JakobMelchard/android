# Consolidation pilot and cutover gates

## Owner decisions needed

- Approve `JakobMelchard/android`, private, with both existing repositories' reader access preserved?
- Approve template draft PR77 for eventual merge, then restore Actions availability and wait for a release-please template tag?
- Use the prepared GitHub `FZ_*`/`PULL_*` secret adapter, or require a scoped Infisical adapter before enabling releases?
- Which existing signing certificate/alias and highest distributed versionCode must each app retain, and who may verify them?
- Which devices may receive signed upgrade tests, including fz Home/default state and pull persisted sessions?
- Migrate Obtainium feeds explicitly (recommended), or maintain old-repo compatibility releases; how long is the overlap?
- Who approves the first per-app versions, pausing original publishers, and setting both destination release gate variables?
- Approve retiring pull development before fz after the overlap, and is any later archive desired? Nothing authorizes archive or deletion now.

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

CI is one call of the pinned shared Android workflow at v2.5.0 with source-owned root `check` and both apps' tasks, so the required `ci / ci` and `ci / pr-title` names stay. The `hooks / prek` job and prek config arrive with the template scaffold commit. Local actionlint and shellcheck run before review. The component plan adds four test cases, including per-app/both/empty selection and malformed metadata rejection. Both debug builds, unit suites, ktlint and Android lint passed again on 2026-10-27. Hosted CI has not run for this unpublished repository. No signing secret was fetched; no production APK signing, device installation, Home-app replacement or signed upgrade test occurred.

## Destination and provisioning plan

Propose `JakobMelchard/android`, private. The name describes the shared native build while leaving fz and pull as independent products. It allows another Android app without another repository migration. Both existing repositories are private, verified through GitHub on 2026-10-27; preserve their access boundary, collaborators and release-reader access. Name availability and provisioner inventory validation remain cutover checks, not a claim that a destination exists.

Provision only through template `scripts/new` and the infra declaration/apply path. Do not use `gh repo create`, workspace hooks or an ordinary clone to create the destination. No provisioning, infra modification or apply is authorized here. Dry-run command, using the draft template fix's immutable SHA:

```sh
/Users/j/Workspaces/JakobMelchard/template.tree/worktrees/fix/android-multi-checks/scripts/new android android \
  -d 'Native Android applications: fz launcher and pull trainer' \
  --owner JakobMelchard \
  --source /Users/j/Workspaces/.staging/android-20261026 \
  --answer android_layout=multi --answer android_apps=fz,pull \
  --ref e1fc7cf3a3864a585da92098ac436bf354e1961b --dry-run
```

Keep the source clean before running this command. No `--public` means private. The SHA contains multi layout plus source check preservation; it is a draft PR pin, not an approved template release. Template draft PR: https://github.com/JakobMelchard/template/pull/77. As of 2026-10-27, latest template tag v0.3.3 predates multi. There is no open release-please PR. Release run 40475600555 never started because GitHub reports failed payments or a spending limit. After approved merge and restored hosted CI, use a release-please release containing this fix, verify the tag's resolved SHA, and repeat this dry run with that pin. Never tag by hand. The template's own non-Android workflow pins remain outside this change.

The source owns root Gradle `check`, including root/app ktlint and Python release tests. The multi renderer and provisioning validator invoke `check`, then the listed app tasks. This avoids losing checks when generated CI replaces the pilot caller. The template retains one `ci` call, `ci / ci`, `ci / pr-title` and `hooks / prek`. It preserves the source Gradle build, signing ignores, release manifests and release workflow. The pinned dry run succeeded without creating a destination, operation state or infra branch. A disposable local Copier overlay retained all54 checked source-owned build/app/release files byte-for-byte and passed the generated Gradle CI tasks. Local Copier overlay validation is separate from provisioning; no real `scripts/new` run is allowed.

## Component release source, disabled until approved

`.github/workflows/release.yml` calls the released shared workflow at df18859960a81d988ca3c7498bdd6a2dc976fec9, v2.6.0. Every run is blocked before release-please unless the ref is main, `ANDROID_RELEASE_ENABLED` is exactly `true`, and `ANDROID_RELEASE_REPOSITORY` exactly matches `github.repository`. Leave both variables unset through provisioning and review. Setting them is a future explicit publication authorization, not part of this pilot.

The read-only plan job consumes `releases_created`, `paths_released` and the `releases` JSON map. It rejects unknown components, mismatched/duplicate paths, invalid versions and mismatched tags before selecting any signing job. Only released apps enter the matrix. Each APK job checks out its component tag and calls `scripts/release-apk <app> <version>`. Tags are `fz-vX.Y.Z` and `pull-vX.Y.Z`; assets remain `fz-X.Y.Z.apk` and `pull-X.Y.Z.apk`. A separate writable job uploads exactly that component's asset and runs no checkout or build code. Signing jobs have read-only repository permission and no OIDC. Release-please alone uses the platform's app/OIDC contract. If signing/upload fails after a release is created, use GitHub's rerun-failed-jobs path to retain the successful release outputs; inspect missing assets before any full workflow restart.

The proposed credential adapter uses selected GitHub repository secrets: `FZ_KEYSTORE_B64`, `FZ_KEYSTORE_PASSWORD`, optional `FZ_KEY_ALIAS`/`FZ_KEY_PASSWORD`, and corresponding `PULL_*`. A job receives only its selected component's values, exported under the existing helper's names. Owner must approve and populate this adapter from the original signing material, or approve a scoped Infisical adapter instead, before enablement. No key or secret has been read or moved. Do not copy the old unscoped `/release` environment wholesale into both component jobs.

App-local conventional commits advance only their component. Inspect the first release PRs and selected `paths_released` before approval. Current seeds are fz0.1.3 and pull0.1.6, matching latest original releases verified on 2026-10-27. Shared build changes must check both apps; if both need a release, add explicit component changelog/release entries in their release PRs. A shared change alone must not silently synchronize their versions.

## Signing identity and upgrade continuity

For each app, inventory the installed production versionCode and SHA-256 signing certificate from an owner-approved existing release APK or device. Public certificates and metadata are sufficient for comparison; do not expose private keys or passwords. Reuse the exact existing private key and alias. Base64 encoding a different key or generating a replacement breaks upgrade continuity. If the keystore has multiple aliases, configure the existing alias explicitly; the helper refuses ambiguous discovery. Preserve a distinct key password where the original uses one.

Keep `io.lilfeelz.fz` and `io.jakobmelchard.pull`, minSDK29, targetSDK35, namespaces and persistence unchanged. Keep the injective major*10000+minor*100+patch versionCode calculation with minor/patch below100. Before the first release, compare the proposed code to the highest distributed/installed code for that app, not only manifest seeds103/106. The next code must be greater. A release uploaded under a new repository URL can upgrade the same package in place when its signing identity and versionCode are compatible. No uninstall or data reset belongs in this plan.

After separate owner authorization, on identified test devices install the old signed release, record fz launcher state/default Home choice and pull sessions/settings, then upgrade to the new signed APK in place. Verify package IDs, certificate digests, increasing versionCode and retained data. Confirm fz remains a usable Home app and pull retains sessions and BLE behavior. Do not replace the owner's Home app or install anything during this pilot. Local debug/JVM/synthetic tests cannot establish signed device continuity.

## Obtainium feed continuity

Keep both original URLs, tags and APK downloads available: `JakobMelchard/fz` and `JakobMelchard/pull`. A new monorepo does not redirect either old feed. Recommend explicit feed migration with an overlap period; an alternative is owner-approved compatibility releases in each old repository, containing byte-identical APKs from the new component release. Choose one before first publication. Do not rebuild or re-sign compatibility assets, and do not let two independent release paths mint competing versionCodes.

Before moving any tracked app, export its Obtainium configuration and check private-repository reader access. Test two separately configured app entries against the destination: component release filtering plus exact APK filtering (`^fz-[0-9]+\.[0-9]+\.[0-9]+\.apk$` and the corresponding pull regex). Component tag patterns are `^fz-v[0-9]+\.[0-9]+\.[0-9]+$` and corresponding pull. Obtainium's release-title filter matches titles, so inspect the actual release-please title and configure it accordingly; do not assume tag and title are identical. Disable repository-wide latest-tag selection if it selects the sibling app. Demonstrate that an fz-only release never hides pull's latest APK, and vice versa. Test version extraction and one in-place update per app before distributing the new configuration. The current [Obtainium source documentation](https://wiki.obtainium.imranr.dev/sources/) describes release/APK filters and latest-tag behavior; final configuration still needs device validation.

A compatibility-feed strategy requires a separate approved implementation and publishing credential scope. Explicit migration requires confirmed feed updates by every affected user. Neither strategy is implemented or executed here. Preserve old releases indefinitely unless the owner later authorizes otherwise.

## Cutover and source retirement order, not executed

1. Owner settles every question above. Restore hosted CI, review template PR and obtain its release-please release. Verify the final template pin and repeat dry-run/overlay checks with a clean pilot.
2. After explicit provisioning authorization, declare the private destination through `scripts/new`; owner reviews infra PR, merges and runs apply. Provisioner waits for an empty destination and overlays the approved source history. Inspect access, scaffold, release gate variables (unset), required checks and history ancestry before any publication. No merge or apply is authorized now.
3. With separate secret-handling authorization, configure the selected signing adapter. Compare certificates without exposing keys. Verify the approved signed device upgrades and Obtainium overlap plan. Record per-app highest distributed versionCode.
4. With explicit publication approval, establish one release authority: pause both original automatic release paths before enabling destination variables. Keep source downloads, repository access and the agreed compatibility feed path working. Approve each first component release PR and validate the APK, certificate and feed before promoting it to users. Nothing here approves those merges or publication.
5. Observe both app feeds and upgrades for an owner-chosen overlap period. Original repositories remain readable and unarchived. Announce the new source of development only after users can update reliably. Update inventory/ownership/docs in a separately approved cutover task.
6. Recommend retiring pull development first once its sessions/upgrades/feed pass, then fz after launcher/Home continuity is proven. The launcher carries the higher device-availability risk. Retire each only after its users have migrated or compatibility publishing has been verified. Disable redundant automation before considering archive. Archive requires fresh owner approval; deletion is not proposed. Keep both histories, releases, original tags and source remotes.

Rollback: before destination publication, leave original automation authoritative and destination variables unset. After publication, disable destination release enablement first, then restore one explicitly approved original publisher. Never lower versionCode: rebuild any fix with the same app signing identity and a code above every version already distributed. Restore the previous Obtainium configuration only if its feed can deliver a compatible newer APK. Preserve user data; uninstall/reinstall and blind APK downgrade are not rollback steps.

## Out of scope

attach remains with workspaces and its shared Swift/SSH/iPad code. Workouts and offline routine viewer stay in services pending an online/offline product contract. Content recovery has its own Paseo session. No app feature work or app retirement is part of this build consolidation.
