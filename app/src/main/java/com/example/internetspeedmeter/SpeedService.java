package com.example.internetspeedmeter;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.example.internetspeedmeter.util.NotificationHelper;
import com.example.internetspeedmeter.util.SpeedCalculator;
import com.example.internetspeedmeter.util.WindowManagerHelper;

public class SpeedService extends Service
{
    private static final int NOTIFICATION_ID = 1;
    private WindowManager windowManager;
    private View floatingView;
    private TextView speedTextView;
    private final Handler handler = new Handler();
    private long previousRxBytes = 0, previousTxBytes = 0, previousTime = 0;
    private String speedtext;

    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "InflateParams"})
    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permission to display over other apps is required.", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_speed_view, null);
        speedTextView = floatingView.findViewById(R.id.speedTextView);

        if (WindowManagerHelper.canCreateOverlay()) {
            WindowManagerHelper.addFloatingView(floatingView, windowManager);
            WindowManagerHelper.setupTouchListener(floatingView, windowManager);
            updateSpeed();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, NotificationHelper.createNotification(this, speedtext));
        return START_STICKY;
    }

    private void updateSpeed() {
        handler.post(new Runnable() {
            @SuppressLint("SetTextI18n")
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

                speedtext = ("↓ " + SpeedCalculator.formatSpeed(rxSpeed) + " | ↑ " + SpeedCalculator.formatSpeed(txSpeed));
                startForeground(NOTIFICATION_ID, NotificationHelper.createNotification(SpeedService.this, speedtext));
                speedTextView.setText(speedtext);
                handler.postDelayed(this, 2000);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
        handler.removeCallbacksAndMessages(null);
        stopForeground(true);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}