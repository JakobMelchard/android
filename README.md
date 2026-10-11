# fz

Android launcher that is one text field: type, fuzzy-match app labels, Enter launches the top hit.

```sh
make build   # debug APK
make test    # unit tests
make home    # install on SERIAL and set as Home (emulator by default)
```

Install from a release with [Obtainium](https://obtainium.imranr.dev): add `https://github.com/JakobMelchard/fz` as the source, APK from GitHub releases. Each release carries a signed `fz-<version>.apk`.

| Path | Contents |
|---|---|
| `app/src/main/java/io/lilfeelz/fz/` | the launcher: `MainActivity.kt`, `Tokens.kt` |
| `app/src/test/` | unit tests for the fuzzy match |
