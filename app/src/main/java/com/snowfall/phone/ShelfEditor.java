package com.snowfall.phone;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ShelfEditor extends FrameLayout {

    public interface Listener {
        void onDone(List<RectF> fractions);

        void onCancel();
    }

    private final float d;
    private final Board board;
    private final Listener listener;

    public static List<RectF> load(SharedPreferences p) {
        ArrayList<RectF> out = new ArrayList<>();
        String s = p.getString("shelves", "");
        if (s == null || s.isEmpty()) return out;
        for (String part : s.split(";")) {
            String[] v = part.split(",");
            if (v.length != 4) continue;
            try {
                out.add(new RectF(Float.parseFloat(v[0]), Float.parseFloat(v[1]), Float.parseFloat(v[2]), Float.parseFloat(v[3])));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    public static void save(SharedPreferences p, List<RectF> list) {
        StringBuilder sb = new StringBuilder();
        for (RectF r : list) {
            if (sb.length() > 0) sb.append(';');
            sb.append(r.left).append(',').append(r.top).append(',').append(r.right).append(',').append(r.bottom);
        }
        p.edit().putString("shelves", sb.toString()).apply();
    }

    public ShelfEditor(Context ctx, List<RectF> initial, Listener l) {
        super(ctx);
        listener = l;
        d = ctx.getResources().getDisplayMetrics().density;
        board = new Board(ctx, initial);
        addView(board, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        TextView banner = new TextView(ctx);
        banner.setText("Drag a box over each widget.\nSnow will pile on its top edge.");
        banner.setTextColor(Color.WHITE);
        banner.setTextSize(16);
        banner.setGravity(Gravity.CENTER);
        banner.setTypeface(Typeface.DEFAULT_BOLD);
        banner.setPadding((int) (18 * d), (int) (12 * d), (int) (18 * d), (int) (12 * d));
        GradientDrawable bb = new GradientDrawable();
        bb.setColor(0xDD0B1D3A);
        bb.setCornerRadius(20 * d);
        banner.setBackground(bb);
        LayoutParams bl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        bl.topMargin = (int) (60 * d);
        addView(banner, bl);

        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding((int) (14 * d), 0, (int) (14 * d), 0);
        final Board b = board;
        bar.addView(button(ctx, "Cancel", false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onCancel();
            }
        }));
        bar.addView(button(ctx, "Delete", false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                b.deleteSelected();
            }
        }));
        bar.addView(button(ctx, "Clear", false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                b.clearAll();
            }
        }));
        bar.addView(button(ctx, "Done", true, new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDone(b.fractions());
            }
        }));
        LayoutParams bp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        bp.bottomMargin = (int) (44 * d);
        addView(bar, bp);
    }

    private Button button(Context ctx, String text, boolean primary, OnClickListener click) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        GradientDrawable g = new GradientDrawable();
        g.setColor(primary ? 0xFF3D8BFF : 0xEE1B3358);
        g.setCornerRadius(26 * d);
        b.setBackground(g);
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (50 * d), 1f);
        lp.setMargins((int) (4 * d), 0, (int) (4 * d), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private class Board extends View {
        private final ArrayList<RectF> rects = new ArrayList<>();
        private List<RectF> pending;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF backup = new RectF();
        private int sel = -1;
        private int mode = 0;
        private int corner = -1;
        private float ax;
        private float ay;
        private float lx;
        private float ly;

        Board(Context c, List<RectF> initial) {
            super(c);
            pending = new ArrayList<>(initial);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            if (pending != null) {
                for (RectF f : pending) rects.add(new RectF(f.left * w, f.top * h, f.right * w, f.bottom * h));
                pending = null;
            }
        }

        List<RectF> fractions() {
            ArrayList<RectF> out = new ArrayList<>();
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return out;
            for (RectF r : rects) {
                if (r.width() < 1f) continue;
                out.add(new RectF(r.left / w, r.top / h, r.right / w, r.bottom / h));
            }
            return out;
        }

        void deleteSelected() {
            if (sel >= 0 && sel < rects.size()) rects.remove(sel);
            sel = -1;
            invalidate();
        }

        void clearAll() {
            rects.clear();
            sel = -1;
            invalidate();
        }

        private int hitCorner(RectF r, float x, float y) {
            float[] cx = {r.left, r.right, r.right, r.left};
            float[] cy = {r.top, r.top, r.bottom, r.bottom};
            float rad = 28f * d;
            for (int i = 0; i < 4; i++) {
                float dx = x - cx[i];
                float dy = y - cy[i];
                if (dx * dx + dy * dy <= rad * rad) return i;
            }
            return -1;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    mode = 0;
                    corner = -1;
                    if (sel >= 0 && sel < rects.size()) {
                        int c = hitCorner(rects.get(sel), x, y);
                        if (c >= 0) {
                            corner = c;
                            mode = 3;
                            backup.set(rects.get(sel));
                            invalidate();
                            return true;
                        }
                    }
                    for (int i = rects.size() - 1; i >= 0; i--) {
                        if (rects.get(i).contains(x, y)) {
                            sel = i;
                            mode = 2;
                            lx = x;
                            ly = y;
                            invalidate();
                            return true;
                        }
                    }
                    rects.add(new RectF(x, y, x, y));
                    sel = rects.size() - 1;
                    mode = 1;
                    ax = x;
                    ay = y;
                    invalidate();
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (sel < 0 || sel >= rects.size()) return true;
                    RectF r = rects.get(sel);
                    if (mode == 1) {
                        r.set(Math.min(ax, x), Math.min(ay, y), Math.max(ax, x), Math.max(ay, y));
                    } else if (mode == 2) {
                        float dx = Math.max(-r.left, Math.min(getWidth() - r.right, x - lx));
                        float dy = Math.max(-r.top, Math.min(getHeight() - r.bottom, y - ly));
                        r.offset(dx, dy);
                        lx = x;
                        ly = y;
                    } else if (mode == 3) {
                        float fx = (corner == 0 || corner == 3) ? backup.right : backup.left;
                        float fy = (corner == 0 || corner == 1) ? backup.bottom : backup.top;
                        r.set(Math.min(fx, x), Math.min(fy, y), Math.max(fx, x), Math.max(fy, y));
                    }
                    invalidate();
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (sel >= 0 && sel < rects.size()) {
                        RectF r = rects.get(sel);
                        boolean small = r.width() < 48f * d || r.height() < 28f * d;
                        if (small && mode == 1) {
                            rects.remove(sel);
                            sel = -1;
                        } else if (small && mode == 3) {
                            r.set(backup);
                        }
                    }
                    mode = 0;
                    invalidate();
                    return true;
                }
                default:
                    return true;
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            c.drawColor(Color.argb(70, 5, 15, 40));
            for (int i = 0; i < rects.size(); i++) {
                RectF r = rects.get(i);
                boolean s = i == sel;
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.argb(s ? 70 : 40, 130, 200, 255));
                c.drawRoundRect(r, 10f * d, 10f * d, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2f * d);
                p.setColor(Color.argb(255, 170, 215, 255));
                c.drawRoundRect(r, 10f * d, 10f * d, p);
                p.setStrokeWidth(5f * d);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setColor(Color.WHITE);
                c.drawLine(r.left + 10f * d, r.top, r.right - 10f * d, r.top, p);
                p.setStyle(Paint.Style.FILL);
                p.setTextSize(11f * d);
                p.setColor(Color.argb(230, 255, 255, 255));
                c.drawText("snow lands here", r.left + 12f * d, r.top + 20f * d, p);
                if (s) {
                    float[] cx = {r.left, r.right, r.right, r.left};
                    float[] cy = {r.top, r.top, r.bottom, r.bottom};
                    for (int k = 0; k < 4; k++) {
                        p.setStyle(Paint.Style.FILL);
                        p.setColor(Color.WHITE);
                        c.drawCircle(cx[k], cy[k], 9f * d, p);
                        p.setStyle(Paint.Style.STROKE);
                        p.setStrokeWidth(2.5f * d);
                        p.setColor(0xFF3D8BFF);
                        c.drawCircle(cx[k], cy[k], 9f * d, p);
                    }
                }
            }
        }
    }
}
