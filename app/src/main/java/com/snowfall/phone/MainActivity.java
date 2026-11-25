package com.snowfall.phone;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int M = LinearLayout.LayoutParams.MATCH_PARENT;
    private static final int W = LinearLayout.LayoutParams.WRAP_CONTENT;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int SOFT = 0xFFB9D0EE;

    private static class Row {
        Ui.IconView icon;
        TextView chip;
    }

    private SharedPreferences prefs;
    private float d;
    private TextView bigBtn;
    private TextView statusLine;
    private View ring;
    private Row ovRow;
    private Row axRow;
    private Row usRow;
    private boolean running;
    private boolean pulsing;
    private boolean lastOv;
    private boolean lastAx;
    private boolean lastUs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        d = getResources().getDisplayMetrics().density;
        prefs = getSharedPreferences("snow", MODE_PRIVATE);
        getWindow().setStatusBarColor(0xFF06122B);
        getWindow().setNavigationBarColor(0xFF0A1C3D);

        FrameLayout root = new FrameLayout(this);
        root.addView(new Ui.Backdrop(this), new FrameLayout.LayoutParams(M, M));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new FrameLayout.LayoutParams(M, M));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(40), dp(20), dp(36));
        scroll.addView(col);

        ArrayList<View> parts = new ArrayList<>();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Ui.IconView logo = new Ui.IconView(this);
        logo.set(Ui.SNOWFLAKE, WHITE, 0x333D8BFF);
        header.addView(logo, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(tv("Snowfall", 30, WHITE, true));
        titles.addView(tv("Winter for your home screen", 14, SOFT, false));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(W, W);
        tp.leftMargin = dp(14);
        header.addView(titles, tp);
        col.addView(header, new LinearLayout.LayoutParams(M, W));
        parts.add(header);
        ObjectAnimator spin = ObjectAnimator.ofFloat(logo, View.ROTATION, 0f, 360f);
        spin.setDuration(24000);
        spin.setRepeatCount(ObjectAnimator.INFINITE);
        spin.setInterpolator(new LinearInterpolator());
        spin.start();

        FrameLayout hero = new FrameLayout(this);
        ring = new View(this);
        GradientDrawable rg = new GradientDrawable();
        rg.setShape(GradientDrawable.OVAL);
        rg.setColor(Color.TRANSPARENT);
        rg.setStroke(dp(3), 0xFF7CC4FF);
        ring.setBackground(rg);
        ring.setAlpha(0f);
        hero.addView(ring, new FrameLayout.LayoutParams(dp(176), dp(176), Gravity.CENTER));
        bigBtn = tv("START", 22, WHITE, true);
        bigBtn.setGravity(Gravity.CENTER);
        bigBtn.setLetterSpacing(0.12f);
        bigBtn.setOnClickListener(v -> toggle());
        press(bigBtn);
        hero.addView(bigBtn, new FrameLayout.LayoutParams(dp(176), dp(176), Gravity.CENTER));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(M, dp(240));
        hp.topMargin = dp(18);
        col.addView(hero, hp);
        parts.add(hero);

        statusLine = tv("", 14, SOFT, false);
        statusLine.setGravity(Gravity.CENTER);
        col.addView(statusLine, new LinearLayout.LayoutParams(M, W));
        parts.add(statusLine);

        LinearLayout setup = card();
        setup.addView(tv("Setup", 17, WHITE, true));
        ovRow = row(setup, "Show over other apps", "Required. Lets snow appear on top of your home screen.", v -> openOverlay());
        axRow = row(setup, "Instant home detection", "Recommended. Hides snow in apps, brings it back instantly.", v -> showAccessibilityDialog());
        usRow = row(setup, "Backup detection", "Optional. Used only if instant detection is off.", v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        col.addView(setup, lp(M, W, 18));
        parts.add(setup);

        LinearLayout widgets = card();
        LinearLayout wh = new LinearLayout(this);
        wh.setOrientation(LinearLayout.HORIZONTAL);
        wh.setGravity(Gravity.CENTER_VERTICAL);
        Ui.IconView wi = new Ui.IconView(this);
        wi.set(Ui.SHELF, 0xFFBFE3FF, 0x2E7CC4FF);
        wh.addView(wi, new LinearLayout.LayoutParams(dp(40), dp(40)));
        TextView wt = tv("Snow on your widgets", 17, WHITE, true);
        LinearLayout.LayoutParams wtp = new LinearLayout.LayoutParams(W, W);
        wtp.leftMargin = dp(12);
        wh.addView(wt, wtp);
        widgets.addView(wh);
        widgets.addView(tv("Android does not tell apps where your widgets are, so you draw a box over each one once. Snow then piles on its top edge.", 13, SOFT, false), lp(M, W, 10));
        TextView placeBtn = tv("Place snow on my widgets", 15, WHITE, true);
        placeBtn.setGravity(Gravity.CENTER);
        placeBtn.setPadding(dp(16), dp(14), dp(16), dp(14));
        placeBtn.setBackground(shape(0xFF3D8BFF, 26, 0, 0));
        placeBtn.setOnClickListener(v -> editShelves());
        press(placeBtn);
        widgets.addView(placeBtn, lp(M, W, 14));
        col.addView(widgets, lp(M, W, 14));
        parts.add(widgets);

        LinearLayout events = card();
        events.addView(tv("Preview an event", 17, WHITE, true));
        events.addView(tv("Once started, these happen by themselves at random times.", 13, SOFT, false), lp(M, W, 4));
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        grid.addView(eventChip(Ui.HAT, "Santa", SnowService.ACTION_SANTA, false));
        grid.addView(eventChip(Ui.PENGUIN, "Penguin", SnowService.ACTION_PENGUIN, true));
        grid.addView(eventChip(Ui.PLOW, "Plow", SnowService.ACTION_PLOW, true));
        grid.addView(eventChip(Ui.WIND, "Gust", SnowService.ACTION_GUST, false));
        events.addView(grid, lp(M, W, 12));
        col.addView(events, lp(M, W, 14));
        parts.add(events);

        LinearLayout opts = card();
        Switch homeSwitch = new Switch(this);
        homeSwitch.setText("Only show on the home screen");
        homeSwitch.setTextColor(WHITE);
        homeSwitch.setTextSize(15);
        homeSwitch.setChecked(prefs.getBoolean("homeOnly", true));
        homeSwitch.setOnCheckedChangeListener((v, on) -> prefs.edit().putBoolean("homeOnly", on).apply());
        opts.addView(homeSwitch);
        opts.addView(tv("Recommended. Snow hides inside apps so nothing is ever blocked, and saves battery.", 12, SOFT, false), lp(M, W, 6));
        col.addView(opts, lp(M, W, 14));
        parts.add(opts);

        LinearLayout safe = card();
        LinearLayout sh = new LinearLayout(this);
        sh.setOrientation(LinearLayout.HORIZONTAL);
        sh.setGravity(Gravity.CENTER_VERTICAL);
        Ui.IconView si = new Ui.IconView(this);
        si.set(Ui.SHIELD, 0xFF7CF0B5, 0x2E7CF0B5);
        sh.addView(si, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(W, W);
        stp.leftMargin = dp(12);
        sh.addView(tv("Safe by design", 17, WHITE, true), stp);
        safe.addView(sh);
        safe.addView(safeLine("Your taps and swipes pass straight through the snow."), lp(M, W, 12));
        safe.addView(safeLine("It hides inside apps and only shows on your home screen."), lp(M, W, 8));
        safe.addView(safeLine("No internet access, no ads, no account. Nothing leaves your phone."), lp(M, W, 8));
        safe.addView(safeLine("Remove it anytime: tap Stop, or touch and hold the icon, App info, Uninstall."), lp(M, W, 8));
        col.addView(safe, lp(M, W, 14));
        parts.add(safe);

        setContentView(root);

        for (int i = 0; i < parts.size(); i++) {
            View v = parts.get(i);
            v.setAlpha(0f);
            v.setTranslationY(dp(36));
            v.animate().alpha(1f).translationY(0f).setStartDelay(70L * i).setDuration(450).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        new Handler(Looper.getMainLooper()).postDelayed(this::refresh, 600);
    }

    private int dp(float v) {
        return (int) (v * d + 0.5f);
    }

    private TextView tv(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private GradientDrawable shape(int fill, float radiusDp, int stroke, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(Math.max(1, dp(strokeDp)), stroke);
        return g;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(topDp);
        return p;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        c.setBackground(shape(0x2EFFFFFF, 22, 0x33FFFFFF, 1));
        return c;
    }

    private void press(final View v) {
        v.setOnTouchListener((view, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start();
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) view.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            return false;
        });
    }

    private TextView chip(String s) {
        TextView t = tv(s, 12, WHITE, true);
        t.setPadding(dp(12), dp(6), dp(12), dp(6));
        return t;
    }

    private Row row(LinearLayout parent, String title, String sub, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(10), 0, dp(10));
        r.setOnClickListener(click);
        press(r);
        Row o = new Row();
        o.icon = new Ui.IconView(this);
        r.addView(o.icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(tv(title, 15, WHITE, true));
        texts.addView(tv(sub, 12, 0xFF9DB9DE, false));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, W, 1f);
        tl.leftMargin = dp(12);
        tl.rightMargin = dp(10);
        r.addView(texts, tl);
        o.chip = chip("");
        r.addView(o.chip);
        parent.addView(r, new LinearLayout.LayoutParams(M, W));
        return o;
    }

    private View eventChip(int icon, String label, final String action, final boolean needsShelves) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(0, dp(6), 0, dp(6));
        Ui.IconView i = new Ui.IconView(this);
        i.set(icon, WHITE, 0x333D8BFF);
        c.addView(i, new LinearLayout.LayoutParams(dp(50), dp(50)));
        TextView t = tv(label, 12, SOFT, false);
        t.setGravity(Gravity.CENTER);
        c.addView(t, lp(W, W, 6));
        c.setOnClickListener(v -> {
            if (needsShelves && prefs.getString("shelves", "").isEmpty()) {
                Toast.makeText(this, "Place snow on your widgets first.", Toast.LENGTH_SHORT).show();
                return;
            }
            send(action);
        });
        press(c);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, W, 1f));
        return c;
    }

    private View safeLine(String s) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        Ui.IconView ic = new Ui.IconView(this);
        ic.set(Ui.CHECK, 0xFF7CF0B5, 0);
        r.addView(ic, new LinearLayout.LayoutParams(dp(20), dp(20)));
        TextView t = tv(s, 13, 0xFFD5E4F8, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, W, 1f);
        p.leftMargin = dp(10);
        r.addView(t, p);
        return r;
    }

    private void setRow(Row r, boolean ok, boolean was, String offLabel) {
        r.icon.set(ok ? Ui.CHECK : Ui.ALERT, ok ? 0xFF7CF0B5 : 0xFFFFC24D, ok ? 0x2E7CF0B5 : 0x2EFFC24D);
        r.chip.setText(ok ? "On" : offLabel);
        r.chip.setBackground(shape(ok ? 0x4D2FBF7A : 0xFF3D8BFF, 16, 0, 0));
        if (ok && !was) {
            r.icon.setScaleX(0.3f);
            r.icon.setScaleY(0.3f);
            r.icon.animate().scaleX(1f).scaleY(1f).setDuration(420).setInterpolator(new OvershootInterpolator(2.4f)).start();
        }
    }

    private void refresh() {
        boolean ov = Settings.canDrawOverlays(this);
        boolean ax = axEnabled();
        boolean us = hasUsage();
        running = isRunning();

        setRow(ovRow, ov, lastOv, "Allow");
        setRow(axRow, ax, lastAx, "Enable");
        setRow(usRow, us, lastUs, "Optional");
        lastOv = ov;
        lastAx = ax;
        lastUs = us;

        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientRadius(dp(110));
        g.setColors(running ? new int[]{0xFFD6F7FF, 0xFF3CC9E6} : new int[]{0xFF5AB0FF, 0xFF2158D0});
        bigBtn.setBackground(g);
        bigBtn.setText(running ? "STOP" : "START");
        bigBtn.setTextColor(running ? 0xFF06284A : WHITE);
        statusLine.setText(running ? "Snow is on. Go to your home screen." : "Tap to start the snow");

        if (running && !pulsing) {
            pulsing = true;
            pulse();
        } else if (!running) {
            pulsing = false;
        }
    }

    private void pulse() {
        if (!pulsing) return;
        ring.setScaleX(1f);
        ring.setScaleY(1f);
        ring.setAlpha(0.7f);
        ring.animate().scaleX(1.4f).scaleY(1.4f).alpha(0f).setDuration(1700).setInterpolator(new DecelerateInterpolator()).withEndAction(this::pulse).start();
    }

    private void openOverlay() {
        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
    }

    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Instant home detection")
                .setMessage("This tells Snowfall which app is open, so snow hides inside apps and comes back the instant you reach the home screen. It only sees the app name. It cannot read your screen or what you type.\n\nOn the next screen, open Installed apps, tap Snowfall and turn it on.\n\nIf the switch is greyed out: tap Open app info, tap the three dots at the top right, tap Allow restricted settings, then try again.")
                .setPositiveButton("Open Accessibility", (dg, w) -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .setNeutralButton("Open app info", (dg, w) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggle() {
        if (isRunning()) {
            send(SnowService.ACTION_STOP);
            delayRefresh();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow display over other apps first.", Toast.LENGTH_LONG).show();
            openOverlay();
            return;
        }
        if (prefs.getBoolean("homeOnly", true) && !axEnabled() && !hasUsage()) {
            showAccessibilityDialog();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        Intent i = new Intent(this, SnowService.class);
        i.setAction(SnowService.ACTION_START);
        startForegroundService(i);
        delayRefresh();
    }

    private void editShelves() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow display over other apps first.", Toast.LENGTH_LONG).show();
            openOverlay();
            return;
        }
        Intent i = new Intent(this, SnowService.class);
        i.setAction(SnowService.ACTION_EDIT);
        startForegroundService(i);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent h = new Intent(Intent.ACTION_MAIN);
            h.addCategory(Intent.CATEGORY_HOME);
            h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(h);
        }, 500);
    }

    private void send(String action) {
        if (!isRunning() && !SnowService.ACTION_STOP.equals(action)) {
            Toast.makeText(this, "Start the snow first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, SnowService.class);
        i.setAction(action);
        startService(i);
    }

    private void delayRefresh() {
        new Handler(Looper.getMainLooper()).postDelayed(this::refresh, 600);
    }

    private boolean axEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        return enabled.contains(new ComponentName(this, HomeWatcherService.class).flattenToString());
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
}
