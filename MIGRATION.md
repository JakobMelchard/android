# Android migration

`JakobMelchard/android` owns pull development and signed builds. The repository is public. `ANDROID_RELEASE_ENABLED=true` and `ANDROID_RELEASE_REPOSITORY=JakobMelchard/android` gate publishing. The original standalone repository is retired; compatibility publishing is removed.

## Release feed

Obtainium subscriptions must use `https://github.com/JakobMelchard/android` with a release-title filter of `pull:`. The first migrated release was `pull-v0.1.7`, versionCode107.

Pull retains application ID `io.jakobmelchard.pull` and certificate SHA256 `d3f7764e66bd5041c2afdc71dca287f88008ab0ddb48aa5750c66270f7f429fd`. Its previous distributed versionCode was106. The first migrated APK's SHA256 was `5cf2cc2cff3e1367c8bd419a2201203ec2699555cf233865b92388c7160f7956`.

No physical device was used for app installation, app-data reset or app uninstallation during cutover. Actual device upgrades and BLE sessions still require an explicitly authorized device test.

## Source provenance

Initial import `d167341bd90b8f8b19aa8ec9c42de39c2ac932d5` preserves pull history at `9a4a415e116946830fcf510df096a6df2cfaf37e`. Application source, resources and tests were byte-preserved at import. Pull retains minSDK29, targetSDK35 and Java17.

## Release authentication

Set repository variable `ANDROID_RELEASE_IDENTITY` to the Infisical identity for local signing and upload jobs. Bind that identity to issuer `https://token.actions.githubusercontent.com`, audience `https://github.com/JakobMelchard`, subject `repo:JakobMelchard@265134052/android@1435967147:ref:refs/heads/main`, and these exact claims:

```json
{
  "repository": "JakobMelchard/android",
  "repository_owner": "JakobMelchard",
  "ref": "refs/heads/main",
  "workflow_ref": "JakobMelchard/android/.github/workflows/release.yml@refs/heads/main"
}
```

The current Infisical plan supports project-wide Viewer access, not per-secret custom roles. `gha-android` uses Viewer in `gha` (all project secrets, read-only), restricted by the exact claims and subject above. Signing loads `/release`; publishing requests only `APP_CLIENT_ID` and `APP_PRIVATE_KEY` individually at `/`. The reusable release-please workflow continues using the shared identity, whose `job_workflow_ref` restriction does not match local jobs.
