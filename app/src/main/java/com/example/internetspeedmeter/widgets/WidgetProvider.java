package com.example.internetspeedmeter.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.widget.RemoteViews;
import com.example.internetspeedmeter.SpeedService;
import com.example.internetspeedmeter.MainActivity;
import com.example.internetspeedmeter.R;
import java.text.DecimalFormat;

public class WidgetProvider extends AppWidgetProvider
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

    private void startUpdatingSpeed(Context context, AppWidgetManager appWidgetManager)
    {
        Runnable updateSpeedRunnable = new Runnable() {
            @Override
            public void run() {
                String networkType = getNetworkType(context);

                long currentRxBytes;
                long currentTxBytes;

                if ("Mobile Data".equals(networkType)) {
                    currentRxBytes = TrafficStats.getMobileRxBytes();
                    currentTxBytes = TrafficStats.getMobileTxBytes();
                } else if ("Wi-Fi".equals(networkType)) {
                    currentRxBytes = TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes();
                    currentTxBytes = TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes();
                } else {
                    currentRxBytes = 0;
                    currentTxBytes = 0;
                }

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

                if (rxSpeed < 5000) {
                    views.setTextColor(R.id.widget_speed_text, 0xFFFF0000); // Red for slow speed
                } else {
                    views.setTextColor(R.id.widget_speed_text, 0xFF00FF00); // Green for normal speed
                }

                views.setTextViewText(R.id.widget_network_type, networkType);

                String dataUsage = "Data: " + formatDataUsage(currentRxBytes + currentTxBytes);
                views.setTextViewText(R.id.widget_data_usage, dataUsage);

                ComponentName widget = new ComponentName(context, WidgetProvider.class);
                appWidgetManager.updateAppWidget(widget, views);

                // Schedule the next update
                handler.postDelayed(this, 1000);
            }
        };

        handler.post(updateSpeedRunnable);
    }

    private String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return "Wi-Fi";
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return "Mobile Data";
            }
        }
        return "No Connection";
    }

    private String formatDataUsage(long bytes) {
        DecimalFormat df = new DecimalFormat("0.00");
        double kb = bytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;
        if (gb > 1) return df.format(gb) + " GB";
        if (mb > 1) return df.format(mb) + " MB";
        return df.format(kb) + " KB";
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
