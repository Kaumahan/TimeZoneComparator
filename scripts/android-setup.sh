#!/usr/bin/env bash
# Run once after `npx cap add android`. Safe to run again.
set -e
RES=android/app/src/main/res
mkdir -p "$RES/drawable" "$RES/raw"
cp android-extras/ic_stat_alarm.xml "$RES/drawable/"
# Alarm tones: one source of truth in www/sounds (also used for the in-app preview)
cp www/sounds/tone_*.wav "$RES/raw/"
# Permissions: notifications, exact alarms, boot, vibrate
python3 scripts/patch_android.py perms
echo "Android alarm setup done."
