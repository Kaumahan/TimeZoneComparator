#!/usr/bin/env python3
"""Idempotent edits to the generated Android project.

Usage: patch_android.py perms
       patch_android.py native      (honours DISABLE_WIDGET / DISABLE_ALARM / DISABLE_CALENDAR = true)
"""
import glob
import os
import re
import sys

MANIFEST = "android/app/src/main/AndroidManifest.xml"


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def write(path, text):
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def off(name):
    return os.environ.get(name, "").strip().lower() == "true"


def add_permissions(s, wanted):
    add = ""
    for name, extra in wanted:
        if 'android.permission.%s"' % name not in s:
            add += '    <uses-permission android:name="android.permission.%s"%s />\n' % (name, extra)
    if add:
        s = s.replace("</manifest>", add + "</manifest>", 1)
    return s


def perms():
    s = read(MANIFEST)
    s = add_permissions(
        s,
        [
            ("POST_NOTIFICATIONS", ""),
            ("RECEIVE_BOOT_COMPLETED", ""),
            ("VIBRATE", ""),
            ("WAKE_LOCK", ""),
            # Only used by the fallback notification path (Android 12/12L). The main alarm engine uses
            # AlarmManager.setAlarmClock, which needs no exact-alarm permission, so USE_EXACT_ALARM is not requested.
            ("SCHEDULE_EXACT_ALARM", ' android:maxSdkVersion="32"'),
        ],
    )
    write(MANIFEST, s)
    print("permissions ok")


WIDGET_RECEIVER = """        <receiver
            android:name="com.tzcomparator.widget.ClockWidgetProvider"
            android:exported="true"
            android:label="@string/widget_label">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/clock_widget_info" />
        </receiver>
"""

ALARM_RECEIVERS = """        <receiver
            android:name="com.tzcomparator.alarm.AlarmReceiver"
            android:exported="false" />
        <receiver
            android:name="com.tzcomparator.alarm.AlarmActionReceiver"
            android:exported="false" />
        <receiver
            android:name="com.tzcomparator.alarm.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>
"""


def native():
    use_widget = not off("DISABLE_WIDGET")
    use_alarm = not off("DISABLE_ALARM")
    use_calendar = not off("DISABLE_CALENDAR")

    s = read(MANIFEST)
    if "</application>" not in s:
        sys.exit("ERROR: </application> not found in AndroidManifest.xml")
    receivers = ""
    if use_widget and "ClockWidgetProvider" not in s:
        receivers += WIDGET_RECEIVER
    if use_alarm and "com.tzcomparator.alarm.AlarmReceiver" not in s:
        receivers += ALARM_RECEIVERS
    if receivers:
        s = s.replace("</application>", receivers[4:] + "    </application>", 1)
    if use_calendar:
        s = add_permissions(s, [("READ_CALENDAR", "")])
    write(MANIFEST, s)
    print("manifest ok (widget=%s alarm=%s calendar=%s)" % (use_widget, use_alarm, use_calendar))

    files = glob.glob("android/app/src/main/java/**/MainActivity.java", recursive=True)
    if not files:
        print("WARNING: MainActivity.java not found; native plugins are not registered.")
        return
    path = files[0]
    m = re.search(r"^package\s+[\w.]+;", read(path), re.M)
    if not m:
        sys.exit("ERROR: no package line in " + path)
    plugins = []
    if use_widget:
        plugins.append("com.tzcomparator.widget.WidgetBridgePlugin")
    if use_alarm:
        plugins.append("com.tzcomparator.alarm.AlarmSchedulerPlugin")
    if use_calendar:
        plugins.append("com.tzcomparator.calendar.CalendarBridgePlugin")
    imports = "".join("import %s;\n" % p for p in plugins)
    registers = "".join("        registerPlugin(%s.class);\n" % p.split(".")[-1] for p in plugins)
    write(
        path,
        m.group(0)
        + "\n\nimport android.os.Bundle;\n\nimport com.getcapacitor.BridgeActivity;\n"
        + imports
        + "\npublic class MainActivity extends BridgeActivity {\n"
        + "    @Override\n    public void onCreate(Bundle savedInstanceState) {\n"
        + registers
        + "        super.onCreate(savedInstanceState);\n    }\n}\n",
    )
    print("MainActivity registers:", ", ".join(p.split(".")[-1] for p in plugins) or "(none)")


if __name__ == "__main__":
    {"perms": perms, "native": native}[sys.argv[1]]()
