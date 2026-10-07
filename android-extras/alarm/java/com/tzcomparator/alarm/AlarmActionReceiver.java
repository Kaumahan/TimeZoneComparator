package com.tzcomparator.alarm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Handles the "Snooze 5 min" and "Dismiss" buttons on the ringing alarm notification. */
public class AlarmActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            int id = intent.getIntExtra("id", 0);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(id); // also stops the repeating alarm sound
            }
            if (AlarmEngine.ACTION_SNOOZE.equals(intent.getAction())) {
                String title = intent.getStringExtra("title");
                String body = intent.getStringExtra("body");
                String sound = intent.getStringExtra("sound");
                long at = System.currentTimeMillis() + AlarmEngine.SNOOZE_MS;
                AlarmEngine.setOne(
                        context,
                        AlarmEngine.snoozeId(id),
                        at,
                        title == null ? "Alarm" : title,
                        "Snoozed: " + (body == null ? "" : body),
                        AlarmEngine.cleanSound(sound));
            }
        } catch (Exception ignored) {
            // never crash from a broadcast
        }
    }
}
