#!/usr/bin/env bash
# Adds the home-screen clock widget to the generated Android project. Safe to run again.
set -e
SRC=android-extras/widget
DST=android/app/src/main
mkdir -p "$DST/java/com/tzcomparator/widget"
cp "$SRC"/java/com/tzcomparator/widget/*.java "$DST/java/com/tzcomparator/widget/"
cp -R "$SRC"/res/. "$DST/res/"
python3 scripts/patch_android.py widget
echo "Clock widget added."
