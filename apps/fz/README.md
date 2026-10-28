# fz

Android launcher that is one text field: type, fuzzy-match app labels, Enter launches the top hit. Empty query lists apps by frecency.

```sh
make -C ../.. build APP=fz   # debug APK
make -C ../.. test APP=fz    # unit tests
make -C ../.. lint APP=fz    # ktlint with Compose rules; make -C ../.. fmt APP=fz fixes
make -C ../.. emu     # boot the fz_pixel7 AVD
make -C ../.. home APP=fz SERIAL=emulator-5554  # explicit owner action: install and set Home
```

Releases carry a signed `fz-<version>.apk`; add `https://github.com/JakobMelchard/android` to [Obtainium](https://obtainium.imranr.dev) and filter release titles by `fz:`.

| Path | Contents |
|---|---|
| `src/main/java/io/lilfeelz/fz/MainActivity.kt` | app list, fuzzy match, frecency, UI |
| `src/main/java/io/lilfeelz/fz/Tokens.kt` | org design tokens |
| `src/test/` | unit tests for the fuzzy match, frecency and ranking |

Development and signed builds now live in `JakobMelchard/android`, under `apps/fz`. Obtainium subscriptions must use `https://github.com/JakobMelchard/android` and filter release titles by `fz:`. The original repository is retired.
