package com.example.campusbiome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_NOTIF_ID = "extra_notif_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        int notifId = intent.getIntExtra(EXTRA_NOTIF_ID, (int) System.currentTimeMillis());

        if (title == null) title = "Upcoming Event";
        if (message == null) message = "You have an upcoming event in 30 minutes.";

        NotificationHelper.sendNotification(context, NotificationHelper.CHANNEL_REMINDERS, notifId, title, message);
    }
}
