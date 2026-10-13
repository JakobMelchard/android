# AGENTS.md

Commands and layout are in [README.md](README.md). Use the Makefile, not `./gradlew` from a shell with the default JDK.

- Colours and radii come from `Tokens.kt`, never literals. It is a hand port of the dark tokens in `JakobMelchard/web` `dist/tokens.json`; update it by hand when they change.
- Ranking is `fuzzy` score plus `ln(1 + frecency)`; frecency is `launches / (1 + days since last launch)`, stored per `pkg/cls` in the `usage` SharedPreferences as `count|epochMs`.
- `onResume` reloads the app list and clears the query. No package-change receiver.
- Back clears the query; it never leaves the launcher.
- Pass `SERIAL`; never run `make home` against a phone without the owner's go, it replaces their Home app.
