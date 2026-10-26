#!/usr/bin/env bash
# Regenerate docs/screenshots on a running emulator: debug build in demo mode (synthetic pulls)
# with the sessions in scripts/demo/sessions.json. It wipes the app's data, so it refuses real devices.
set -euo pipefail
cd "$(dirname "$0")/.."
adb=${ADB:-adb}
app=io.jakobmelchard.pull
out=docs/screenshots

[ "$("$adb" shell getprop ro.kernel.qemu | tr -d '\r')" = 1 ] || { echo "screenshots: not an emulator (set ANDROID_SERIAL)" >&2; exit 1; }

ui() { "$adb" exec-out uiautomator dump /dev/tty 2>/dev/null || true; }
# Tap the centre of the first view whose text is $1.
tap() {
  local b x y
  b=$(ui | grep -o "text=\"$1\"[^>]*" | grep -o 'bounds="[^"]*"' | sed -n 1p | tr '[],"' '    ' | awk '{print int(($2+$4)/2), int(($3+$5)/2)}') || true
  [ -n "$b" ] || { echo "screenshots: no view \"$1\"" >&2; exit 1; }
  read -r x y <<<"$b"
  "$adb" shell input tap "$x" "$y"
  sleep 1.5
}
shot() { "$adb" exec-out screencap -p >"$out/$1.png"; }
sysui() { "$adb" shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }

../../gradlew -p ../.. -q :apps:pull:assembleDebug
"$adb" install -r -g build/outputs/apk/debug/pull-debug.apk >/dev/null
night=$("$adb" shell cmd uimode night | tr -d '\r' | awk '{print $NF}')
"$adb" shell cmd uimode night yes >/dev/null
"$adb" shell settings put global sysui_demo_allowed 1
sysui enter
sysui clock -e hhmm 1200
sysui battery -e level 100 -e plugged false
sysui network -e wifi show -e level 4 -e mobile show -e level 4
sysui notifications -e visible false
trap 'sysui exit; "$adb" shell cmd uimode night "$night" >/dev/null' EXIT

"$adb" shell pm clear $app >/dev/null
"$adb" push scripts/demo/sessions.json /data/local/tmp/pull-sessions.json >/dev/null
"$adb" shell run-as $app sh -c "'mkdir -p files && cp /data/local/tmp/pull-sessions.json files/sessions.json'"
"$adb" shell am start -n $app/.MainActivity --ez demo true >/dev/null
sleep 5

mkdir -p $out
shot live
tap Max; shot max
tap Grips; shot grips
tap History; shot history
tap Repeat; tap Start
# The demo cycle is not aligned to the app's phases: wait until one dump shows a pull with 3 to 6 s
# left and the force inside the 20 to 25 kg band, so the shot lands mid-pull.
for _ in $(seq 40); do
  d=$(ui)
  echo "$d" | grep -q 'text="next: rest' && echo "$d" | grep -q 'text="[3-6] s"' &&
    echo "$d" | grep -q 'text="2[0-4]\.[0-9] kg"' && break
  sleep 0.5
done
shot repeaters
tap "Stop and save"
echo "wrote live, max, grips, history and repeaters to $out"
