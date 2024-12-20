package com.example.internetspeedmeter;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class FloatingSpeedService extends Service
{
    private WindowManager windowManager;
    private View floatingView;
    private TextView speedTextView;

    private long previousRxBytes = 0;
    private long previousTxBytes = 0;
    private long previousTime = 0;

    private final Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize TrafficStats values
        previousRxBytes = TrafficStats.getTotalRxBytes();
        previousTxBytes = TrafficStats.getTotalTxBytes();
        previousTime = System.currentTimeMillis();

        // Create the floating overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createFloatingOverlay();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createFloatingOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate the layout for the overlay
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_speed_view, null);
        speedTextView = floatingView.findViewById(R.id.speedTextView);

        // Set layout parameters for the overlay
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // For overlay permission
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        // Position the overlay near the status bar
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 10; // Distance from right edge
        params.y = 10; // Distance from top

        // Add the floating view to the window
        windowManager.addView(floatingView, params);

        // Start updating internet speed
        updateSpeed();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
        handler.removeCallbacksAndMessages(null);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
