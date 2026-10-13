# pull

Android force trainer for the WH-C06 Bluetooth crane scale, a cheap stand-in for a Tindeq Progressor: live force, max pull tests, repeaters and per-grip high scores.

<p>
  <img alt="Live force with peak, average and a 10 s graph" src="docs/screenshots/live.png" width="190">
  <img alt="Max test setup with grip, hand and the high score" src="docs/screenshots/max.png" width="190">
  <img alt="Repeaters mid-pull with the target band" src="docs/screenshots/repeaters.png" width="190">
  <img alt="Grips with best max per hand" src="docs/screenshots/grips.png" width="190">
  <img alt="History with the max-pull trend" src="docs/screenshots/history.png" width="190">
</p>

Screenshots use demo data and synthetic pulls.

```sh
brew install prek && prek install    # hooks pinned in .pre-commit-config.yaml
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
scripts/screenshots.sh               # emulator only: ANDROID_SERIAL=emulator-5554
```

## Screens

| Tab | What it does |
|---|---|
| Live | Current force, peak, average and a 10 s graph. Record saves the span to history. |
| Max | Pick grip and hand (L, R, Both), 3 s countdown, timed pull. Shows the peak, % bodyweight and whether it beats the grip's high score. |
| Repeat | Pull, rest, reps, sets, set rest and a target band. Beeps on each phase, colours the force by the band, saves average, peak and time in target. |
| Grips | Presets plus your own grips, each with best max for L, R and Both, best repeaters average and last session. Tap to select, delete takes two taps. |
| History | Every session with its grip, and the max-pull trend for the selected grip once it has two max pulls. |

Tare zeroes the reading. The screen stays on while the app is open, since Android pauses scanning with the screen off.

## Scale

The WH-C06 (sold under many names, blue or orange, advertising as `IF_B7`) never takes a connection. It broadcasts the weight in BLE advertisements, about 7 per second; the app scans for them and locks onto the first scale it sees.

| Byte (manufacturer data, company `0x0100`) | Meaning |
|---|---|
| 10..11 | weight in hundredths of the display unit, big-endian |
| 14 high nibble | stable flag (stays 0 on the units tested) |
| 14 low nibble | unit: 0 or 2 lb, 3 st, 4 jin, anything else kg; converted to kg |
| 15 | packet counter |

Confirmed against a real scale: a pull read 28.25 kg in the packets and about 30 kg on the display. Readings over 300 kg are dropped. Android 12+ asks for Nearby devices and Location: scanning without location makes Android filter beacon-like packets such as these. Android 11 and older also need Location switched on.

## Data

Everything stays on the phone: sessions in `files/sessions.json`, grips, bodyweight and the repeaters plan in shared preferences. No network permission.

| Path | Contents |
|---|---|
| `app/src/main/kotlin/Scale.kt` | scan, packet decode, tare, debug demo feed |
| `app/src/main/kotlin/Store.kt` | sessions, grips, high scores, settings |
| `app/src/main/kotlin/Screens.kt` | the five tabs |
| `app/src/main/kotlin/Theme.kt` | palette, type scale and shared components |
| `app/src/main/kotlin/tokens/Tokens.kt` | colours and radii vendored from [JakobMelchard/web](https://github.com/JakobMelchard/web) `dist/tokens.kt` |
| `scripts/screenshots.sh`, `scripts/demo/` | screenshot run and its demo sessions |

Scaffolded from [JakobMelchard/template](https://github.com/JakobMelchard/template) (`android`). Templated files (CI caller, `.pre-commit-config.yaml`, devcontainer, org configs) come back through `copier update`; Renovate opens those updates as PRs.
