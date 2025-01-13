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
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class FloatingSpeedService extends Service {
    private static final String CHANNEL_ID = "speed_meter_channel";
    private static final int NOTIFICATION_ID = 1;

    private WindowManager windowManager;
    private View floatingView;
    private TextView speedTextView;

    private long previousRxBytes = 0;
    private long previousTxBytes = 0;
    private long previousTime = 0;

    private final Handler handler = new Handler();

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_LAUNCHER);
        registerReceiver(appReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        previousRxBytes = TrafficStats.getTotalRxBytes();
        previousTxBytes = TrafficStats.getTotalTxBytes();
        previousTime = System.currentTimeMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createFloatingOverlay();
        }
    }

    @SuppressLint("InflateParams")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createFloatingOverlay() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_speed_view, null);
        speedTextView = floatingView.findViewById(R.id.speedTextView);

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

        windowManager.addView(floatingView, params);

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();

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

                        params.x = initialX + deltaX;
                        params.y = initialY + deltaY;

                        // Get display width and height using DisplayMetrics
                        DisplayMetrics metrics = new DisplayMetrics();
                        windowManager.getDefaultDisplay().getMetrics(metrics);
                        int screenWidth = metrics.widthPixels;
                        int screenHeight = metrics.heightPixels;

                        floatingView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                        int viewWidth = floatingView.getMeasuredWidth();
                        int viewHeight = floatingView.getMeasuredHeight();

                        // Ensure the view stays within screen boundaries
                        if (params.x < 0) {
                            params.x = 0;
                        } else if (params.x + viewWidth > screenWidth) {
                            params.x = screenWidth - viewWidth;
                        }

                        if (params.y < 0) {
                            params.y = 0;
                        } else if (params.y + viewHeight > screenHeight) {
                            params.y = screenHeight - viewHeight;
                        }

                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    default:
                        return false;
                }
            }
        });

        updateSpeed();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Speed Meter Service";
            String description = "Foreground service for monitoring speed";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Internet Speed Meter")
                .setContentText("Monitoring internet speed...")
                .setSmallIcon(R.drawable.network_check)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    private void updateSpeed() {
        Runnable speedUpdateRunnable = new Runnable() {
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

                String speedText = "↓ " + formatSpeed(rxSpeed) + " | ↑ " + formatSpeed(txSpeed);
                speedTextView.setText(speedText);

                handler.postDelayed(this, 1000);
            }
        };

        handler.post(speedUpdateRunnable);
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
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        if (floatingView != null) windowManager.removeView(floatingView);
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(appReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
