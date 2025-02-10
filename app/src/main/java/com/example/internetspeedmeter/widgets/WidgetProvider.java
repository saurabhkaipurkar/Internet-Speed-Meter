package com.example.internetspeedmeter.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.os.Handler;
import android.widget.RemoteViews;
import com.example.internetspeedmeter.MainActivity;
import com.example.internetspeedmeter.R;
import com.example.internetspeedmeter.util.SpeedCalculator;

import java.text.DecimalFormat;

public class WidgetProvider extends AppWidgetProvider {
    private static final Handler handler = new Handler();
    private static long previousRxBytes;
    private static long previousTxBytes;
    private static long previousTime;
    private static final String PREF_NAME = "WidgetPrefs";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        loadTrafficData(context);

        // Update all widgets
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        // Start updating speed dynamically
        startUpdatingSpeed(context, appWidgetManager);
    }

    private static String getNetworkType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return "No Connection";

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return "No Connection";

        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (networkCapabilities == null) return "No Connection";

        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return "Wi-Fi";
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return "Mobile Data";
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return "Ethernet";
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
            return "Bluetooth";
        } else {
            // Fallback for older Android versions
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) return "Wi-Fi";
                if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) return "Mobile Data";
            }
        }
        return "Unknown Network";
    }

    private void startUpdatingSpeed(Context context, AppWidgetManager appWidgetManager) {
        handler.removeCallbacksAndMessages(null); // Clear previous handler callbacks

        Runnable updateSpeedRunnable = new Runnable() {
            @Override
            public void run() {
                long currentRxBytes = SpeedCalculator.getTotalRxBytes();
                long currentTxBytes = SpeedCalculator.getTotalTxBytes();
                long currentTime = System.currentTimeMillis();

                long rxSpeed = SpeedCalculator.calculateSpeed(currentRxBytes, previousRxBytes, currentTime, previousTime);
                long txSpeed = SpeedCalculator.calculateSpeed(currentTxBytes, previousTxBytes, currentTime, previousTime);

                previousRxBytes = currentRxBytes;
                previousTxBytes = currentTxBytes;
                previousTime = currentTime;

                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_internet_speed);
                String speedText = " ↓ " + SpeedCalculator.formatSpeed(rxSpeed) + " | ↑ " + SpeedCalculator.formatSpeed(txSpeed);
                views.setTextViewText(R.id.widget_speed_text, speedText);

                String dataUsage = "Data: " + formatDataUsage(currentRxBytes + currentTxBytes);
                views.setTextViewText(R.id.widget_data_usage, dataUsage);

                ComponentName widget = new ComponentName(context, WidgetProvider.class);
                appWidgetManager.updateAppWidget(widget, views);
                saveTrafficData(context, currentRxBytes, currentTxBytes, currentTime);

                String networkType = getNetworkType(context);
                views.setTextViewText(R.id.widget_network_type, "Network: " + networkType);

                // Schedule the next update
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(updateSpeedRunnable);
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

    private void saveTrafficData(Context context, long rx, long tx, long time) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong("rxBytes", rx)
                .putLong("txBytes", tx)
                .putLong("previousTime", time)
                .apply();
    }

    private void loadTrafficData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        previousRxBytes = prefs.getLong("rxBytes", TrafficStats.getTotalRxBytes());
        previousTxBytes = prefs.getLong("txBytes", TrafficStats.getTotalTxBytes());
        previousTime = prefs.getLong("previousTime", System.currentTimeMillis());
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        handler.removeCallbacksAndMessages(null);
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        handler.removeCallbacksAndMessages(null);
        super.onDisabled(context);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_internet_speed);
        String networkType = getNetworkType(context);
        views.setTextViewText(R.id.widget_network_type, "Network: " + networkType);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root_layout, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
