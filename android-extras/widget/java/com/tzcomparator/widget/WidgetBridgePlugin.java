package com.tzcomparator.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Lets the web app send its clock list to the home-screen widget and ask to pin the widget. */
@CapacitorPlugin(name = "ClockWidget")
public class WidgetBridgePlugin extends Plugin {

    @PluginMethod
    public void setClocks(PluginCall call) {
        JSArray clocks = call.getArray("clocks");
        if (clocks == null) {
            call.reject("clocks is required");
            return;
        }
        getContext()
                .getSharedPreferences(ClockWidgetProvider.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(ClockWidgetProvider.KEY, clocks.toString())
                .apply();
        ClockWidgetProvider.refreshAll(getContext());
        call.resolve();
    }

    @PluginMethod
    public void pin(PluginCall call) {
        JSObject result = new JSObject();
        boolean requested = false;
        if (Build.VERSION.SDK_INT >= 26) {
            Context context = getContext();
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            if (manager.isRequestPinAppWidgetSupported()) {
                ComponentName provider = new ComponentName(context, ClockWidgetProvider.class);
                requested = manager.requestPinAppWidget(provider, null, null);
            }
        }
        result.put("requested", requested);
        call.resolve(result);
    }
}
