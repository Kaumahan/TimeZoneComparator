package com.tzcomparator.alarm;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Bridge between the web app and AlarmEngine. */
@CapacitorPlugin(name = "AlarmScheduler")
public class AlarmSchedulerPlugin extends Plugin {

    @PluginMethod
    public void schedule(PluginCall call) {
        try {
            JSArray alarms = call.getArray("alarms");
            int n = AlarmEngine.scheduleAll(getContext(), alarms);
            JSObject result = new JSObject();
            result.put("scheduled", n);
            call.resolve(result);
        } catch (Exception e) {
            call.reject("schedule failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void cancelAll(PluginCall call) {
        AlarmEngine.scheduleAll(getContext(), new JSArray());
        call.resolve();
    }

    @PluginMethod
    public void fireIn(PluginCall call) {
        try {
            Integer seconds = call.getInt("seconds", 5);
            String title = call.getString("title", "Test alert");
            String body = call.getString("body", "Alarms are working.");
            String sound = AlarmEngine.cleanSound(call.getString("sound", "classic"));
            AlarmEngine.ensureChannel(getContext(), sound);
            long at = System.currentTimeMillis() + seconds.intValue() * 1000L;
            AlarmEngine.setOne(getContext(), 2000000001, at, title, body, sound);
            call.resolve();
        } catch (Exception e) {
            call.reject("fireIn failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void status(PluginCall call) {
        Context context = getContext();
        JSObject result = new JSObject();
        boolean notificationsOn = true;
        if (Build.VERSION.SDK_INT >= 24) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationsOn = nm.areNotificationsEnabled();
        }
        boolean ignoringBattery = true;
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            ignoringBattery = pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        result.put("notificationsEnabled", notificationsOn);
        result.put("pending", AlarmEngine.pendingCount(context));
        result.put("next", AlarmEngine.nextAt(context));
        result.put("ignoringBattery", ignoringBattery);
        result.put("sdk", Build.VERSION.SDK_INT);
        call.resolve(result);
    }

    @PluginMethod
    public void openAppSettings(PluginCall call) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getContext().getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        call.resolve();
    }

    @PluginMethod
    public void openBatterySettings(PluginCall call) {
        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            getContext().startActivity(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallback.setData(Uri.parse("package:" + getContext().getPackageName()));
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(fallback);
        }
        call.resolve();
    }
}
