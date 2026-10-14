# AGENTS.md

Commands and layout are in [README.md](README.md). Use the Makefile, not `./gradlew` from a shell with the default JDK.

- Colours and radii come from `Tokens.kt`, never literals. It is a hand port of the dark tokens in `JakobMelchard/web` `dist/tokens.json`; update it by hand when they change.
- Ranking is `fuzzy` score plus `ln(1 + frecency)`; frecency is `launches / (1 + days since last launch)`, stored per `pkg/cls` in the `usage` SharedPreferences as `count|epochMs`.
- `onResume` clears the query. The app list loads once in `onCreate` and again from a package-change receiver (`ACTION_PACKAGE_ADDED`, `REMOVED`, `CHANGED`, `REPLACED`, scheme `package`), off the main thread; `usage` keys of apps no longer listed are dropped on each load.
- Back clears the query; it never leaves the launcher.
- Pass `SERIAL`; never run `make home` against a phone without the owner's go, it replaces their Home app.
