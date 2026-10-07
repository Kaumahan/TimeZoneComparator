package com.tzcomparator.alarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Schedules alarms with AlarmManager.setAlarmClock (the same mechanism the Clock app uses). These alarms
 * fire on time even in Doze mode and need no exact-alarm permission. When one fires, AlarmReceiver shows
 * a loud, repeating alarm notification on the ALARM audio stream.
 */
public final class AlarmEngine {
    static final String PREFS = "tzc_alarm_engine";
    static final String KEY = "alarms";
    static final int MAX_ALARMS = 300;
    static final String ACTION_SNOOZE = "com.tzcomparator.ALARM_SNOOZE";
    static final String ACTION_DISMISS = "com.tzcomparator.ALARM_DISMISS";
    static final long SNOOZE_MS = 5L * 60L * 1000L;

    /** Id used for the one-off alarm created by "Snooze". Stays clear of the ids the web app generates. */
    static int snoozeId(int id) {
        return 1000000000 + (id & 0xFFFFF);
    }

    static PendingIntent actionPending(Context ctx, int id, String action, String title, String body, String sound) {
        Intent intent = new Intent(ctx, AlarmActionReceiver.class);
        intent.setAction(action);
        intent.putExtra("id", id);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        intent.putExtra("sound", sound);
        return PendingIntent.getBroadcast(
                ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private AlarmEngine() {
    }

    static String cleanSound(String sound) {
        if (sound == null) {
            return "classic";
        }
        String s = sound.replaceAll("[^a-z0-9_]", "");
        return s.length() == 0 ? "classic" : s;
    }

    static String channelId(String sound) {
        return "ring_" + cleanSound(sound);
    }

    private static AlarmManager alarmManager(Context ctx) {
        return (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    }

    static PendingIntent firePending(Context ctx, int id, String title, String body, String sound) {
        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.setAction("com.tzcomparator.ALARM_FIRE");
        intent.putExtra("id", id);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        intent.putExtra("sound", sound);
        return PendingIntent.getBroadcast(
                ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent launchPending(Context ctx) {
        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launch == null) {
            return null;
        }
        return PendingIntent.getActivity(
                ctx, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void setOne(Context ctx, int id, long at, String title, String body, String sound) {
        AlarmManager am = alarmManager(ctx);
        PendingIntent fire = firePending(ctx, id, title, body, sound);
        PendingIntent show = launchPending(ctx);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show != null ? show : fire), fire);
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, at, fire);
        }
    }

    static void cancelStored(Context ctx) {
        AlarmManager am = alarmManager(ctx);
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray old = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < old.length(); i++) {
                int id = old.getJSONObject(i).getInt("id");
                am.cancel(firePending(ctx, id, "", "", "classic"));
            }
        } catch (Exception ignored) {
            // nothing stored or unreadable: nothing to cancel
        }
    }

    /** Replaces every previously scheduled alarm with the given list. Returns how many were scheduled. */
    static int scheduleAll(Context ctx, JSONArray list) {
        cancelStored(ctx);
        long now = System.currentTimeMillis();
        JSONArray kept = new JSONArray();
        int count = 0;
        if (list != null) {
            for (int i = 0; i < list.length() && count < MAX_ALARMS; i++) {
                try {
                    JSONObject o = list.getJSONObject(i);
                    long at = o.getLong("at");
                    if (at <= now + 1000) {
                        continue;
                    }
                    int id = o.getInt("id");
                    String sound = cleanSound(o.optString("sound", "classic"));
                    ensureChannel(ctx, sound);
                    setOne(ctx, id, at, o.optString("title", "Alarm"), o.optString("body", ""), sound);
                    kept.put(o);
                    count++;
                } catch (Exception ignored) {
                    // skip a malformed entry, keep going
                }
            }
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, kept.toString()).apply();
        return count;
    }

    /** Re-creates alarms after a reboot or app update. */
    static void restore(Context ctx) {
        try {
            JSONArray stored = new JSONArray(
                    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"));
            scheduleAll(ctx, stored);
        } catch (Exception ignored) {
            // nothing to restore
        }
    }

    static int pendingCount(Context ctx) {
        try {
            JSONArray stored = new JSONArray(
                    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"));
            long now = System.currentTimeMillis();
            int n = 0;
            for (int i = 0; i < stored.length(); i++) {
                if (stored.getJSONObject(i).getLong("at") > now) {
                    n++;
                }
            }
            return n;
        } catch (Exception e) {
            return 0;
        }
    }

    static long nextAt(Context ctx) {
        long best = 0;
        try {
            JSONArray stored = new JSONArray(
                    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"));
            long now = System.currentTimeMillis();
            for (int i = 0; i < stored.length(); i++) {
                long at = stored.getJSONObject(i).getLong("at");
                if (at > now && (best == 0 || at < best)) {
                    best = at;
                }
            }
        } catch (Exception ignored) {
            // no alarms stored
        }
        return best;
    }

    static Uri soundUri(Context ctx, String sound) {
        int res = ctx.getResources().getIdentifier("tone_" + sound, "raw", ctx.getPackageName());
        if (res != 0) {
            return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + ctx.getPackageName() + "/" + res);
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    }

    static void ensureChannel(Context ctx, String sound) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        String id = channelId(sound);
        if (nm.getNotificationChannel(id) != null) {
            return;
        }
        NotificationChannel channel =
                new NotificationChannel(id, "Alarm ring: " + sound, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Loud meeting alarm");
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        channel.setSound(soundUri(ctx, sound), attrs);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[] {0, 600, 300, 600, 300, 600});
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);
    }

    /** Shows the ringing alarm. The sound repeats until the notification is dismissed (or 2 minutes pass). */
    static void show(Context ctx, int id, String title, String body, String sound) {
        String snd = cleanSound(sound);
        ensureChannel(ctx, snd);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(ctx, channelId(snd))
                : new Notification.Builder(ctx);
        int small = ctx.getResources().getIdentifier("ic_stat_alarm", "drawable", ctx.getPackageName());
        if (small == 0) {
            small = ctx.getApplicationInfo().icon;
        }
        builder.setSmallIcon(small)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis());
        PendingIntent content = launchPending(ctx);
        if (content != null) {
            builder.setContentIntent(content);
        }
        builder.addAction(small, "Snooze 5 min", actionPending(ctx, id, ACTION_SNOOZE, title, body, snd));
        builder.addAction(small, "Dismiss", actionPending(ctx, id, ACTION_DISMISS, title, body, snd));
        if (Build.VERSION.SDK_INT >= 26) {
            builder.setTimeoutAfter(120000);
        } else {
            builder.setPriority(Notification.PRIORITY_MAX);
            builder.setSound(soundUri(ctx, snd), AudioManager.STREAM_ALARM);
            builder.setVibrate(new long[] {0, 600, 300, 600, 300, 600});
        }
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_INSISTENT;
        nm.notify(id, notification);
    }
}
