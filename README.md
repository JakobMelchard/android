# pull

Android force trainer for the WH-C06 Bluetooth crane scale, a cheap stand-in for a Tindeq Progressor: live force, max pull tests, repeaters and per-grip high scores.

<table>
  <tr>
    <td align="center"><img alt="Live force with peak, average and a 10 s graph" src="docs/screenshots/live.png" width="160"><br><sub>Live</sub></td>
    <td align="center"><img alt="Max test setup with grip, hand and the high score" src="docs/screenshots/max.png" width="160"><br><sub>Max</sub></td>
    <td align="center"><img alt="Repeaters mid-pull with the target band" src="docs/screenshots/repeaters.png" width="160"><br><sub>Repeaters</sub></td>
    <td align="center"><img alt="Grips with best max per hand" src="docs/screenshots/grips.png" width="160"><br><sub>Grips</sub></td>
    <td align="center"><img alt="History with the max-pull trend" src="docs/screenshots/history.png" width="160"><br><sub>History</sub></td>
  </tr>
</table>

```sh
brew install prek && prek install
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
ANDROID_SERIAL=emulator-5554 scripts/screenshots.sh   # regenerates docs/screenshots, emulator only
```

Releases carry a signed `pull-<version>.apk`; add the repo to [Obtainium](https://github.com/ImranR98/Obtainium) (with a GitHub token, the repo is private) for updates.

| Path | Contents |
|---|---|
| `app/src/main/kotlin/Scale.kt` | scan, packet decode, tare, debug demo feed |
| `app/src/main/kotlin/Store.kt` | sessions, grips, high scores, settings |
| `app/src/main/kotlin/Screens.kt` | the five tabs |
| `app/src/main/kotlin/Theme.kt` | palette, type scale, shared components |
| `app/src/main/kotlin/tokens/Tokens.kt` | colours and radii vendored from [JakobMelchard/web](https://github.com/JakobMelchard/web) `dist/tokens.kt` |
| `scripts/` | `screenshots.sh` and its demo sessions |
| `.github/workflows/release.yml` | release-please, then the signed APK |
