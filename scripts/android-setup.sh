#!/usr/bin/env bash
# Run once after `npx cap add android`. Safe to run again.
set -e
RES=android/app/src/main/res
M=android/app/src/main/AndroidManifest.xml
mkdir -p "$RES/drawable" "$RES/raw"
cp android-extras/ic_stat_alarm.xml "$RES/drawable/"
cp android-extras/res/raw/alarm_tone.wav "$RES/raw/"

add_perm() {  # $1 = permission name, $2 = optional extra attributes
  if ! grep -q "android.permission.$1\"" "$M"; then
    sed -i.bak "s#</manifest>#    <uses-permission android:name=\"android.permission.$1\" $2/>\n</manifest>#" "$M"
  fi
}
add_perm POST_NOTIFICATIONS
add_perm RECEIVE_BOOT_COMPLETED
add_perm VIBRATE
add_perm WAKE_LOCK
# Exact alarms: SCHEDULE_EXACT_ALARM is needed on Android 12-12L; USE_EXACT_ALARM is granted
# automatically on Android 13+ (Google Play limits it to alarm / calendar style apps).
add_perm SCHEDULE_EXACT_ALARM 'android:maxSdkVersion="32" '
add_perm USE_EXACT_ALARM
rm -f "$M.bak"
echo "Android alarm setup done."
