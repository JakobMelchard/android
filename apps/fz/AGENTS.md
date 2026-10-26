# AGENTS.md

Commands and layout are in [README.md](README.md). Use the Makefile, not `./gradlew` from a shell with the default JDK.

- Colours and radii come from `Tokens.kt`, never literals. It is a hand port of the dark tokens in `JakobMelchard/web` `dist/tokens.json`; update it by hand when they change.
- Ranking is `fuzzy` score plus `ln(1 + frecency)`; frecency is `launches / (1 + days since last launch)`, stored per `pkg/cls` in the `usage` SharedPreferences as `count|epochMs`.
- `onResume` clears the query. The app list comes from `LauncherApps` for every profile in `UserManager.userProfiles`; it loads once in `onCreate` and again from a `LauncherApps.Callback` (package changes in any profile) and a receiver for `ACTION_MANAGED_PROFILE_ADDED`/`REMOVED`, off the main thread. Apps outside fz's own user carry the profile serial, show a muted `work` suffix and keep usage under `pkg/cls@serial`; `usage` keys of apps no longer listed are dropped on each load, unless the list is empty.
- Long-press on a row opens an action row (info, uninstall, forget). Back closes it first, otherwise clears the query; it never leaves the launcher.
- `onResume` requests the Home role through `RoleManager` once per process when fz does not hold it; while it does not, the count line shows `set as home`, which opens the Home app settings.
- Pass `SERIAL`; never run `make home` against a phone without the owner's go, it replaces their Home app.
