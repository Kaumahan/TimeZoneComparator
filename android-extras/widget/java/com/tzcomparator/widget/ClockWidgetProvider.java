package com.tzcomparator.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Home-screen clock widget. The first clock is shown large, the others in compact rows. Times are drawn by
 * TextClock views, so they tick on their own. The work-status dot and the sun/moon icon are refreshed every
 * 15 minutes, whenever the widget is resized, and whenever the app changes the clock list.
 */
public class ClockWidgetProvider extends AppWidgetProvider {
    static final String PREFS = "tzc_widget";
    static final String KEY = "clocks";
    static final String KEY_H24 = "h24";

    private static final int MAX_ROWS = 4;
    private static final int SMALL_ROWS = 2;
    private static final int SMALL_LAYOUT_BELOW_DP = 150;
    private static final long REFRESH_MS = 15L * 60L * 1000L;

    private static final int GREEN = 0xFF34D399;
    private static final int ROSE = 0xFFFB7185;
    private static final int GRAY = 0xFF71717A;
    private static final int SUN = 0xFFFBBF24;
    private static final int MOON = 0xFF93C5FD;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, build(context, heightOf(manager, id)));
        }
        scheduleRefresh(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int appWidgetId, Bundle newOptions) {
        int height = newOptions == null ? 0 : newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        manager.updateAppWidget(appWidgetId, build(context, height));
    }

    @Override
    public void onEnabled(Context context) {
        scheduleRefresh(context);
    }

    @Override
    public void onDisabled(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.cancel(refreshIntent(context));
            }
        } catch (Exception ignored) {
            // nothing to cancel
        }
    }

    static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
        for (int id : ids) {
            manager.updateAppWidget(id, build(context, heightOf(manager, id)));
        }
    }

    private static int heightOf(AppWidgetManager manager, int id) {
        Bundle options = manager.getAppWidgetOptions(id);
        return options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
    }

    private static PendingIntent refreshIntent(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
        Intent intent = new Intent(context, ClockWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        return PendingIntent.getBroadcast(
                context, 4242, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Inexact, battery-friendly refresh at the next quarter hour. No special permission needed. */
    static void scheduleRefresh(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                return;
            }
            long now = System.currentTimeMillis();
            long next = ((now / REFRESH_MS) + 1) * REFRESH_MS + 1000L;
            am.set(AlarmManager.RTC, next, refreshIntent(context));
        } catch (Exception ignored) {
            // the widget still works, the status dot just refreshes less often
        }
    }

    private static int rid(Context context, String type, String name) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    /** How many clock rows fit in the given widget height (0 = unknown, use all). */
    private static int rowsFor(int heightDp) {
        if (heightDp <= 0) {
            return MAX_ROWS;
        }
        if (heightDp < SMALL_LAYOUT_BELOW_DP) {
            return SMALL_ROWS;
        }
        if (heightDp < 190) {
            return 2;
        }
        if (heightDp < 235) {
            return 3;
        }
        return MAX_ROWS;
    }

    static RemoteViews build(Context context, int heightDp) {
        boolean small = heightDp > 0 && heightDp < SMALL_LAYOUT_BELOW_DP;
        int rows = rowsFor(heightDp);
        RemoteViews views = new RemoteViews(
                context.getPackageName(), rid(context, "layout", small ? "widget_clocks_small" : "widget_clocks"));

        String[] labels = new String[MAX_ROWS];
        String[] zones = new String[MAX_ROWS];
        int[] starts = new int[MAX_ROWS];
        int[] ends = new int[MAX_ROWS];
        String[] days = new String[MAX_ROWS];
        int count = 0;
        boolean h24 = false;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            h24 = prefs.getBoolean(KEY_H24, false);
            JSONArray arr = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < arr.length() && count < MAX_ROWS; i++) {
                JSONObject o = arr.getJSONObject(i);
                String zone = o.optString("zone", "");
                if (zone.length() == 0) {
                    continue;
                }
                labels[count] = o.optString("label", zone);
                zones[count] = zone;
                starts[count] = (int) o.optLong("ws", 9L);
                ends[count] = (int) o.optLong("we", 18L);
                days[count] = o.optString("wd", "12345");
                count++;
            }
        } catch (Exception e) {
            count = 0;
        }
        if (count == 0) {
            String[] defLabels = {"Manila", "New York", "London"};
            String[] defZones = {"Asia/Manila", "America/New_York", "Europe/London"};
            for (int i = 0; i < 3; i++) {
                labels[i] = defLabels[i];
                zones[i] = defZones[i];
                starts[i] = 9;
                ends[i] = 18;
                days[i] = "12345";
            }
            count = 3;
        }

        for (int i = 0; i < rows; i++) {
            int row = rid(context, "id", "row" + i);
            if (i < count) {
                views.setViewVisibility(row, View.VISIBLE);
                bindRow(context, views, i, labels[i], zones[i], starts[i], ends[i], days[i], h24);
            } else {
                views.setViewVisibility(row, View.GONE);
            }
        }
        if (!small) {
            views.setViewVisibility(rid(context, "divider"), count > 1 && rows > 1 ? View.VISIBLE : View.GONE);
        }

        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch != null) {
            PendingIntent pending = PendingIntent.getActivity(
                    context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(rid(context, "id", "widget_root"), pending);
        }
        return views;
    }

    private static int rid(Context context, String name) {
        return rid(context, "id", name);
    }

    private static void bindRow(Context c, RemoteViews v, int i, String label, String zone,
                                int workStart, int workEnd, String workDays, boolean h24) {
        v.setTextViewText(rid(c, "name" + i), label);

        String timeFormat = h24 ? "HH:mm" : "h:mm";
        int time = rid(c, "time" + i);
        v.setString(time, "setTimeZone", zone);
        v.setCharSequence(time, "setFormat12Hour", timeFormat);
        v.setCharSequence(time, "setFormat24Hour", timeFormat);

        int ap = rid(c, "ap" + i);
        v.setString(ap, "setTimeZone", zone);
        v.setCharSequence(ap, "setFormat12Hour", "a");
        v.setCharSequence(ap, "setFormat24Hour", "a");
        v.setViewVisibility(ap, h24 ? View.GONE : View.VISIBLE);

        int day = rid(c, "day" + i);
        v.setString(day, "setTimeZone", zone);
        v.setCharSequence(day, "setFormat12Hour", "EEE, MMM d");
        v.setCharSequence(day, "setFormat24Hour", "EEE, MMM d");

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zone));
        int dow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday, same as the web app
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minutes = hour * 60 + cal.get(Calendar.MINUTE);
        boolean workDay = workDays.indexOf((char) ('0' + dow)) >= 0;
        boolean working = workDay && minutes >= workStart * 60 && minutes < workEnd * 60;

        int color;
        String text;
        if (!workDay) {
            color = GRAY;
            text = "Day off";
        } else if (working) {
            color = GREEN;
            text = "Working";
        } else {
            color = ROSE;
            text = "Off hours";
        }
        v.setTextViewText(rid(c, "st" + i), text);
        v.setTextColor(rid(c, "st" + i), color);
        v.setTextColor(rid(c, "dot" + i), color);

        boolean night = hour < 6 || hour >= 18;
        v.setTextViewText(rid(c, "ico" + i), night ? "\u263E" : "\u2600");
        v.setTextColor(rid(c, "ico" + i), night ? MOON : SUN);
    }
}
