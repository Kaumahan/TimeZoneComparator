#!/usr/bin/env python3
"""Idempotent edits to the generated Android project. Usage: patch_android.py perms|widget"""
import glob
import re
import sys

MANIFEST = "android/app/src/main/AndroidManifest.xml"


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def write(path, text):
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def perms():
    s = read(MANIFEST)
    wanted = [
        ("POST_NOTIFICATIONS", ""),
        ("RECEIVE_BOOT_COMPLETED", ""),
        ("VIBRATE", ""),
        ("WAKE_LOCK", ""),
        # Android 12/12L only; Android 13+ uses USE_EXACT_ALARM which is granted automatically.
        ("SCHEDULE_EXACT_ALARM", ' android:maxSdkVersion="32"'),
        ("USE_EXACT_ALARM", ""),
    ]
    add = ""
    for name, extra in wanted:
        if 'android.permission.%s"' % name not in s:
            add += '    <uses-permission android:name="android.permission.%s"%s />\n' % (name, extra)
    if add:
        s = s.replace("</manifest>", add + "</manifest>", 1)
        write(MANIFEST, s)
    print("permissions ok")


RECEIVER = """        <receiver
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


def widget():
    s = read(MANIFEST)
    if "ClockWidgetProvider" not in s:
        if "</application>" not in s:
            sys.exit("ERROR: </application> not found in AndroidManifest.xml")
        s = s.replace("</application>", RECEIVER[4:] + "    </application>", 1)
        write(MANIFEST, s)
    print("widget receiver ok")

    files = glob.glob("android/app/src/main/java/**/MainActivity.java", recursive=True)
    if not files:
        print("WARNING: MainActivity.java not found; widget will show default clocks only.")
        return
    path = files[0]
    src = read(path)
    if "WidgetBridgePlugin" in src:
        print("MainActivity already patched")
        return
    m = re.search(r"^package\s+[\w.]+;", src, re.M)
    if not m:
        sys.exit("ERROR: no package line in " + path)
    write(
        path,
        m.group(0)
        + """

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;
import com.tzcomparator.widget.WidgetBridgePlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(WidgetBridgePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
""",
    )
    print("MainActivity patched:", path)


if __name__ == "__main__":
    {"perms": perms, "widget": widget}[sys.argv[1]]()
