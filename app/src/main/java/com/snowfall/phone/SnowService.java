package com.snowfall.phone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public class SnowService extends Service {

    public static final String ACTION_START = "com.snowfall.phone.START";
    public static final String ACTION_STOP = "com.snowfall.phone.STOP";
    public static final String ACTION_SANTA = "com.snowfall.phone.SANTA";
    public static final String ACTION_PLOW = "com.snowfall.phone.PLOW";

    private static final String CHANNEL = "snowfall";
    private static final int NOTIFICATION_ID = 7;

    private WindowManager wm;
    private SnowView view;
    private Handler handler;
    private SharedPreferences prefs;
    private String launcherPkg = "";
    private String fgPkg = "";
    private long lastQuery = 0L;
    private boolean screenOn = true;
    private boolean viewAdded = false;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(a)) screenOn = false;
            if (Intent.ACTION_SCREEN_ON.equals(a)) screenOn = true;
            updateVisibility();
        }
    };

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            updateVisibility();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("snow", MODE_PRIVATE);

        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        if (ri != null && ri.activityInfo != null) launcherPkg = ri.activityInfo.packageName;

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, f);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            shutDown();
            return START_NOT_STICKY;
        }

        startInForeground();

        if (!ensureView()) {
            shutDown();
            return START_NOT_STICKY;
        }

        if (ACTION_SANTA.equals(action)) view.startSanta();
        if (ACTION_PLOW.equals(action)) view.startPlow();

        handler.removeCallbacks(poll);
        handler.post(poll);
        return START_NOT_STICKY;
    }

    private void startInForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Snowfall", NotificationManager.IMPORTANCE_LOW));

        Intent stop = new Intent(this, SnowService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_snowflake)
                .setContentTitle("Snowfall is on")
                .setContentText("Tap to open. Use Stop to turn it off.")
                .setOngoing(true)
                .setContentIntent(openPi)
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_snowflake), "Stop", stopPi).build())
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
    }

    private boolean ensureView() {
        if (view != null && viewAdded) return true;
        view = new SnowView(this);
        view.setVisibility(View.INVISIBLE);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.alpha = 0.79f;
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

        try {
            wm.addView(view, lp);
            viewAdded = true;
            return true;
        } catch (Exception e) {
            view = null;
            viewAdded = false;
            return false;
        }
    }

    private void refreshForeground() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return;
        long now = System.currentTimeMillis();
        long from = lastQuery == 0L ? now - 3L * 3600000L : lastQuery - 2000L;
        lastQuery = now;
        UsageEvents events = usm.queryEvents(from, now);
        if (events == null) return;
        UsageEvents.Event e = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            if (e.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                fgPkg = e.getPackageName();
            }
        }
    }

    private void updateVisibility() {
        if (view == null || !viewAdded) return;
        boolean show = screenOn;
        if (show && prefs.getBoolean("homeOnly", true)) {
            refreshForeground();
            show = !launcherPkg.isEmpty() && launcherPkg.equals(fgPkg);
        }
        view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        view.setRunning(show);
    }

    private void shutDown() {
        handler.removeCallbacks(poll);
        if (view != null) {
            view.setRunning(false);
            if (viewAdded) {
                try {
                    wm.removeView(view);
                } catch (Exception ignored) {
                }
            }
        }
        view = null;
        viewAdded = false;
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(poll);
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
        }
        if (view != null && viewAdded) {
            view.setRunning(false);
            try {
                wm.removeView(view);
            } catch (Exception ignored) {
            }
        }
        view = null;
        viewAdded = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
