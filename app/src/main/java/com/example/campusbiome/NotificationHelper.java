package com.example.campusbiome;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    public static final String CHANNEL_APPOINTMENTS = "appointment_updates";
    public static final String CHANNEL_REMINDERS = "class_reminders";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            if (manager != null) {
                NotificationChannel apptChannel = new NotificationChannel(
                        CHANNEL_APPOINTMENTS,
                        "Appointment Updates",
                        NotificationManager.IMPORTANCE_HIGH
                );
                apptChannel.setDescription("Notifications when your appointments are approved or rejected.");

                NotificationChannel remChannel = new NotificationChannel(
                        CHANNEL_REMINDERS,
                        "Class & Meeting Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                );
                remChannel.setDescription("Reminders 30 minutes before classes or appointments.");

                manager.createNotificationChannel(apptChannel);
                manager.createNotificationChannel(remChannel);
            }
        }
    }

    public static void sendNotification(Context context, String channelId, int notificationId, String title, String message) {
        Intent intent = new Intent(context, StudentDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_account_circle) // using existing icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }
}
