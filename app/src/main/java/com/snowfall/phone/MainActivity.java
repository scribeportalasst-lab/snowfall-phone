package com.snowfall.phone;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView status;
    private Button startBtn;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("snow", MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0B1D3A);
        scroll.setFillViewport(true);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(22), dp(52), dp(22), dp(32));
        scroll.addView(col);

        add(col, text("Snowfall", 34, 0xFFFFFFFF), 0);
        add(col, text("Gentle falling snow, a snow drift, a midnight snowplow and Santa on your home screen. Your touches pass straight through it.", 15, 0xFFB9D0EE), 8);

        status = text("", 15, 0xFFE4EEFB);
        add(col, status, 22);

        Button overlayBtn = button("1. Allow display over other apps", false);
        overlayBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        add(col, overlayBtn, 22);

        Button usageBtn = button("2. Allow usage access (so it only shows on the home screen)", false);
        usageBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        add(col, usageBtn, 12);

        Switch homeSwitch = new Switch(this);
        homeSwitch.setText("Only show on the home screen");
        homeSwitch.setTextColor(0xFFFFFFFF);
        homeSwitch.setTextSize(16);
        homeSwitch.setChecked(prefs.getBoolean("homeOnly", true));
        homeSwitch.setOnCheckedChangeListener((v, on) -> {
            prefs.edit().putBoolean("homeOnly", on).apply();
            refresh();
        });
        add(col, homeSwitch, 22);

        startBtn = button("Start snow", true);
        startBtn.setOnClickListener(v -> toggle());
        add(col, startBtn, 22);

        Button santaBtn = button("Send Santa", false);
        santaBtn.setOnClickListener(v -> sendAction(SnowService.ACTION_SANTA));
        add(col, santaBtn, 12);

        Button plowBtn = button("Plow the snow now", false);
        plowBtn.setOnClickListener(v -> sendAction(SnowService.ACTION_PLOW));
        add(col, plowBtn, 12);

        add(col, text("To uninstall: touch and hold the Snowfall icon, tap App info, then Uninstall. To just turn it off, tap Stop snow.", 13, 0xFF8FB0DA), 28);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        new Handler(Looper.getMainLooper()).postDelayed(this::refresh, 600);
    }

    private void toggle() {
        if (isRunning()) {
            sendAction(SnowService.ACTION_STOP);
            new Handler(Looper.getMainLooper()).postDelayed(this::refresh, 600);
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow display over other apps first.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            return;
        }
        if (prefs.getBoolean("homeOnly", true) && !hasUsage()) {
            Toast.makeText(this, "Please allow usage access first, or turn off the home screen only switch.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        Intent i = new Intent(this, SnowService.class);
        i.setAction(SnowService.ACTION_START);
        startForegroundService(i);
        new Handler(Looper.getMainLooper()).postDelayed(this::refresh, 600);
    }

    private void sendAction(String action) {
        if (!isRunning() && !SnowService.ACTION_STOP.equals(action)) {
            Toast.makeText(this, "Start the snow first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, SnowService.class);
        i.setAction(action);
        startService(i);
    }

    private void refresh() {
        boolean overlayOk = Settings.canDrawOverlays(this);
        boolean usageOk = hasUsage();
        boolean running = isRunning();
        status.setText("Display over other apps: " + (overlayOk ? "Allowed" : "Not allowed")
                + "\nUsage access: " + (usageOk ? "Allowed" : "Not allowed")
                + "\nSnow is: " + (running ? "ON" : "OFF"));
        startBtn.setText(running ? "Stop snow" : "Start snow");
    }

    private boolean hasUsage() {
        AppOpsManager ao = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = ao.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (SnowService.class.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private Button button(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(16);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? 0xFF3D8BFF : 0xFF1B3358);
        bg.setCornerRadius(dp(28));
        b.setBackground(bg);
        return b;
    }

    private void add(LinearLayout col, View v, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        col.addView(v, lp);
    }
}
