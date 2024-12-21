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
    // Define the channel ID constant
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
        // Initialize TrafficStats values
        previousRxBytes = TrafficStats.getTotalRxBytes();
        previousTxBytes = TrafficStats.getTotalTxBytes();
        previousTime = System.currentTimeMillis();

        // Create the floating overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createFloatingOverlay();
        }
    }

    @SuppressLint("InflateParams")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createFloatingOverlay() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // Inflate the layout for the overlay
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_speed_view, null);
        speedTextView = floatingView.findViewById(R.id.speedTextView);

        // Set layout parameters for the overlay
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // For overlay permission
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // Prevents the view from interacting with other elements
                PixelFormat.TRANSLUCENT
        );

        // Position the overlay near the status bar
        params.gravity = Gravity.TOP | Gravity.START; // Gravity.TOP will place it at the top of the screen
        params.x = 10; // Initial horizontal position
        params.y = 10; // Initial vertical position

        // Add the floating view to the window
        windowManager.addView(floatingView, params);

        // Set touch listener for dragging the floating window
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Save the initial touch position and window layout parameters
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Calculate the change in touch position (delta)
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);

                        // Update the position of the floating window
                        params.x = initialX + deltaX;
                        params.y = initialY + deltaY;

                        // Constrain the floating window within the screen boundaries (for dragging)
                        if (params.x < 0) {
                            params.x = 0;
                        } else if (params.x + floatingView.getWidth() > windowManager.getDefaultDisplay().getWidth()) {
                            params.x = windowManager.getDefaultDisplay().getWidth() - floatingView.getWidth();
                        }

                        if (params.y < 0) {
                            params.y = 0;
                        } else if (params.y + floatingView.getHeight() > windowManager.getDefaultDisplay().getHeight()) {
                            params.y = windowManager.getDefaultDisplay().getHeight() - floatingView.getHeight();
                        }

                        // Update the window position
                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    default:
                        return false;
                }
            }
        });

        // Start updating internet speed
        updateSpeed();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Call startForeground immediately after starting the service
        startForeground(NOTIFICATION_ID, createNotification());
        return super.onStartCommand(intent, flags, startId);
    }

    private Notification createNotification() {
        // Create NotificationChannel for devices running Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Speed Meter Service";
            String description = "Foreground service for monitoring speed";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Create your notification for the foreground service
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Internet Speed Meter")
                .setContentText("Monitoring speed")
                .setSmallIcon(R.drawable.network_check)  // Make sure this icon is available in your resources
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        return builder.build();
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

                // Schedule the next update
                handler.postDelayed(this, 1000);
            }
        };

        handler.post(speedUpdateRunnable);
    }

    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond >= 1024 * 1024) {
            return (bytesPerSecond / (1024 * 1024)) + " MB/s";
        } else if (bytesPerSecond >= 1024) {
            return (bytesPerSecond / 1024) + " KB/s";
        } else {
            return bytesPerSecond + " B/s";
        }
    }

    // Create a receiver to handle the app launch
    private final BroadcastReceiver appReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Remove the floating window when the app is opened
            if (floatingView != null) {
                windowManager.removeView(floatingView);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);  // Removes the service from the foreground
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
