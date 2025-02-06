package com.example.internetspeedmeter.util;

import android.net.TrafficStats;

public class SpeedCalculator
{
    public static long getTotalRxBytes() {
        return TrafficStats.getTotalRxBytes();
    }

    public static long getTotalTxBytes() {
        return TrafficStats.getTotalTxBytes();
    }

    public static long calculateSpeed(long currentBytes, long previousBytes, long currentTime, long previousTime) {
        long elapsedTime = Math.max(1, currentTime - previousTime); // Prevent division by zero
        long speed = (currentBytes - previousBytes) * 1000 / elapsedTime;
        return Math.max(0, speed); // Prevent negative values
    }

    public static String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond >= 1024 * 1024) {
            return (bytesPerSecond / (1024 * 1024)) + " MB/s";
        } else if (bytesPerSecond >= 1024) {
            return (bytesPerSecond / 1024) + " KB/s";
        } else {
            return bytesPerSecond + " B/s";
        }
    }
}
