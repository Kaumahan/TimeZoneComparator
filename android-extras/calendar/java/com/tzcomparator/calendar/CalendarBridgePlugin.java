package com.tzcomparator.calendar;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

/**
 * Reads upcoming events from the calendars on the phone (this includes Google Calendar when the Google
 * account is added to the phone) and opens the calendar app to add events. Nothing leaves the device.
 */
@CapacitorPlugin(
        name = "CalendarBridge",
        permissions = {@Permission(alias = "calendar", strings = {Manifest.permission.READ_CALENDAR})})
public class CalendarBridgePlugin extends Plugin {

    @PluginMethod
    public void listEvents(PluginCall call) {
        if (getPermissionState("calendar") != PermissionState.GRANTED) {
            requestPermissionForAlias("calendar", call, "calendarPermissionResult");
            return;
        }
        readEvents(call);
    }

    @PermissionCallback
    private void calendarPermissionResult(PluginCall call) {
        if (getPermissionState("calendar") == PermissionState.GRANTED) {
            readEvents(call);
        } else {
            call.reject("Calendar permission denied", "DENIED");
        }
    }

    private void readEvents(PluginCall call) {
        Integer days = call.getInt("days", 7);
        long start = System.currentTimeMillis();
        long end = start + days.intValue() * 24L * 3600L * 1000L;

        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);

        String[] projection = new String[] {
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_TIMEZONE,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        };

        JSArray events = new JSArray();
        Cursor cursor = null;
        try {
            ContentResolver resolver = getContext().getContentResolver();
            cursor = resolver.query(
                    builder.build(),
                    projection,
                    CalendarContract.Instances.VISIBLE + " = 1",
                    null,
                    CalendarContract.Instances.BEGIN + " ASC");
            while (cursor != null && cursor.moveToNext() && events.length() < 60) {
                JSObject e = new JSObject();
                e.put("id", cursor.getLong(0));
                e.put("begin", cursor.getLong(1));
                e.put("end", cursor.getLong(2));
                e.put("title", cursor.getString(3));
                e.put("location", cursor.getString(4));
                e.put("allDay", cursor.getInt(5) == 1);
                e.put("timezone", cursor.getString(6));
                e.put("calendar", cursor.getString(7));
                events.put(e);
            }
        } catch (Exception ex) {
            call.reject("Could not read calendar: " + ex.getMessage());
            return;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        JSObject result = new JSObject();
        result.put("events", events);
        call.resolve(result);
    }

    /** Opens the calendar app with a new event filled in. No permission needed. */
    @PluginMethod
    public void insertEvent(PluginCall call) {
        try {
            String title = call.getString("title", "Meeting");
            // getDouble exists in every Capacitor version; epoch milliseconds fit exactly in a double
            Double beginD = call.getDouble("begin", Double.valueOf((double) System.currentTimeMillis()));
            long begin = beginD.longValue();
            Double endD = call.getDouble("end", Double.valueOf((double) (begin + 3600000L)));
            long end = endD.longValue();
            String timezone = call.getString("timezone", "");

            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(CalendarContract.Events.CONTENT_URI);
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin);
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end);
            intent.putExtra(CalendarContract.Events.TITLE, title);
            if (timezone != null && timezone.length() > 0) {
                intent.putExtra(CalendarContract.Events.EVENT_TIMEZONE, timezone);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("No calendar app found: " + e.getMessage());
        }
    }
}
