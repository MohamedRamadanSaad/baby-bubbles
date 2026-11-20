#!/usr/bin/env bash
# Takes Play Store screenshots of the real app on an emulator.
set -u
OUT=shots
mkdir -p "$OUT"
PKG=com.mramadan.babybubbles

tap_text() {
  # Taps the centre of the first on-screen element whose text matches $1.
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1
  python3 tools/shots/find.py ui.xml "$1" | while read -r x y; do
    adb shell input tap "$x" "$y"
  done
}

shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }

launch() {
  adb shell am force-stop $PKG
  adb shell am start -W -n $PKG/.MainActivity >/dev/null
  sleep 4
}

dismiss_pin() {
  # Accept the "pin this app" prompt if Android shows one.
  for t in "Got it" "GOT IT" "OK" "Start" "START" "Pin"; do tap_text "$t"; done
}

play_mode() {
  local label="$1" name="$2"
  launch
  tap_text "$label"; sleep 1
  tap_text "ابدأ اللعب"; sleep 3
  dismiss_pin
  sleep 9
  shot "$name"
  # Pop a couple of bubbles near the middle so the pop effect shows.
  adb shell input tap 540 1300; sleep 0.4
  shot "${name}_pop"
  adb shell am task lock stop >/dev/null 2>&1 || true
}

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 1
adb shell cmd uimode night no || true
adb install -r baby-bubbles.apk

launch
shot 01_home
play_mode "الألوان" 02_colors
play_mode "الحيوانات" 03_animals
play_mode "البحر" 04_sea
play_mode "الطيور" 05_birds
play_mode "الغابة" 06_jungle

launch
adb shell input swipe 540 1800 540 600 400; sleep 1
tap_text "صيد الطيور"; sleep 2
shot 07_hunt_setup
tap_text "ابدأ المباراة"; sleep 2
dismiss_pin
sleep 6
shot 08_hunt_play
sleep 3
shot 09_hunt_play2
ls -la "$OUT"
