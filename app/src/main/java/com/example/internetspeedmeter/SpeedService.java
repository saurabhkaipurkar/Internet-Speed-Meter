package com.example.internetspeedmeter;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class SpeedService extends Service
{
    private static final String CHANNEL_ID = "speed_meter_channel";
    private static final int NOTIFICATION_ID = 1;

    private WindowManager windowManager;
    private View floatingView;
    private TextView speedTextView;
    private final Handler handler = new Handler();

    private long previousRxBytes = 0, previousTxBytes = 0, previousTime = 0;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private String speedtext;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permission to display over other apps is required.", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        registerReceiver(appReceiver, createIntentFilter());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createFloatingOverlay();
    }

    private IntentFilter createIntentFilter() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_LAUNCHER);
        return filter;
    }

    @SuppressLint("InflateParams")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createFloatingOverlay() {
        if (floatingView != null) return; // Prevent multiple floating views

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_speed_view, null);
        speedTextView = floatingView.findViewById(R.id.speedTextView);

        WindowManager.LayoutParams params = createLayoutParams();
        windowManager.addView(floatingView, params);

        setupTouchListener(params);
        updateSpeed();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private WindowManager.LayoutParams createLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 10;
        params.y = 10;
        return params;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener(WindowManager.LayoutParams params) {
        floatingView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int deltaX = (int) (event.getRawX() - initialTouchX);
                    int deltaY = (int) (event.getRawY() - initialTouchY);
                    params.x = clamp(initialX + deltaX, getScreenWidth() - floatingView.getWidth());
                    params.y = clamp(initialY + deltaY, getScreenHeight() - floatingView.getHeight());
                    windowManager.updateViewLayout(floatingView, params);
                    return true;

                default:
                    return false;
            }
        });
    }

    private int getScreenWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    private int getScreenHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    private int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification(speedtext));
        return START_STICKY;
    }

    private Notification createNotification(String speed) {
        createNotificationChannel();
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Internet Speed Meter App")
                .setContentText("Monitoring Speed\n"+ speed)
                .setSmallIcon(R.drawable.network_check)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Speed Meter Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Foreground service for monitoring speed");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void updateSpeed() {
        handler.post(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                long currentRxBytes = TrafficStats.getTotalRxBytes();
                long currentTxBytes = TrafficStats.getTotalTxBytes();
                long currentTime = System.currentTimeMillis();

                long rxSpeed = calculateSpeed(currentRxBytes, previousRxBytes, currentTime, previousTime);
                long txSpeed = calculateSpeed(currentTxBytes, previousTxBytes, currentTime, previousTime);

                previousRxBytes = currentRxBytes;
                previousTxBytes = currentTxBytes;
                previousTime = currentTime;

                speedtext = ("↓ " + formatSpeed(rxSpeed) + " | ↑ " + formatSpeed(txSpeed));
                startForeground(NOTIFICATION_ID,createNotification(speedtext));
                speedTextView.setText(speedtext);
                handler.postDelayed(this, 2000);
            }
        });
    }

    private long calculateSpeed(long currentBytes, long previousBytes, long currentTime, long previousTime) {
        long elapsedTime = Math.max(1, currentTime - previousTime); // Prevent division by zero
        long speed = (currentBytes - previousBytes) * 1000 / elapsedTime;
        return Math.max(0, speed); // Prevent negative values
    }

    public String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond >= 1024 * 1024) {
            return (bytesPerSecond / (1024 * 1024)) + " MB/s";
        } else if (bytesPerSecond >= 1024) {
            return (bytesPerSecond / 1024) + " KB/s";
        } else {
            return bytesPerSecond + " B/s";
        }
    }

    private final BroadcastReceiver appReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (floatingView != null) {
                windowManager.removeView(floatingView);
                floatingView = null;
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(appReceiver);
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
