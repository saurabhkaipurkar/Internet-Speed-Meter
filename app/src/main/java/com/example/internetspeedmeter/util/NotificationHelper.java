package com.example.internetspeedmeter.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.internetspeedmeter.R;
public class NotificationHelper
{
    private static final String CHANNEL_ID = "speed_meter_channel";

    public static Notification createNotification(Context context, String speed) {
        createNotificationChannel(context);
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Internet Speed Meter App")
                .setContentText("Monitoring Speed \n" + speed)
                .setSmallIcon(R.drawable.network_check)
                .setOnlyAlertOnce(true)
                .build();
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Speed Meter Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Foreground service for monitoring speed");

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
