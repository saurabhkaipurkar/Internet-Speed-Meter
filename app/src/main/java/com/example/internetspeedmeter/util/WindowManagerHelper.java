package com.example.internetspeedmeter.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

public class WindowManagerHelper
{
    public static boolean canCreateOverlay() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static void addFloatingView(View floatingView, WindowManager windowManager) {
        WindowManager.LayoutParams params = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = createLayoutParams();
        }
        windowManager.addView(floatingView, params);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static WindowManager.LayoutParams createLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 10;
        params.y = 10;
        return params;
    }

    public static void setupTouchListener(View floatingView, WindowManager windowManager) {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

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
                        params.x = clamp(initialX + deltaX, getScreenWidth(windowManager) - floatingView.getWidth());
                        params.y = clamp(initialY + deltaY, getScreenHeight(windowManager) - floatingView.getHeight());
                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    default:
                        return false;
                }
            }

            private int clamp(int value, int max) {
                return Math.max(0, Math.min(value, max));
            }

            private int getScreenWidth(WindowManager windowManager) {
                DisplayMetrics metrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(metrics);
                return metrics.widthPixels;
            }

            private int getScreenHeight(WindowManager windowManager) {
                DisplayMetrics metrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(metrics);
                return metrics.heightPixels;
            }
        });
    }
}
