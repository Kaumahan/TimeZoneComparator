#!/usr/bin/env bash
# Adds the native Android parts: home-screen widget, alarm engine, calendar bridge.
# Safe to run again. Skip a feature by setting DISABLE_WIDGET / DISABLE_ALARM / DISABLE_CALENDAR to "true".
set -e
DST=android/app/src/main
copy_feature() {  # $1 = folder under android-extras
  mkdir -p "$DST/java"
  [ -d "android-extras/$1/java" ] && cp -R "android-extras/$1/java/." "$DST/java/"
  [ -d "android-extras/$1/res" ] && cp -R "android-extras/$1/res/." "$DST/res/"
  return 0
}
[ "$DISABLE_WIDGET" = "true" ]   || copy_feature widget
[ "$DISABLE_ALARM" = "true" ]    || copy_feature alarm
[ "$DISABLE_CALENDAR" = "true" ] || copy_feature calendar
python3 scripts/patch_android.py native
echo "Native features added."
