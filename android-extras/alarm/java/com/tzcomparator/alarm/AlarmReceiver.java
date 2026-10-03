package com.tzcomparator.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Called by Android at the moment an alarm is due. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            int id = intent.getIntExtra("id", 0);
            String title = intent.getStringExtra("title");
            String body = intent.getStringExtra("body");
            String sound = intent.getStringExtra("sound");
            AlarmEngine.show(context, id, title == null ? "Alarm" : title, body == null ? "" : body, sound);
        } catch (Exception ignored) {
            // never crash from a broadcast
        }
    }
}
