package com.snowfall.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

final class Ui {

    static final int SNOWFLAKE = 0;
    static final int CHECK = 1;
    static final int ALERT = 2;
    static final int SHIELD = 3;
    static final int HAT = 4;
    static final int PENGUIN = 5;
    static final int PLOW = 6;
    static final int WIND = 7;
    static final int SHELF = 8;
    static final int BOLT = 9;

    private Ui() {
    }

    static void icon(Canvas c, int t, float cx, float cy, float r, int color, Paint p) {
        Path a = new Path();
        p.setColor(color);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(r * 0.16f);
        p.setStyle(Paint.Style.STROKE);
        switch (t) {
            case SNOWFLAKE:
                for (int k = 0; k < 3; k++) {
                    double an = k * Math.PI / 3.0 + Math.PI / 6.0;
                    float dx = (float) Math.cos(an) * r;
                    float dy = (float) Math.sin(an) * r;
                    c.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, p);
                }
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy, r * 0.14f, p);
                break;
            case CHECK:
                a.moveTo(cx - r * 0.55f, cy);
                a.lineTo(cx - r * 0.15f, cy + r * 0.4f);
                a.lineTo(cx + r * 0.6f, cy - r * 0.4f);
                c.drawPath(a, p);
                break;
            case ALERT:
                a.moveTo(cx, cy - r * 0.8f);
                a.lineTo(cx + r * 0.85f, cy + r * 0.65f);
                a.lineTo(cx - r * 0.85f, cy + r * 0.65f);
                a.close();
                c.drawPath(a, p);
                c.drawLine(cx, cy - r * 0.25f, cx, cy + r * 0.15f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy + r * 0.42f, r * 0.09f, p);
                break;
            case SHIELD:
                a.moveTo(cx, cy - r * 0.9f);
                a.lineTo(cx + r * 0.75f, cy - r * 0.6f);
                a.quadTo(cx + r * 0.75f, cy + r * 0.45f, cx, cy + r * 0.95f);
                a.quadTo(cx - r * 0.75f, cy + r * 0.45f, cx - r * 0.75f, cy - r * 0.6f);
                a.close();
                c.drawPath(a, p);
                a.reset();
                a.moveTo(cx - r * 0.3f, cy);
                a.lineTo(cx - r * 0.05f, cy + r * 0.25f);
                a.lineTo(cx + r * 0.35f, cy - r * 0.25f);
                c.drawPath(a, p);
                break;
            case HAT:
                p.setStyle(Paint.Style.FILL);
                a.moveTo(cx - r * 0.7f, cy + r * 0.45f);
                a.lineTo(cx + r * 0.6f, cy + r * 0.45f);
                a.quadTo(cx + r * 0.4f, cy - r * 0.5f, cx + r * 0.2f, cy - r * 0.7f);
                a.close();
                c.drawPath(a, p);
                c.drawRoundRect(cx - r * 0.85f, cy + r * 0.35f, cx + r * 0.75f, cy + r * 0.75f, r * 0.2f, r * 0.2f, p);
                c.drawCircle(cx + r * 0.28f, cy - r * 0.72f, r * 0.2f, p);
                break;
            case PENGUIN:
                c.drawOval(cx - r * 0.6f, cy - r * 0.9f, cx + r * 0.6f, cy + r * 0.9f, p);
                c.drawOval(cx - r * 0.34f, cy - r * 0.2f, cx + r * 0.34f, cy + r * 0.7f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx - r * 0.2f, cy - r * 0.5f, r * 0.08f, p);
                c.drawCircle(cx + r * 0.2f, cy - r * 0.5f, r * 0.08f, p);
                a.moveTo(cx - r * 0.12f, cy - r * 0.34f);
                a.lineTo(cx + r * 0.12f, cy - r * 0.34f);
                a.lineTo(cx, cy - r * 0.16f);
                a.close();
                c.drawPath(a, p);
                break;
            case PLOW:
                c.drawRoundRect(cx - r * 0.9f, cy - r * 0.15f, cx + r * 0.25f, cy + r * 0.45f, r * 0.1f, r * 0.1f, p);
                a.moveTo(cx - r * 0.5f, cy - r * 0.15f);
                a.lineTo(cx - r * 0.35f, cy - r * 0.6f);
                a.lineTo(cx + r * 0.05f, cy - r * 0.6f);
                a.lineTo(cx + r * 0.25f, cy - r * 0.15f);
                c.drawPath(a, p);
                p.setStyle(Paint.Style.FILL);
                a.reset();
                a.moveTo(cx + r * 0.4f, cy - r * 0.4f);
                a.lineTo(cx + r * 0.85f, cy + r * 0.5f);
                a.lineTo(cx + r * 0.5f, cy + r * 0.5f);
                a.close();
                c.drawPath(a, p);
                c.drawCircle(cx - r * 0.55f, cy + r * 0.5f, r * 0.17f, p);
                c.drawCircle(cx + r * 0.05f, cy + r * 0.5f, r * 0.17f, p);
                break;
            case WIND:
                a.moveTo(cx - r * 0.85f, cy - r * 0.35f);
                a.quadTo(cx + r * 0.3f, cy - r * 0.35f, cx + r * 0.45f, cy - r * 0.6f);
                a.quadTo(cx + r * 0.7f, cy - r * 0.85f, cx + r * 0.8f, cy - r * 0.5f);
                c.drawPath(a, p);
                a.reset();
                a.moveTo(cx - r * 0.85f, cy);
                a.lineTo(cx + r * 0.55f, cy);
                a.quadTo(cx + r * 0.9f, cy, cx + r * 0.85f, cy + r * 0.3f);
                c.drawPath(a, p);
                a.reset();
                a.moveTo(cx - r * 0.6f, cy + r * 0.38f);
                a.quadTo(cx + r * 0.1f, cy + r * 0.38f, cx + r * 0.2f, cy + r * 0.6f);
                a.quadTo(cx + r * 0.3f, cy + r * 0.85f, cx + r * 0.55f, cy + r * 0.62f);
                c.drawPath(a, p);
                break;
            case SHELF:
                c.drawRoundRect(cx - r * 0.85f, cy - r * 0.05f, cx + r * 0.85f, cy + r * 0.8f, r * 0.18f, r * 0.18f, p);
                a.moveTo(cx - r * 0.85f, cy - r * 0.05f);
                a.quadTo(cx - r * 0.5f, cy - r * 0.75f, cx - r * 0.1f, cy - r * 0.18f);
                a.quadTo(cx + r * 0.3f, cy - r * 0.8f, cx + r * 0.85f, cy - r * 0.05f);
                c.drawPath(a, p);
                break;
            case BOLT:
                p.setStyle(Paint.Style.FILL);
                a.moveTo(cx + r * 0.2f, cy - r * 0.95f);
                a.lineTo(cx - r * 0.6f, cy + r * 0.15f);
                a.lineTo(cx - r * 0.05f, cy + r * 0.15f);
                a.lineTo(cx - r * 0.2f, cy + r * 0.95f);
                a.lineTo(cx + r * 0.6f, cy - r * 0.2f);
                a.lineTo(cx + r * 0.05f, cy - r * 0.2f);
                a.close();
                c.drawPath(a, p);
                break;
            default:
                break;
        }
        p.setStyle(Paint.Style.FILL);
    }

    static final class IconView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int type;
        private int color = Color.WHITE;
        private int bg = 0;

        IconView(Context c) {
            super(c);
        }

        void set(int type, int color, int bg) {
            this.type = type;
            this.color = color;
            this.bg = bg;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            float r = Math.min(w, h) / 2f;
            if (bg != 0) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(bg);
                c.drawCircle(w / 2f, h / 2f, r, p);
            }
            icon(c, type, w / 2f, h / 2f, r * (bg != 0 ? 0.52f : 0.9f), color, p);
        }
    }

    static final class Backdrop extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random rnd = new Random();
        private final float d;
        private final float[] x = new float[56];
        private final float[] y = new float[56];
        private final float[] v = new float[56];
        private final float[] r = new float[56];
        private final float[] ph = new float[56];
        private Shader sky;
        private Shader moon;
        private long last = 0L;
        private float t = 0f;

        Backdrop(Context c) {
            super(c);
            d = c.getResources().getDisplayMetrics().density;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            sky = new LinearGradient(0f, 0f, 0f, h, new int[]{0xFF06122B, 0xFF0E2F66, 0xFF0A1C3D}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
            moon = new RadialGradient(w * 0.85f, h * 0.1f, w * 0.7f, new int[]{0x553C7CD6, 0x00000000}, null, Shader.TileMode.CLAMP);
            for (int i = 0; i < x.length; i++) {
                x[i] = rnd.nextFloat() * w;
                y[i] = rnd.nextFloat() * h;
                v[i] = (10f + rnd.nextFloat() * 22f) * d;
                r[i] = (0.8f + rnd.nextFloat() * 2.2f) * d;
                ph[i] = rnd.nextFloat() * 6.28f;
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth();
            int h = getHeight();
            long now = System.nanoTime();
            float dt = last == 0L ? 0f : (now - last) / 1000000000f;
            last = now;
            if (dt > 0.1f) dt = 0.1f;
            t += dt;
            p.setStyle(Paint.Style.FILL);
            p.setShader(sky);
            c.drawRect(0f, 0f, w, h, p);
            p.setShader(moon);
            c.drawRect(0f, 0f, w, h, p);
            p.setShader(null);
            for (int i = 0; i < x.length; i++) {
                y[i] += v[i] * dt;
                x[i] += (float) Math.sin(t * 0.7f + ph[i]) * 8f * d * dt;
                if (y[i] > h + 10f) {
                    y[i] = -10f;
                    x[i] = rnd.nextFloat() * w;
                }
                p.setColor(Color.argb(r[i] > 2f * d ? 150 : 100, 255, 255, 255));
                c.drawCircle(x[i], y[i], r[i], p);
            }
            postInvalidateOnAnimation();
        }
    }
}
