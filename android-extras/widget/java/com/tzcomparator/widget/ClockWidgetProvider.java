package com.tzcomparator.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Home-screen widget that shows up to four live clocks. The clock list is written by the web app
 * (through WidgetBridgePlugin) into SharedPreferences. The times themselves are drawn by TextClock
 * views, so they keep ticking without any background work.
 */
public class ClockWidgetProvider extends AppWidgetProvider {
    static final String PREFS = "tzc_widget";
    static final String KEY = "clocks";
    private static final int ROWS = 4;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, build(context));
        }
    }

    static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
        for (int id : ids) {
            manager.updateAppWidget(id, build(context));
        }
    }

    private static int rid(Context context, String type, String name) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    static RemoteViews build(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), rid(context, "layout", "widget_clocks"));

        String[] labels = new String[ROWS];
        String[] zones = new String[ROWS];
        int count = 0;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < arr.length() && count < ROWS; i++) {
                JSONObject o = arr.getJSONObject(i);
                String zone = o.optString("zone", "");
                if (zone.length() == 0) {
                    continue;
                }
                labels[count] = o.optString("label", zone);
                zones[count] = zone;
                count++;
            }
        } catch (Exception e) {
            count = 0;
        }
        if (count == 0) {
            labels[0] = "Manila";
            zones[0] = "Asia/Manila";
            labels[1] = "New York";
            zones[1] = "America/New_York";
            labels[2] = "London";
            zones[2] = "Europe/London";
            count = 3;
        }

        for (int i = 0; i < ROWS; i++) {
            int row = rid(context, "id", "row" + i);
            if (i < count) {
                views.setViewVisibility(row, View.VISIBLE);
                views.setTextViewText(rid(context, "id", "name" + i), labels[i]);
                views.setString(rid(context, "id", "time" + i), "setTimeZone", zones[i]);
                views.setString(rid(context, "id", "day" + i), "setTimeZone", zones[i]);
            } else {
                views.setViewVisibility(row, View.GONE);
            }
        }

        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch != null) {
            PendingIntent pending = PendingIntent.getActivity(
                    context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(rid(context, "id", "widget_root"), pending);
        }
        return views;
    }
}
