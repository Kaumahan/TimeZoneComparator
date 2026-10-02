#!/usr/bin/env bash
# Run once after `npx cap add android`. Safe to run again.
set -e
RES=android/app/src/main/res/drawable
M=android/app/src/main/AndroidManifest.xml
mkdir -p "$RES"
cp android-extras/ic_stat_alarm.xml "$RES/"
for P in POST_NOTIFICATIONS SCHEDULE_EXACT_ALARM RECEIVE_BOOT_COMPLETED VIBRATE; do
  if ! grep -q "android.permission.$P" "$M"; then
    sed -i.bak "s#</manifest>#    <uses-permission android:name=\"android.permission.$P\" />\n</manifest>#" "$M"
  fi
done
rm -f "$M.bak"
echo "Android notification setup done."
