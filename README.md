# pull

Android force trainer for the WH-C06 Bluetooth crane scale, a cheap stand-in for a
Tindeq Progressor: live force, max pull tests, repeaters and per-grip high scores.

<table>
  <tr>
    <td align="center"><img alt="Live force with peak, average and a 10 s graph" src="docs/screenshots/live.png" width="160"><br><sub>Live</sub></td>
    <td align="center"><img alt="Max test setup with grip, hand and the high score" src="docs/screenshots/max.png" width="160"><br><sub>Max</sub></td>
    <td align="center"><img alt="Repeaters mid-pull with the target band" src="docs/screenshots/repeaters.png" width="160"><br><sub>Repeaters</sub></td>
    <td align="center"><img alt="Grips with best max per hand" src="docs/screenshots/grips.png" width="160"><br><sub>Grips</sub></td>
    <td align="center"><img alt="History with the max-pull trend" src="docs/screenshots/history.png" width="160"><br><sub>History</sub></td>
  </tr>
</table>

## Install

Download `pull-<version>.apk` from [releases](https://github.com/JakobMelchard/pull/releases).
For automatic updates, add the repository to
[Obtainium](https://github.com/ImranR98/Obtainium) and filter release titles by `pull:`.

## Use

Switch the scale on, open the app and allow Bluetooth and location. It locks onto the
first WH-C06 it hears. Press Tare with nothing hanging.

| Tab | What it does |
|---|---|
| Live | force, peak and average over the last 10 s; Record saves a session |
| Max | countdown, timed max pull per grip and hand, high score and bodyweight percentage |
| Repeat | work/rest repeaters with sets, a target band and time in target |
| Grips | add and delete grips, best max per hand |
| History | every session and the max-pull trend for the current grip |

## Architecture

```text
BLE scale ──▶ Scale.kt ──▶ Screens.kt ──▶ Store.kt
                 │              │            │
                 │              └── theme, layouts
                 └── scan, decode, tare
```

| Path | Contents |
|---|---|
| `src/main/kotlin/Scale.kt` | scan, packet decode, tare, debug demo feed |
| `src/main/kotlin/Store.kt` | sessions, grips, high scores, settings |
| `src/main/kotlin/Screens.kt` | the five tabs |
| `src/main/kotlin/Theme.kt` | palette, type scale, shared components |
| `scripts/` | `screenshots.sh` and its demo sessions |

## Build

```sh
make check
ANDROID_SERIAL=emulator-5554 scripts/screenshots.sh   # regenerates docs/screenshots, emulator only
```

Installation on a physical device is a manual step.

## License

[MIT](LICENSE)
