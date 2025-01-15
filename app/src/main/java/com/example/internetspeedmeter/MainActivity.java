package com.example.internetspeedmeter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.internetspeedmeter.Adapter.DataUsageAdapter;
import com.example.internetspeedmeter.datahandler.DataUsageHandler;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private final Handler handler = new Handler();
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

        RecyclerView recyclerView = findViewById(R.id.dataUsageRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<DataUsageHandler> dataUsageList = new ArrayList<>();
        dataUsageList.add(new DataUsageHandler("Wi-Fi", formatDataSize(getWiFiDataUsage())));
        dataUsageList.add(new DataUsageHandler("Mobile Data", formatDataSize(getMobileDataUsage())));

        DataUsageAdapter adapter = new DataUsageAdapter(dataUsageList);
        recyclerView.setAdapter(adapter);
        startUpdatingRecyclerView(dataUsageList, adapter);
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isProcessRunning = preferences.getBoolean(KEY_PROCESS_STATE, true);

        if (isProcessRunning)
        {
            startFloatingSpeedService();
        }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
            {
                Toast.makeText(this, "Please enable 'Display over other apps' permission.", Toast.LENGTH_SHORT).show();
            }

        // Check if the app has permission to draw over other apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
        {
            // Request permission using ActivityResultLauncher
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(intent);
        }
    }

    private long getMobileDataUsage() {
        long mobileRxBytes = TrafficStats.getMobileRxBytes(); // Received bytes
        long mobileTxBytes = TrafficStats.getMobileTxBytes(); // Transmitted bytes
        return mobileRxBytes + mobileTxBytes;
    }

    // Get total data usage (Wi-Fi + Mobile Data)
    private long getTotalDataUsage() {
        long totalRxBytes = TrafficStats.getTotalRxBytes(); // All received bytes
        long totalTxBytes = TrafficStats.getTotalTxBytes(); // All transmitted bytes
        return totalRxBytes + totalTxBytes;
    }

    // Calculate Wi-Fi data usage (Total - Mobile)
    private long getWiFiDataUsage() {
        return getTotalDataUsage() - getMobileDataUsage();
    }

    // Format data size to a readable format (e.g., KB, MB, GB)
    @SuppressLint("DefaultLocale")
    private String formatDataSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }

    private void startUpdatingRecyclerView(final List<DataUsageHandler> dataUsageList, DataUsageAdapter adapter)
    {
        handler.postDelayed(new Runnable()
        {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void run()
            {
                // Update data usage dynamically
                dataUsageList.get(0).setUsage(formatDataSize(getWiFiDataUsage()));
                dataUsageList.get(1).setUsage(formatDataSize(getMobileDataUsage()));

                // Notify the adapter that data has changed
                adapter.notifyDataSetChanged();

                // Re-run this task every 5 seconds
                handler.postDelayed(this, 5000);
            }
        },5000);
    }
    private void startFloatingSpeedService()
    {
        if (!Settings.canDrawOverlays(this))
        {
            Toast.makeText(this, "Please enable 'Display over other apps' permission in settings.", Toast.LENGTH_LONG).show();
        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                startForegroundService(new Intent(this, FloatingSpeedService.class));
            }
            else
            {
                startService(new Intent(this, FloatingSpeedService.class));
            }
        }
    }
}
