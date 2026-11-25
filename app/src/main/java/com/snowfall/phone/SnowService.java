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
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.HashSet;
import java.util.List;

public class SnowService extends Service {

    public static final String ACTION_START = "com.snowfall.phone.START";
    public static final String ACTION_STOP = "com.snowfall.phone.STOP";
    public static final String ACTION_SANTA = "com.snowfall.phone.SANTA";
    public static final String ACTION_PENGUIN = "com.snowfall.phone.PENGUIN";
    public static final String ACTION_PLOW = "com.snowfall.phone.PLOW";
    public static final String ACTION_GUST = "com.snowfall.phone.GUST";
    public static final String ACTION_EDIT = "com.snowfall.phone.EDIT";

    private static final String CHANNEL = "snowfall";
    private static final int NOTIFICATION_ID = 7;

    private WindowManager wm;
    private SnowView view;
    private ShelfEditor editor;
    private Handler handler;
    private SharedPreferences prefs;
    private final HashSet<String> ignore = new HashSet<>();
    private String launcherPkg = "";
    private String fgPkg = "";
    private long lastQuery = 0L;
    private boolean screenOn = true;
    private boolean viewAdded = false;
    private boolean started = false;
    private boolean editing = false;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(a)) screenOn = false;
            if (Intent.ACTION_SCREEN_ON.equals(a)) screenOn = true;
            applyVisibility();
        }
    };

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            boolean connected = HomeWatcherService.isConnected();
            if (!connected && screenOn && started && prefs.getBoolean("homeOnly", true)) {
                refreshForeground();
                applyVisibility();
            }
            handler.postDelayed(this, connected ? 2000 : 300);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("snow", MODE_PRIVATE);

        ignore.add("com.android.systemui");
        ignore.add("android");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            for (InputMethodInfo i : imm.getEnabledInputMethodList()) ignore.add(i.getPackageName());
        }

        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        if (ri != null && ri.activityInfo != null) launcherPkg = ri.activityInfo.packageName;

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, f);

        HomeWatcherService.listener = new HomeWatcherService.Listener() {
            @Override
            public void onPackage(String pkg) {
                onForeground(pkg);
            }
        };
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

        if (ACTION_START.equals(action)) {
            started = true;
            fgPkg = getPackageName();
        } else if (ACTION_SANTA.equals(action)) {
            view.trigger(SnowView.EV_SANTA);
        } else if (ACTION_PENGUIN.equals(action)) {
            view.trigger(SnowView.EV_PENGUIN);
        } else if (ACTION_PLOW.equals(action)) {
            view.trigger(SnowView.EV_PLOW);
        } else if (ACTION_GUST.equals(action)) {
            view.trigger(SnowView.EV_GUST);
        } else if (ACTION_EDIT.equals(action)) {
            beginEdit();
        }

        applyVisibility();
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

    private WindowManager.LayoutParams overlayParams(boolean touchable) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        if (!touchable) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.alpha = touchable ? 1f : 0.79f;
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        return lp;
    }

    private boolean ensureView() {
        if (view != null && viewAdded) return true;
        view = new SnowView(this);
        view.setVisibility(View.INVISIBLE);
        view.setShelves(ShelfEditor.load(prefs));
        try {
            wm.addView(view, overlayParams(false));
            viewAdded = true;
            return true;
        } catch (Exception e) {
            view = null;
            viewAdded = false;
            return false;
        }
    }

    private void beginEdit() {
        if (editor != null) return;
        editing = true;
        applyVisibility();
        editor = new ShelfEditor(this, ShelfEditor.load(prefs), new ShelfEditor.Listener() {
            @Override
            public void onDone(List<RectF> fractions) {
                ShelfEditor.save(prefs, fractions);
                if (view != null) view.setShelves(fractions);
                endEdit();
            }

            @Override
            public void onCancel() {
                endEdit();
            }
        });
        try {
            wm.addView(editor, overlayParams(true));
        } catch (Exception e) {
            editor = null;
            editing = false;
            if (!started) shutDown();
        }
    }

    private void endEdit() {
        if (editor != null) {
            try {
                wm.removeView(editor);
            } catch (Exception ignored) {
            }
        }
        editor = null;
        editing = false;
        if (!started) shutDown();
        else applyVisibility();
    }

    private void onForeground(String pkg) {
        if (pkg == null || ignore.contains(pkg)) return;
        fgPkg = pkg;
        applyVisibility();
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
                String p = e.getPackageName();
                if (p != null && !ignore.contains(p)) fgPkg = p;
            }
        }
    }

    private void applyVisibility() {
        if (view == null || !viewAdded) return;
        boolean onHome = !launcherPkg.isEmpty() && launcherPkg.equals(fgPkg);
        boolean show = screenOn && started && !editing && (!prefs.getBoolean("homeOnly", true) || onHome);
        int vis = show ? View.VISIBLE : View.INVISIBLE;
        if (view.getVisibility() != vis) view.setVisibility(vis);
        view.setRunning(show);
    }

    private void shutDown() {
        handler.removeCallbacks(poll);
        if (editor != null) {
            try {
                wm.removeView(editor);
            } catch (Exception ignored) {
            }
            editor = null;
        }
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
        started = false;
        editing = false;
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(poll);
        HomeWatcherService.listener = null;
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
        }
        if (editor != null) {
            try {
                wm.removeView(editor);
            } catch (Exception ignored) {
            }
            editor = null;
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
