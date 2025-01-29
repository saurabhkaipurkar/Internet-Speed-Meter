package com.example.internetspeedmeter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.graphics.Insets;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.internetspeedmeter.Adapter.DataUsageAdapter;
import com.example.internetspeedmeter.datahandler.DataUsageHandler;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String PREF_NAME = "AppPreferences";
    private static final String KEY_PROCESS_STATE = "processState";
    private static final int UPDATE_INTERVAL_MS = 5000;

    private final Handler handler = new Handler();
    private final List<DataUsageHandler> dataUsageList = new ArrayList<>();
    private DataUsageAdapter adapter;

    private final Runnable updateDataTask = new Runnable() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void run() {
            // Update data usage dynamically
            if (dataUsageList.size() >= 2) {
                dataUsageList.get(0).setUsage(formatDataSize(getWiFiDataUsage()));
                dataUsageList.get(1).setUsage(formatDataSize(getMobileDataUsage()));
                adapter.notifyItemRangeChanged(0, 2);
            }

            // Re-run this task after the interval
            handler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Settings.canDrawOverlays(this)) {
                    startFloatingSpeedService();
                } else {
                    Toast.makeText(this, "Overlay permission is required to use this feature.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
          return insets;
        });
        setupRecyclerView();
        checkAndRequestPermissions();
        restoreProcessState();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.dataUsageRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dataUsageList.add(new DataUsageHandler("Wi-Fi Usage : ", formatDataSize(getWiFiDataUsage())));
        dataUsageList.add(new DataUsageHandler("Mobile Data Usage : ", formatDataSize(getMobileDataUsage())));
        adapter = new DataUsageAdapter(dataUsageList);
        recyclerView.setAdapter(adapter);

        startDataUpdates();
    }

    private void checkAndRequestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(intent);
        }
    }

    private void restoreProcessState() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isProcessRunning = preferences.getBoolean(KEY_PROCESS_STATE, true);

        if (isProcessRunning) {
            startFloatingSpeedService();
        }
    }

    private void startDataUpdates() {
        handler.postDelayed(updateDataTask, UPDATE_INTERVAL_MS);
    }

    private long getMobileDataUsage() {
        return TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
    }

    private long getTotalDataUsage() {
        return TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
    }

    private long getWiFiDataUsage() {
        return getTotalDataUsage() - getMobileDataUsage();
    }

    @SuppressLint("DefaultLocale")
    private String formatDataSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }

    private void startFloatingSpeedService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please enable 'Display over other apps' permission in settings.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent serviceIntent = new Intent(this, SpeedService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateDataTask); // Stop updating when the activity is destroyed
    }
}
