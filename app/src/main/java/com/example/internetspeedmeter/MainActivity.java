package com.example.internetspeedmeter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class MainActivity extends AppCompatActivity {
    SwitchCompat toggleSwitch;
    private static final String PREF_NAME = "AppPreferences";
    private static final String KEY_PROCESS_STATE = "processState";

    // Use the new ActivityResultLauncher
    @SuppressLint("ObsoleteSdkInt")
    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    startFloatingSpeedService();
                } else {
                    Toast.makeText(this, "Overlay permission is required to use this feature.", Toast.LENGTH_SHORT).show();
                }
            });

    @SuppressLint("ObsoleteSdkInt")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleSwitch = findViewById(R.id.toggleSwitch);
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isProcessRunning = preferences.getBoolean(KEY_PROCESS_STATE, true);

        toggleSwitch.setChecked(isProcessRunning);
        if (isProcessRunning) {
            startFloatingSpeedService();
        }

        toggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startFloatingSpeedService();
                Toast.makeText(MainActivity.this, "Processes Started", Toast.LENGTH_SHORT).show();
            } else {
                stopFloatingService();
                Toast.makeText(MainActivity.this, "Processes Stopped", Toast.LENGTH_SHORT).show();
            }
        });

        // Check if the app has permission to draw over other apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Request permission using ActivityResultLauncher
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(intent);
        } else {
            startFloatingSpeedService();
        }
    }

    private void stopFloatingService() {
        // Stop the FloatingSpeedService
        stopService(new Intent(this, FloatingSpeedService.class));
    }

    private void startFloatingSpeedService() {
        // Start FloatingSpeedService using startForegroundService for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, FloatingSpeedService.class));
        } else {
            startService(new Intent(this, FloatingSpeedService.class));
        }
    }
}
