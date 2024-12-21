package com.example.internetspeedmeter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1234; // Arbitrary request code for permission result

    @SuppressLint("ObsoleteSdkInt")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if the app has permission to draw over other apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Request permission to draw over other apps
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE);
        } else {
            // Permission already granted or not needed
            startFloatingSpeedService();
        }
    }

    // Start the FloatingSpeedService based on Android version
    private void startFloatingSpeedService() {
        // Start FloatingSpeedService using startForegroundService for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, FloatingSpeedService.class));
        } else {
            startService(new Intent(this, FloatingSpeedService.class));
        }
    }

    // Handle the result of the permission request
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, start the service
                startFloatingSpeedService();
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Permission to draw over other apps is required to show the floating speed meter.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
