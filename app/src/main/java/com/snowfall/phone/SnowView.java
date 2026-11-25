package com.snowfall.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.Choreographer;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class SnowView extends View implements Choreographer.FrameCallback {

    private static class Spark {
        float x, y, vx, vy, grav, life, max, size;
        boolean gold;
    }

    private static final int FLAKES = 110;

    private final float d;
    private final float step;
    private final Random rnd = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path fillPath = new Path();
    private final ArrayList<Spark> sparks = new ArrayList<>();

    private final float[] fx = new float[FLAKES];
    private final float[] fy = new float[FLAKES];
    private final float[] fvy = new float[FLAKES];
    private final float[] fr = new float[FLAKES];
    private final float[] fph = new float[FLAKES];
    private final float[] fsp = new float[FLAKES];
    private final float[] fsw = new float[FLAKES];
    private final int[] fa = new int[FLAKES];

    private float[] dh;
    private float[] prof;
    private int cols;

    private boolean running;
    private long last;
    private float acc;
    private float time;
    private float dayCheck;
    private int lastDay;

    private boolean plowOn;
    private float plowX;

    private boolean santaOn;
    private float santaT;
    private final float santaDur = 13f;
    private float nextSanta = 8f;
    private float santaBaseY;
    private float santaX;
    private float santaY;
    private float santaAng;

    public SnowView(Context context) {
        super(context);
        d = context.getResources().getDisplayMetrics().density;
        step = 6f * d;
        lastDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
    }

    public void setRunning(boolean r) {
        if (r == running) return;
        running = r;
        if (r) {
            last = 0L;
            acc = 0f;
            Choreographer.getInstance().postFrameCallback(this);
        } else {
            Choreographer.getInstance().removeFrameCallback(this);
        }
    }

    public void startPlow() {
        if (plowOn) return;
        plowOn = true;
        plowX = -80f * d;
    }

    public void startSanta() {
        if (santaOn) return;
        santaOn = true;
        santaT = 0f;
        santaBaseY = getHeight() * (0.12f + rnd.nextFloat() * 0.18f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        for (int i = 0; i < FLAKES; i++) resetFlake(i, true);
        cols = (int) (w / step) + 2;
        dh = new float[cols];
        prof = new float[cols];
        float p1 = rnd.nextFloat() * 6.28f;
        float p2 = rnd.nextFloat() * 6.28f;
        float p3 = rnd.nextFloat() * 6.28f;
        for (int i = 0; i < cols; i++) {
            float t = i * step / d;
            float n = 0.5f + 0.25f * (float) Math.sin(t * 0.03f + p1) + 0.15f * (float) Math.sin(t * 0.08f + p2) + 0.1f * (float) Math.sin(t * 0.2f + p3);
            prof[i] = 0.45f + 0.55f * n;
            dh[i] = 3f * d * prof[i];
        }
    }

    private void resetFlake(int i, boolean anywhere) {
        float r0 = 0.8f + rnd.nextFloat() * 1.9f;
        fr[i] = r0 * d;
        fvy[i] = (30f + r0 * 24f) * d;
        fx[i] = rnd.nextFloat() * Math.max(1, getWidth());
        fy[i] = anywhere ? rnd.nextFloat() * Math.max(1, getHeight()) : -rnd.nextFloat() * 40f;
        fph[i] = rnd.nextFloat() * 6.28f;
        fsp[i] = 0.6f + rnd.nextFloat() * 1.2f;
        fsw[i] = (6f + rnd.nextFloat() * 12f) * d;
        fa[i] = Math.min(255, 150 + (int) (r0 * 30f));
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;
        if (last == 0L) last = frameTimeNanos;
        float dt = (frameTimeNanos - last) / 1000000000f;
        last = frameTimeNanos;
        if (dt > 0.1f) dt = 0.1f;
        acc += dt;
        if (acc >= 0.033f) {
            advance(acc);
            acc = 0f;
            invalidate();
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void addSpray(float x, float y, float strength) {
        if (sparks.size() > 150) return;
        Spark s = new Spark();
        s.x = x;
        s.y = y;
        s.vx = (40f + rnd.nextFloat() * 160f * strength) * d;
        s.vy = -(60f + rnd.nextFloat() * 160f * strength) * d;
        s.grav = 420f * d;
        s.max = 0.5f + rnd.nextFloat() * 0.6f;
        s.life = s.max;
        s.size = (1f + rnd.nextFloat() * 1.6f) * d;
        s.gold = false;
        sparks.add(s);
    }

    private void advance(float dt) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0 || dh == null) return;
        time += dt;

        float wind = (float) Math.sin(time * 0.15f) * 10f * d;
        for (int i = 0; i < FLAKES; i++) {
            fph[i] += dt * fsp[i];
            fx[i] += ((float) Math.sin(fph[i]) * fsw[i] + wind) * dt;
            fy[i] += fvy[i] * dt;
            if (fy[i] > h + 10f) resetFlake(i, false);
            if (fx[i] < -20f) fx[i] += w + 40f;
            else if (fx[i] > w + 20f) fx[i] -= w + 40f;
        }

        float cap = 30f * d;
        for (int i = 0; i < cols; i++) {
            float c = cap * prof[i];
            dh[i] += dt * (cap / 900f) * prof[i];
            if (dh[i] > c) dh[i] = c;
        }

        dayCheck += dt;
        if (dayCheck > 30f) {
            dayCheck = 0f;
            int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            if (day != lastDay) {
                lastDay = day;
                startPlow();
            }
        }

        if (plowOn) {
            plowX += 110f * d * dt;
            float blade = plowX + 47f * d;
            for (int i = 0; i < cols; i++) {
                float x = i * step;
                if (x < blade && dh[i] > 0f) {
                    if (dh[i] > 0.8f * d && rnd.nextFloat() < 0.15f) addSpray(x, h - dh[i], 1f);
                    dh[i] = 0f;
                }
            }
            addSpray(blade, h - 6f * d, 1.2f);
            if (plowX > w + 150f * d) plowOn = false;
        }

        if (!santaOn) {
            if (time >= nextSanta) startSanta();
        } else {
            santaT += dt;
            float p = santaT / santaDur;
            if (p >= 1f) {
                santaOn = false;
                nextSanta = time + 90f + rnd.nextFloat() * 90f;
            } else {
                float span = w + 400f * d;
                float amp = h * 0.04f;
                santaX = -220f * d + p * span;
                santaY = santaBaseY + (float) Math.sin(p * 6.9f) * amp;
                float dydp = (float) Math.cos(p * 6.9f) * 6.9f * amp;
                santaAng = (float) (Math.atan2(dydp, span) * 180.0 / Math.PI);
                if (sparks.size() < 150) {
                    for (int k = 0; k < 2; k++) {
                        Spark s = new Spark();
                        s.x = santaX - 50f * d + rnd.nextFloat() * 25f * d;
                        s.y = santaY + (rnd.nextFloat() - 0.5f) * 30f * d;
                        s.vx = -(8f + rnd.nextFloat() * 24f) * d;
                        s.vy = (rnd.nextFloat() - 0.5f) * 24f * d;
                        s.grav = 0f;
                        s.max = 0.9f + rnd.nextFloat() * 0.8f;
                        s.life = s.max;
                        s.size = (1.2f + rnd.nextFloat() * 1.8f) * d;
                        s.gold = true;
                        sparks.add(s);
                    }
                }
            }
        }

        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.life -= dt;
            if (s.life <= 0f) {
                sparks.remove(i);
                continue;
            }
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.vy += s.grav * dt;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (dh == null) return;
        drawDrift(c);
        drawFlakes(c);
        if (plowOn) drawPlow(c);
        drawSparks(c);
        if (santaOn) drawSanta(c);
    }

    private void drawDrift(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        float mx = 0f;
        for (int i = 0; i < cols; i++) if (dh[i] > mx) mx = dh[i];
        if (mx < 0.8f * d) return;

        float px = 0f;
        float py = h - dh[0];
        path.reset();
        path.moveTo(px, py);
        for (int i = 1; i < cols; i++) {
            float x = Math.min(i * step, (float) w);
            float y = h - dh[i];
            path.quadTo(px, py, (px + x) / 2f, (py + y) / 2f);
            px = x;
            py = y;
        }
        path.lineTo(px, py);

        fillPath.set(path);
        fillPath.lineTo(w, h + 2f * d);
        fillPath.lineTo(0f, h + 2f * d);
        fillPath.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0f, h - mx, 0f, h, Color.WHITE, Color.rgb(196, 218, 244), Shader.TileMode.CLAMP));
        c.drawPath(fillPath, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f * d);
        paint.setColor(Color.argb(235, 255, 255, 255));
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawFlakes(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < FLAKES; i++) {
            if (fr[i] > 2.2f * d) {
                paint.setColor(Color.argb(34, 255, 255, 255));
                c.drawCircle(fx[i], fy[i], fr[i] * 2.3f, paint);
            }
            paint.setColor(Color.argb(fa[i], 255, 255, 255));
            c.drawCircle(fx[i], fy[i], fr[i], paint);
        }
    }

    private void drawSparks(Canvas c) {
        for (int i = 0; i < sparks.size(); i++) {
            Spark s = sparks.get(i);
            float a = Math.min(1f, s.life / s.max);
            if (s.gold) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb((int) (255f * a), 255, 226, 120));
                c.drawCircle(s.x, s.y, s.size, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.2f * d);
                paint.setColor(Color.argb((int) (180f * a), 255, 240, 170));
                c.drawLine(s.x - s.size * 2.4f, s.y, s.x + s.size * 2.4f, s.y, paint);
                c.drawLine(s.x, s.y - s.size * 2.4f, s.x, s.y + s.size * 2.4f, paint);
                paint.setStyle(Paint.Style.FILL);
            } else {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb((int) (235f * a), 255, 255, 255));
                c.drawCircle(s.x, s.y, s.size, paint);
            }
        }
    }

    private void poly(Canvas c, int color, float... p) {
        path.reset();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawPath(path, paint);
    }

    private void oval(Canvas c, int color, float l, float t, float r, float b) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawOval(l, t, r, b, paint);
    }

    private void line(Canvas c, int color, float width, float x1, float y1, float x2, float y2) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(color);
        c.drawLine(x1, y1, x2, y2, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawPlow(Canvas c) {
        c.save();
        c.translate(plowX, getHeight() - 2f * d);
        c.scale(d * 0.75f, d * 0.75f);

        poly(c, Color.argb(40, 255, 240, 170), 34f, -22f, 160f, -44f, 160f, -2f);
        oval(c, Color.argb(50, 0, 0, 0), -46f, -3f, 64f, 3f);
        poly(c, 0xFFCE2C2C, -42f, -28f, 32f, -28f, 32f, -10f, -42f, -10f);
        poly(c, 0xFFA01E22, -4f, -28f, 1f, -46f, 24f, -46f, 32f, -28f);
        poly(c, 0xFFB6DCF5, 2f, -43f, 20f, -43f, 26f, -30f, 0f, -30f);
        poly(c, 0xFFF4B024, 40f, -32f, 48f, -34f, 62f, -4f, 54f, -2f, 40f, -4f);
        oval(c, 0xFF24262C, -34f, -18f, -12f, 4f);
        oval(c, 0xFF24262C, 8f, -18f, 30f, 4f);
        oval(c, 0xFFBEC4CD, -27f, -11f, -19f, -3f);
        oval(c, 0xFFBEC4CD, 15f, -11f, 23f, -3f);
        boolean blink = ((int) (time * 5f) % 2) == 0;
        oval(c, blink ? 0xFFFFAA1E : 0xFF785014, 7f, -52f, 15f, -45f);
        line(c, 0xFF464A52, 3f, 32f, -16f, 42f, -16f);

        c.restore();
    }

    private void drawReindeer(Canvas c, float cx, boolean lead) {
        float gal = (float) Math.sin(time * 9f + cx) * 5f;
        int leg = 0xFF422A1C;
        line(c, leg, 3f, cx - 10f, -16f, cx - 18f + gal, -2f);
        line(c, leg, 3f, cx - 4f, -16f, cx - 6f - gal, -1f);
        line(c, leg, 3f, cx + 10f, -16f, cx + 16f + gal, -2f);
        line(c, leg, 3f, cx + 16f, -16f, cx + 22f - gal, -1f);

        int body = 0xFF623E26;
        oval(c, body, cx - 20f, -32f, cx + 22f, -12f);
        poly(c, body, cx + 14f, -30f, cx + 24f, -45f, cx + 31f, -41f, cx + 22f, -24f);
        oval(c, body, cx + 22f, -51f, cx + 38f, -41f);
        oval(c, 0xFFF5F0E8, cx - 24f, -30f, cx - 18f, -24f);

        int ant = 0xFFC4A26E;
        line(c, ant, 1.6f, cx + 28f, -50f, cx + 24f, -64f);
        line(c, ant, 1.6f, cx + 24f, -64f, cx + 18f, -68f);
        line(c, ant, 1.6f, cx + 24f, -64f, cx + 26f, -72f);
        line(c, ant, 1.6f, cx + 33f, -50f, cx + 34f, -64f);
        line(c, ant, 1.6f, cx + 34f, -64f, cx + 40f, -68f);
        line(c, ant, 1.6f, cx + 34f, -64f, cx + 32f, -72f);

        if (lead) {
            oval(c, Color.argb(70, 255, 60, 60), cx + 31f, -52f, cx + 45f, -38f);
            oval(c, 0xFFEB2828, cx + 35f, -48f, cx + 41f, -42f);
        }
    }

    private void drawSanta(Canvas c) {
        c.save();
        c.translate(santaX, santaY);
        c.rotate(santaAng);
        c.scale(d * 0.62f, d * 0.62f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f);
        paint.setColor(Color.argb(230, 205, 170, 110));
        path.reset();
        path.moveTo(38f, -16f);
        path.quadTo(80f, -4f, 120f, -18f);
        path.quadTo(160f, -6f, 200f, -20f);
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);

        drawReindeer(c, 120f, false);
        drawReindeer(c, 200f, true);

        oval(c, 0xFF96623A, -46f, -42f, -16f, -14f);
        poly(c, 0xFFC8242A, -36f, -22f, 22f, -22f, 34f, -34f, 42f, -32f, 40f, -14f, 30f, -4f, -30f, -4f, -40f, -12f);

        int gold = 0xFFE8B83C;
        line(c, gold, 3f, -34f, -12f, 34f, -12f);
        line(c, gold, 3f, -42f, 6f, 30f, 5f);
        line(c, gold, 3f, 30f, 5f, 44f, 2f);
        line(c, gold, 3f, 44f, 2f, 48f, -8f);
        line(c, gold, 3f, 48f, -8f, 44f, -14f);
        line(c, gold, 3f, -24f, -4f, -24f, 5f);
        line(c, gold, 3f, 16f, -4f, 16f, 5f);

        oval(c, 0xFFC8242A, -16f, -48f, 8f, -20f);
        oval(c, 0xFFFFD6AA, -12f, -60f, 4f, -44f);
        oval(c, 0xFFFAFAFA, -12f, -54f, 4f, -42f);
        poly(c, 0xFFC8242A, -14f, -59f, 4f, -59f, -8f, -77f);
        poly(c, 0xFFFAFAFA, -14f, -61f, 5f, -61f, 5f, -56f, -14f, -56f);
        oval(c, 0xFFFAFAFA, -13f, -81f, -5f, -73f);

        c.restore();
    }
}
