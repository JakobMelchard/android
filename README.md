# fz

Android launcher that is one text field: type, fuzzy-match app labels, Enter launches the top hit. Empty query lists apps by frecency.

```sh
make build   # debug APK
make test    # unit tests
make lint    # ktlint with Compose rules; make fmt fixes
make emu     # boot the fz_pixel7 AVD
make home    # install on SERIAL (default emulator-5554) and set as Home
```

Releases carry a signed `fz-<version>.apk`; add `https://github.com/JakobMelchard/fz` to [Obtainium](https://obtainium.imranr.dev).

| Path | Contents |
|---|---|
| `app/src/main/java/io/lilfeelz/fz/MainActivity.kt` | app list, fuzzy match, frecency, UI |
| `app/src/main/java/io/lilfeelz/fz/Tokens.kt` | org design tokens |
| `app/src/test/` | unit tests for the fuzzy match, frecency and ranking |
