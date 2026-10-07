# fz: fuzzy-search Android launcher

Home app that is one text field. Type, fuzzy-match app labels, Enter launches the
top hit. Empty query lists apps by frecency. No icons, widgets or home grid.

## Commands

```sh
make build        # assembleDebug
make test         # testDebugUnitTest
make emu          # boot the fz_pixel7 AVD
make home         # install on SERIAL (default emulator-5554), set as Home
```

The Makefile sets `JAVA_HOME` (JDK 17) and `ANDROID_AVD_HOME`; use it instead of
calling `./gradlew` from a shell with the default JDK.

## Layout

- `app/src/main/java/io/lilfeelz/fz/MainActivity.kt` app list, fuzzy match,
  frecency, UI. One Activity, nothing else
- `app/src/main/java/io/lilfeelz/fz/Tokens.kt` org design tokens
- `app/src/test/` unit tests for `fuzzy`

## Rules

- Colours and radii come from `Tokens.kt`, never literals. It is a hand port of
  `JakobMelchard/.config` `tokens/tokens.json` @v1.2.0 mirroring `tokens.swift`;
  replace it with generated output once `build_tokens.py` emits Kotlin.
- Ranking is `fuzzy` score plus `ln(1 + frecency)`. Frecency is
  `launches / (1 + days since last launch)`, stored per `pkg/cls` in the
  `usage` SharedPreferences as `count|epochMs`.
- `onResume` reloads the app list and clears the query. Pressing Home on the
  launcher itself goes through `onPause`/`onResume` too, so there is no
  package-change receiver.
- Back clears the query; it never leaves the launcher.

## Gotchas

- A physical device may be attached next to the emulator. Pass `SERIAL`; never
  run `make home` against a phone without the owner's go, it replaces their Home app.
- `XDG_CONFIG_HOME` makes `avdmanager` write AVDs to `~/.config/.android/avd`,
  where the emulator does not look without `ANDROID_AVD_HOME`.
