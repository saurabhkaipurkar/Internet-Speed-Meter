package com.example.internetspeedmeter.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Handler;
import android.widget.RemoteViews;

import com.example.internetspeedmeter.SpeedService;
import com.example.internetspeedmeter.MainActivity;
import com.example.internetspeedmeter.R;

public class InternetWidgetProvider extends AppWidgetProvider
{
    private static final Handler handler = new Handler();
    private static long previousRxBytes = TrafficStats.getTotalRxBytes();
    private static long previousTxBytes = TrafficStats.getTotalTxBytes();
    private static long previousTime = System.currentTimeMillis();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update all widgets
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        // Start updating speed dynamically
        startUpdatingSpeed(context, appWidgetManager);
    }

    private void startUpdatingSpeed(Context context, AppWidgetManager appWidgetManager) {
        Runnable updateSpeedRunnable = new Runnable() {
            @Override
            public void run() {
                long currentRxBytes = TrafficStats.getTotalRxBytes();
                long currentTxBytes = TrafficStats.getTotalTxBytes();
                long currentTime = System.currentTimeMillis();

                long rxSpeed = (currentRxBytes - previousRxBytes) * 1000 / (currentTime - previousTime);
                long txSpeed = (currentTxBytes - previousTxBytes) * 1000 / (currentTime - previousTime);

                previousRxBytes = currentRxBytes;
                previousTxBytes = currentTxBytes;
                previousTime = currentTime;

                // Update the widget UI
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_internet_speed);
                SpeedService service = new SpeedService();
                String speedText = "↓ " + service.formatSpeed(rxSpeed) + " | ↑ " + service.formatSpeed(txSpeed);
                views.setTextViewText(R.id.widget_speed_text,speedText);


                ComponentName widget = new ComponentName(context, InternetWidgetProvider.class);
                appWidgetManager.updateAppWidget(widget, views);

                // Schedule the next update
                handler.postDelayed(this, 1000);
            }
        };

        handler.post(updateSpeedRunnable);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // Stop updates when the widget is deleted
        handler.removeCallbacksAndMessages(null);
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        // Stop updates when the last widget is disabled
        handler.removeCallbacksAndMessages(null);
        super.onDisabled(context);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_internet_speed);

        // Set up a click listener to launch the main app when the widget is clicked
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_speed_text, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
