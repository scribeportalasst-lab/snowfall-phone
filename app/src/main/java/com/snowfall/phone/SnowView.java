package com.snowfall.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Choreographer;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnowView extends View implements Choreographer.FrameCallback {

    public static final int EV_NONE = 0;
    public static final int EV_SANTA = 1;
    public static final int EV_PENGUIN = 2;
    public static final int EV_PLOW = 3;
    public static final int EV_GUST = 4;

    private static final int FLAKES = 120;
    private static final int CRYSTALS = 14;
    private static final float CAP = 28f;

    private static class Spark {
        float x, y, vx, vy, grav, life, max, size;
        int kind;
    }

    private static class Shelf {
        RectF r;
        float x0;
        int cols;
        float[] h;
        float[] prof;
        float[] gx = new float[6];
        float[] gp = new float[6];
        LinearGradient shadow, front, back;
    }

    private final float d;
    private final float step;
    private final Random rnd = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path ap = new Path();
    private final Path pileTop = new Path();
    private final Path pileFill = new Path();
    private final ArrayList<Spark> sparks = new ArrayList<>();
    private final ArrayList<Shelf> shelves = new ArrayList<>();
    private List<RectF> fractions = new ArrayList<>();

    private final float[] fx = new float[FLAKES];
    private final float[] fy = new float[FLAKES];
    private final float[] fvy = new float[FLAKES];
    private final float[] fr = new float[FLAKES];
    private final float[] fph = new float[FLAKES];
    private final float[] fsp = new float[FLAKES];
    private final float[] fsw = new float[FLAKES];
    private final float[] frot = new float[FLAKES];
    private final float[] frs = new float[FLAKES];
    private final int[] fa = new int[FLAKES];
    private final int[] ft = new int[FLAKES];

    private final LinearGradient gSleigh = new LinearGradient(0f, -46f, 0f, -3f, 0xFFE8433F, 0xFF9C1C24, Shader.TileMode.CLAMP);
    private final LinearGradient gDeer = new LinearGradient(0f, -34f, 0f, -12f, 0xFF93603C, 0xFF5A3822, Shader.TileMode.CLAMP);
    private final LinearGradient gTruck = new LinearGradient(0f, -34f, 0f, -12f, 0xFFEC4A40, 0xFFA82227, Shader.TileMode.CLAMP);
    private final LinearGradient gCoat = new LinearGradient(0f, -54f, 0f, -22f, 0xFFE23B3B, 0xFFA01E26, Shader.TileMode.CLAMP);
    private final LinearGradient gBlade = new LinearGradient(0f, -34f, 0f, -2f, 0xFFFFCB4C, 0xFFE0951C, Shader.TileMode.CLAMP);
    private final LinearGradient gPeng = new LinearGradient(0f, -50f, 0f, -2f, 0xFF33425C, 0xFF121A28, Shader.TileMode.CLAMP);

    private boolean running;
    private long last;
    private long hiddenAt;
    private float acc;
    private float time;

    private int evType = EV_NONE;
    private int lastEv = EV_NONE;
    private float evT;
    private float evDur;
    private float nextEvent = 6f;
    private int evShelf;
    private float evX;
    private float santaBaseY;
    private float santaX;
    private float santaY;
    private float santaAng;

    public SnowView(Context context) {
        super(context);
        d = context.getResources().getDisplayMetrics().density;
        step = 5f * d;
    }

    public void setRunning(boolean r) {
        if (r == running) return;
        running = r;
        if (r) {
            if (hiddenAt != 0L) {
                grow(Math.min((System.currentTimeMillis() - hiddenAt) / 1000f, 1800f) * 0.6f);
                hiddenAt = 0L;
            }
            last = 0L;
            acc = 0f;
            invalidate();
            Choreographer.getInstance().postFrameCallback(this);
        } else {
            hiddenAt = System.currentTimeMillis();
            Choreographer.getInstance().removeFrameCallback(this);
        }
    }

    public void setShelves(List<RectF> f) {
        fractions = new ArrayList<>(f);
        rebuild();
        invalidate();
    }

    public void trigger(int type) {
        if (evType != EV_NONE || getWidth() == 0) return;
        startEvent(type);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        for (int i = 0; i < FLAKES; i++) resetFlake(i, true);
        rebuild();
    }

    private void rebuild() {
        shelves.clear();
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        float cap = CAP * d;
        for (RectF f : fractions) {
            Shelf s = new Shelf();
            s.r = new RectF(f.left * w, f.top * h, f.right * w, f.bottom * h);
            s.x0 = s.r.left - 8f * d;
            s.cols = (int) ((s.r.width() + 16f * d) / step) + 2;
            s.h = new float[s.cols];
            s.prof = new float[s.cols];
            float p1 = rnd.nextFloat() * 6.28f;
            float p2 = rnd.nextFloat() * 6.28f;
            float p3 = rnd.nextFloat() * 6.28f;
            for (int i = 0; i < s.cols; i++) {
                float t = i * step / d;
                float n = 0.5f + 0.25f * (float) Math.sin(t * 0.03f + p1) + 0.15f * (float) Math.sin(t * 0.08f + p2) + 0.1f * (float) Math.sin(t * 0.2f + p3);
                s.prof[i] = 0.45f + 0.55f * n;
                s.h[i] = 3f * d * s.prof[i];
            }
            for (int k = 0; k < 6; k++) {
                s.gx[k] = 0.08f + rnd.nextFloat() * 0.84f;
                s.gp[k] = rnd.nextFloat() * 6.28f;
            }
            s.shadow = new LinearGradient(0f, s.r.top, 0f, s.r.top + 9f * d, Color.argb(70, 15, 35, 80), Color.argb(0, 15, 35, 80), Shader.TileMode.CLAMP);
            s.front = new LinearGradient(0f, s.r.top - cap, 0f, s.r.top + 4f * d, Color.WHITE, Color.rgb(224, 235, 250), Shader.TileMode.CLAMP);
            s.back = new LinearGradient(0f, s.r.top - cap, 0f, s.r.top + 6f * d, Color.rgb(205, 222, 244), Color.rgb(160, 186, 224), Shader.TileMode.CLAMP);
            shelves.add(s);
        }
    }

    private void resetFlake(int i, boolean anywhere) {
        float r0;
        if (i < CRYSTALS) {
            ft[i] = 2;
            r0 = 3.4f + rnd.nextFloat() * 2.2f;
            fa[i] = 225;
        } else if (i < 60) {
            ft[i] = 1;
            r0 = 1.7f + rnd.nextFloat() * 1.3f;
            fa[i] = 190;
        } else {
            ft[i] = 0;
            r0 = 0.8f + rnd.nextFloat() * 0.9f;
            fa[i] = 130;
        }
        fr[i] = r0 * d;
        fvy[i] = (28f + r0 * 10f) * d;
        fx[i] = rnd.nextFloat() * Math.max(1, getWidth());
        fy[i] = anywhere ? rnd.nextFloat() * Math.max(1, getHeight()) : -rnd.nextFloat() * 40f;
        fph[i] = rnd.nextFloat() * 6.28f;
        fsp[i] = 0.6f + rnd.nextFloat() * 1.2f;
        fsw[i] = (6f + rnd.nextFloat() * 12f) * d;
        frot[i] = rnd.nextFloat() * 6.28f;
        frs[i] = (rnd.nextFloat() - 0.5f) * 1.2f;
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

    private float hAt(Shelf s, int i) {
        float tp = Math.min(i, s.cols - 1 - i) / 6f;
        if (tp > 1f) tp = 1f;
        if (tp < 0f) tp = 0f;
        tp = tp * tp * (3f - 2f * tp);
        return s.h[i] * tp;
    }

    private void grow(float secs) {
        float cap = CAP * d;
        for (int k = 0; k < shelves.size(); k++) {
            Shelf s = shelves.get(k);
            for (int i = 0; i < s.cols; i++) {
                float c = cap * s.prof[i];
                s.h[i] += secs * (cap / 420f) * s.prof[i];
                if (s.h[i] > c) s.h[i] = c;
            }
        }
    }

    private float fillFraction() {
        if (shelves.isEmpty()) return 0f;
        float tot = 0f;
        for (int k = 0; k < shelves.size(); k++) {
            Shelf s = shelves.get(k);
            float mx = 0f;
            for (int i = 0; i < s.cols; i++) if (s.h[i] > mx) mx = s.h[i];
            tot += mx / (CAP * d);
        }
        return tot / shelves.size();
    }

    private void addSpray(float x, float y, float strength) {
        if (sparks.size() > 160) return;
        Spark s = new Spark();
        s.x = x;
        s.y = y;
        s.vx = (40f + rnd.nextFloat() * 160f * strength) * d;
        s.vy = -(60f + rnd.nextFloat() * 160f * strength) * d;
        s.grav = 420f * d;
        s.max = 0.5f + rnd.nextFloat() * 0.6f;
        s.life = s.max;
        s.size = (1f + rnd.nextFloat() * 1.6f) * d;
        s.kind = 0;
        sparks.add(s);
    }

    private void addSmoke(float x, float y) {
        if (sparks.size() > 160) return;
        Spark s = new Spark();
        s.x = x;
        s.y = y;
        s.vx = -(10f + rnd.nextFloat() * 20f) * d;
        s.vy = -(14f + rnd.nextFloat() * 16f) * d;
        s.grav = 0f;
        s.max = 0.9f + rnd.nextFloat() * 0.5f;
        s.life = s.max;
        s.size = (2f + rnd.nextFloat() * 2f) * d;
        s.kind = 2;
        sparks.add(s);
    }

    private void addGold(float x, float y) {
        if (sparks.size() > 160) return;
        Spark s = new Spark();
        s.x = x;
        s.y = y;
        s.vx = -(8f + rnd.nextFloat() * 24f) * d;
        s.vy = (rnd.nextFloat() - 0.5f) * 24f * d;
        s.grav = 0f;
        s.max = 0.9f + rnd.nextFloat() * 0.8f;
        s.life = s.max;
        s.size = (1.8f + rnd.nextFloat() * 2f) * d;
        s.kind = 1;
        sparks.add(s);
    }

    private void scheduleNext() {
        nextEvent = time + 18f + rnd.nextFloat() * 40f;
    }

    private void endEvent() {
        evType = EV_NONE;
        scheduleNext();
    }

    private boolean startEvent(int type) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0) return false;
        if (type == EV_SANTA) {
            santaBaseY = h * (0.12f + rnd.nextFloat() * 0.2f);
            evDur = 13f;
        } else if (type == EV_GUST) {
            evDur = 5f;
        } else if (type == EV_PENGUIN) {
            ArrayList<Integer> ok = new ArrayList<>();
            for (int i = 0; i < shelves.size(); i++) if (shelves.get(i).r.width() >= 90f * d) ok.add(i);
            if (ok.isEmpty()) return false;
            evShelf = ok.get(rnd.nextInt(ok.size()));
            RectF r = shelves.get(evShelf).r;
            evX = r.left + (0.2f + 0.6f * rnd.nextFloat()) * r.width();
            evDur = 7f;
        } else if (type == EV_PLOW) {
            if (shelves.isEmpty()) return false;
            evShelf = 0;
            evX = shelves.get(0).r.left - 90f * d;
            evDur = 1000f;
        } else {
            return false;
        }
        evType = type;
        evT = 0f;
        lastEv = type;
        return true;
    }

    private void pickEvent() {
        boolean has = !shelves.isEmpty();
        int[] pool = new int[12];
        int n = 0;
        for (int k = 0; k < 3; k++) pool[n++] = EV_SANTA;
        for (int k = 0; k < 2; k++) pool[n++] = EV_GUST;
        if (has) {
            for (int k = 0; k < 3; k++) pool[n++] = EV_PENGUIN;
            if (fillFraction() > 0.5f) for (int k = 0; k < 2; k++) pool[n++] = EV_PLOW;
        }
        int pick = pool[rnd.nextInt(n)];
        if (pick == lastEv) pick = pool[rnd.nextInt(n)];
        if (!startEvent(pick)) scheduleNext();
    }

    private void updatePlow(float dt) {
        if (evShelf >= shelves.size()) {
            endEvent();
            return;
        }
        Shelf s = shelves.get(evShelf);
        evX += 270f * d * dt;
        float blade = evX + 44f * d;
        for (int i = 0; i < s.cols; i++) {
            float x = s.x0 + i * step;
            if (x < blade && s.h[i] > 0f) {
                if (s.h[i] > 0.8f * d && rnd.nextFloat() < 0.2f) addSpray(x, s.r.top - s.h[i], 1f);
                s.h[i] = 0f;
            }
        }
        addSpray(blade, s.r.top - 4f * d, 1.2f);
        if (rnd.nextFloat() < 0.15f) addSmoke(evX - 30f * d, s.r.top - 30f * d);
        if (evX > s.r.right + 70f * d) {
            evShelf++;
            if (evShelf >= shelves.size()) endEvent();
            else evX = shelves.get(evShelf).r.left - 90f * d;
        }
    }

    private void updateSanta(int w, int h) {
        float p = evT / evDur;
        if (p >= 1f) {
            endEvent();
            return;
        }
        float span = w + 460f * d;
        float amp = h * 0.04f;
        santaX = -260f * d + p * span;
        santaY = santaBaseY + (float) Math.sin(p * 6.9f) * amp;
        float dydp = (float) Math.cos(p * 6.9f) * 6.9f * amp;
        santaAng = (float) (Math.atan2(dydp, span) * 180.0 / Math.PI);
        for (int k = 0; k < 2; k++) {
            addGold(santaX - 60f * d + rnd.nextFloat() * 28f * d, santaY + (rnd.nextFloat() - 0.5f) * 34f * d);
        }
    }

    private void advance(float dt) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        time += dt;

        float env = 0f;
        if (evType == EV_GUST) env = (float) Math.sin(Math.PI * Math.min(1f, evT / evDur));
        float wind = (float) Math.sin(time * 0.15f) * 10f * d + env * 150f * d;

        for (int i = 0; i < FLAKES; i++) {
            float py = fy[i];
            fph[i] += dt * fsp[i];
            frot[i] += dt * frs[i];
            fx[i] += ((float) Math.sin(fph[i]) * fsw[i] + wind) * dt;
            fy[i] += fvy[i] * dt;
            if (fy[i] > h + 10f) {
                resetFlake(i, false);
                continue;
            }
            if (fx[i] < -20f) fx[i] += w + 40f;
            else if (fx[i] > w + 20f) fx[i] -= w + 40f;

            for (int k = 0; k < shelves.size(); k++) {
                Shelf s = shelves.get(k);
                if (fx[i] < s.r.left || fx[i] > s.r.right) continue;
                int col = (int) ((fx[i] - s.x0) / step);
                if (col < 0 || col >= s.cols) continue;
                float top = s.r.top - hAt(s, col) * 0.9f;
                if (py < top && fy[i] >= top) {
                    if (rnd.nextFloat() < 0.12f) addSpray(fx[i], top, 0.3f);
                    resetFlake(i, false);
                    break;
                }
            }
        }

        grow(dt);

        if (evType == EV_NONE) {
            if (time >= nextEvent) pickEvent();
        } else {
            evT += dt;
            if (evType == EV_SANTA) {
                updateSanta(w, h);
            } else if (evType == EV_PENGUIN) {
                if (evT > evDur) endEvent();
            } else if (evType == EV_PLOW) {
                updatePlow(dt);
            } else if (evType == EV_GUST) {
                if (!shelves.isEmpty() && rnd.nextFloat() < 0.4f) {
                    Shelf s = shelves.get(rnd.nextInt(shelves.size()));
                    float gx = s.r.left + rnd.nextFloat() * s.r.width();
                    int col = Math.max(0, Math.min(s.cols - 1, (int) ((gx - s.x0) / step)));
                    addSpray(gx, s.r.top - hAt(s, col), 0.8f);
                }
                if (evT > evDur) endEvent();
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
            if (s.kind == 2) s.size += 9f * d * dt;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (getWidth() == 0) return;
        for (int i = 0; i < shelves.size(); i++) drawShelf(c, shelves.get(i), i);
        drawFlakes(c);
        if (evType == EV_PLOW) drawPlow(c);
        drawSparks(c);
        if (evType == EV_SANTA) drawSanta(c);
    }

    private void trace(Shelf s, float hs, float yo) {
        pileTop.reset();
        float px = s.x0;
        float py = s.r.top - hAt(s, 0) * hs + yo;
        pileTop.moveTo(px, py);
        for (int i = 1; i < s.cols; i++) {
            float x = s.x0 + i * step;
            float y = s.r.top - hAt(s, i) * hs + yo;
            pileTop.quadTo(px, py, (px + x) / 2f, (py + y) / 2f);
            px = x;
            py = y;
        }
        pileTop.lineTo(px, py);
    }

    private void closeFill(Shelf s, float yo) {
        pileFill.set(pileTop);
        float base = s.r.top + 4f * d + yo;
        pileFill.lineTo(s.x0 + (s.cols - 1) * step, base);
        pileFill.lineTo(s.x0, base);
        pileFill.close();
    }

    private void drawShelf(Canvas c, Shelf s, int idx) {
        float mx = 0f;
        for (int i = 0; i < s.cols; i++) if (s.h[i] > mx) mx = s.h[i];
        boolean pile = mx >= 0.8f * d;

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(s.shadow);
        c.drawRect(s.r.left, s.r.top, s.r.right, s.r.top + 9f * d, paint);
        paint.setShader(null);

        if (pile) {
            trace(s, 1f, 0f);
            closeFill(s, 2f * d);
            paint.setShader(s.back);
            c.drawPath(pileFill, paint);
            paint.setShader(null);
        }
        if (evType == EV_PENGUIN && evShelf == idx) drawPenguin(c, s);
        if (!pile) return;

        trace(s, 0.92f, 0f);
        closeFill(s, 0f);
        paint.setShader(s.front);
        c.drawPath(pileFill, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * d);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.argb(235, 255, 255, 255));
        c.drawPath(pileTop, paint);
        paint.setStyle(Paint.Style.FILL);

        for (int k = 0; k < 6; k++) {
            float a = (float) Math.sin(time * 2.4f + s.gp[k]);
            if (a < 0.82f) continue;
            float gx = s.r.left + s.gx[k] * s.r.width();
            int col = (int) ((gx - s.x0) / step);
            if (col < 0 || col >= s.cols) continue;
            star(c, gx, s.r.top - hAt(s, col) * 0.92f - d, 3.2f * d, (a - 0.82f) / 0.18f, 0xFFFFFF);
        }
    }

    private int al(int rgb, float a) {
        return (((int) Math.max(0f, Math.min(255f, a))) << 24) | (rgb & 0xFFFFFF);
    }

    private void star(Canvas c, float x, float y, float s, float a, int rgb) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(1.3f * d);
        paint.setColor(al(rgb, 220f * a));
        c.drawLine(x - s, y, x + s, y, paint);
        c.drawLine(x, y - s, x, y + s, paint);
        paint.setStrokeWidth(d);
        paint.setColor(al(rgb, 150f * a));
        c.drawLine(x - s * 0.6f, y - s * 0.6f, x + s * 0.6f, y + s * 0.6f, paint);
        c.drawLine(x - s * 0.6f, y + s * 0.6f, x + s * 0.6f, y - s * 0.6f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(al(rgb, 255f * a));
        c.drawCircle(x, y, s * 0.22f, paint);
    }

    private void drawFlakes(Canvas c) {
        for (int i = 0; i < FLAKES; i++) {
            float x = fx[i];
            float y = fy[i];
            float r = fr[i];
            if (ft[i] == 2) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(28, 255, 255, 255));
                c.drawCircle(x, y, r * 1.9f, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(Math.max(1f, r * 0.17f));
                paint.setColor(Color.argb(fa[i], 255, 255, 255));
                for (int k = 0; k < 3; k++) {
                    float a = frot[i] + k * 1.0472f;
                    float dx = (float) Math.cos(a) * r;
                    float dy = (float) Math.sin(a) * r;
                    c.drawLine(x - dx, y - dy, x + dx, y + dy, paint);
                    for (int sg = -1; sg <= 1; sg += 2) {
                        float bx = x + sg * dx * 0.62f;
                        float by = y + sg * dy * 0.62f;
                        float ex = sg * dx * 0.3f;
                        float ey = sg * dy * 0.3f;
                        float qx = -dy * 0.32f;
                        float qy = dx * 0.32f;
                        c.drawLine(bx, by, bx + ex + qx, by + ey + qy, paint);
                        c.drawLine(bx, by, bx + ex - qx, by + ey - qy, paint);
                    }
                }
                paint.setStyle(Paint.Style.FILL);
                c.drawCircle(x, y, r * 0.16f, paint);
            } else {
                paint.setStyle(Paint.Style.FILL);
                if (ft[i] == 1) {
                    paint.setColor(Color.argb(34, 255, 255, 255));
                    c.drawCircle(x, y, r * 2.2f, paint);
                }
                paint.setColor(Color.argb(fa[i], 255, 255, 255));
                c.drawCircle(x, y, r, paint);
            }
        }
    }

    private void drawSparks(Canvas c) {
        for (int i = 0; i < sparks.size(); i++) {
            Spark s = sparks.get(i);
            float a = Math.min(1f, s.life / s.max);
            if (s.kind == 1) {
                star(c, s.x, s.y, s.size, a, 0xFFE27A);
            } else if (s.kind == 2) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(al(0xB8C4D6, 90f * a));
                c.drawCircle(s.x, s.y, s.size, paint);
            } else {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(al(0xFFFFFF, 235f * a));
                c.drawCircle(s.x, s.y, s.size, paint);
            }
        }
    }

    private void poly(Canvas c, int color, float... p) {
        ap.reset();
        ap.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) ap.lineTo(p[i], p[i + 1]);
        ap.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawPath(ap, paint);
    }

    private void oval(Canvas c, int color, float l, float t, float r, float b) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawOval(l, t, r, b, paint);
    }

    private void ovalS(Canvas c, Shader sh, float l, float t, float r, float b) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setShader(sh);
        c.drawOval(l, t, r, b, paint);
        paint.setShader(null);
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

    private void wheel(Canvas c, float cx, float cy, float r, float ang) {
        oval(c, 0xFF1E2128, cx - r, cy - r, cx + r, cy + r);
        oval(c, 0xFFC4CAD4, cx - r * 0.55f, cy - r * 0.55f, cx + r * 0.55f, cy + r * 0.55f);
        for (int k = 0; k < 3; k++) {
            float a = ang + k * 1.0472f;
            line(c, 0xFF7C8494, 1.4f, cx - (float) Math.cos(a) * r * 0.5f, cy - (float) Math.sin(a) * r * 0.5f, cx + (float) Math.cos(a) * r * 0.5f, cy + (float) Math.sin(a) * r * 0.5f);
        }
    }

    private void drawPlow(Canvas c) {
        if (evShelf >= shelves.size()) return;
        Shelf s = shelves.get(evShelf);
        float prog = Math.max(0f, Math.min(1f, (evX - s.r.left) / Math.max(1f, s.r.width())));
        c.save();
        c.translate(evX, s.r.top);
        c.scale(d * 0.72f, d * 0.72f);

        poly(c, Color.argb(42, 255, 240, 170), 34f, -22f, 170f, -46f, 170f, -2f);
        oval(c, Color.argb(60, 0, 0, 0), -48f, -3f, 70f, 3f);
        poly(c, 0xFF2B2F38, -46f, -13f, 36f, -13f, 36f, -6f, -46f, -6f);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(gTruck);
        c.drawRoundRect(-46f, -32f, 34f, -12f, 6f, 6f, paint);
        paint.setShader(null);
        poly(c, 0xFFB22428, -6f, -32f, -1f, -50f, 24f, -50f, 33f, -32f);
        poly(c, 0xFFB9E0F7, 0f, -47f, 20f, -47f, 27f, -34f, -2f, -34f);
        poly(c, Color.argb(120, 255, 255, 255), 2f, -46f, 8f, -46f, 4f, -35f, -1f, -35f);
        poly(c, 0xFF2B2F38, -50f, -26f, -46f, -26f, -46f, -14f, -50f, -14f);
        oval(c, 0xFFFFF2B0, 31f, -29f, 38f, -23f);

        boolean blink = ((int) (time * 5f) % 2) == 0;
        poly(c, 0xFF2B2F38, 4f, -53f, 18f, -53f, 18f, -50f, 4f, -50f);
        oval(c, blink ? 0xFFFFAA1E : 0xFF7A5014, 5f, -57f, 11f, -52f);
        oval(c, blink ? 0xFF3D8BFF : 0xFF1B3A70, 12f, -57f, 18f, -52f);

        float ang = evX / (11f * d * 0.72f);
        wheel(c, -28f, -10f, 10f, ang);
        wheel(c, 14f, -10f, 10f, ang);

        line(c, 0xFF464A52, 3.4f, 32f, -18f, 44f, -18f);
        ap.reset();
        ap.moveTo(42f, -36f);
        ap.lineTo(52f, -38f);
        ap.quadTo(66f, -20f, 64f, -2f);
        ap.lineTo(54f, -2f);
        ap.quadTo(44f, -4f, 42f, -8f);
        ap.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(gBlade);
        c.drawPath(ap, paint);
        paint.setShader(null);
        line(c, 0xFF2B2F38, 2.4f, 47f, -32f, 57f, -24f);
        line(c, 0xFF2B2F38, 2.4f, 48f, -22f, 59f, -14f);
        line(c, 0xFF2B2F38, 2.4f, 49f, -12f, 59f, -6f);
        float m = 5f + 10f * prog;
        oval(c, 0xFFF2F7FD, 56f, -m * 1.1f, 56f + m * 1.8f, 1f);
        oval(c, 0xFFD6E4F6, 56f + m * 0.3f, -m * 0.6f, 56f + m * 1.6f, 1f);
        c.restore();
    }

    private void leg(Canvas c, float hx, float ph) {
        float kx = hx + (float) Math.sin(ph) * 5f;
        float ky = -6f;
        float fx2 = kx + (float) Math.sin(ph - 0.9f) * 7f;
        float fy2 = -1f - (float) Math.max(0.0, Math.cos(ph)) * 4f;
        line(c, 0xFF4A2F1E, 3.4f, hx, -16f, kx, ky);
        line(c, 0xFF4A2F1E, 2.8f, kx, ky, fx2, fy2);
        oval(c, 0xFF2A1A10, fx2 - 2f, fy2 - 1.5f, fx2 + 3f, fy2 + 1.5f);
    }

    private void drawReindeer(Canvas c, float cx, boolean lead) {
        float ph = time * 10f + cx * 0.1f;
        c.save();
        c.translate(0f, (float) Math.sin(ph) * 1.6f);
        leg(c, cx - 12f, ph);
        leg(c, cx - 5f, ph + 2.2f);
        leg(c, cx + 11f, ph + 3.1f);
        leg(c, cx + 17f, ph + 5.2f);
        ovalS(c, gDeer, cx - 20f, -33f, cx + 23f, -12f);
        oval(c, 0xFFD9B99B, cx - 12f, -20f, cx + 14f, -12f);
        poly(c, 0xFF7A4E30, cx + 12f, -28f, cx + 22f, -47f, cx + 31f, -44f, cx + 25f, -22f);
        oval(c, 0xFF7A4E30, cx + 23f, -55f, cx + 40f, -42f);
        oval(c, 0xFFC9A27E, cx + 35f, -52f, cx + 47f, -43f);
        poly(c, 0xFF5E3B26, cx + 27f, -54f, cx + 22f, -63f, cx + 32f, -57f);
        oval(c, Color.WHITE, cx + 31f, -52f, cx + 35f, -48f);
        oval(c, 0xFF1A1008, cx + 32.5f, -51f, cx + 35f, -48.5f);
        oval(c, 0xFFF5F0E8, cx - 25f, -31f, cx - 17f, -23f);
        int ant = 0xFFD8BC86;
        line(c, ant, 2f, cx + 30f, -54f, cx + 26f, -68f);
        line(c, ant, 2f, cx + 26f, -68f, cx + 30f, -78f);
        line(c, ant, 1.6f, cx + 26f, -68f, cx + 19f, -70f);
        line(c, ant, 1.6f, cx + 27f, -62f, cx + 20f, -61f);
        line(c, ant, 2f, cx + 35f, -54f, cx + 39f, -68f);
        line(c, ant, 2f, cx + 39f, -68f, cx + 36f, -78f);
        line(c, ant, 1.6f, cx + 39f, -68f, cx + 46f, -70f);
        line(c, 0xFFC8242A, 3f, cx + 8f, -31f, cx + 10f, -13f);
        line(c, 0xFFC8242A, 3f, cx + 17f, -42f, cx + 24f, -33f);
        oval(c, 0xFFF2C14E, cx + 7f, -15f, cx + 13f, -9f);
        oval(c, 0xFFFFE9A0, cx + 8f, -14f, cx + 10f, -12f);
        if (lead) {
            oval(c, Color.argb(36, 255, 60, 60), cx + 28f, -56f, cx + 52f, -34f);
            oval(c, Color.argb(80, 255, 60, 60), cx + 32f, -52f, cx + 48f, -38f);
            oval(c, 0xFFEB2828, cx + 38f, -49f, cx + 45f, -43f);
        }
        c.restore();
    }

    private void drawSanta(Canvas c) {
        float wv = (float) Math.sin(time * 7f);
        c.save();
        c.translate(santaX, santaY);
        c.rotate(santaAng);
        c.scale(d * 0.62f, d * 0.62f);

        oval(c, Color.argb(20, 255, 210, 120), -90f, -110f, 110f, 30f);
        oval(c, Color.argb(24, 255, 220, 140), -60f, -90f, 80f, 14f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f);
        paint.setColor(Color.argb(230, 205, 170, 110));
        ap.reset();
        ap.moveTo(38f, -16f);
        ap.quadTo(80f, -4f, 120f, -18f);
        ap.quadTo(160f, -6f, 200f, -20f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        c.drawPath(ap, paint);
        paint.setStyle(Paint.Style.FILL);

        drawReindeer(c, 120f, false);
        drawReindeer(c, 200f, true);

        oval(c, 0xFF96623A, -50f, -44f, -18f, -14f);
        oval(c, 0xFFB5824F, -46f, -42f, -24f, -26f);
        line(c, 0xFFE8B83C, 2f, -40f, -30f, -28f, -30f);
        poly(c, 0xFF3F7FD6, -44f, -38f, -30f, -38f, -30f, -26f, -44f, -26f);
        line(c, 0xFFF2C14E, 2f, -37f, -38f, -37f, -26f);
        poly(c, 0xFF2E9E5B, -32f, -46f, -18f, -46f, -18f, -26f, -32f, -26f);
        line(c, 0xFFF2C14E, 2f, -25f, -46f, -25f, -26f);
        line(c, 0xFFF2C14E, 2f, -32f, -37f, -18f, -37f);

        ap.reset();
        ap.moveTo(-40f, -26f);
        ap.lineTo(30f, -26f);
        ap.quadTo(44f, -28f, 47f, -46f);
        ap.quadTo(53f, -30f, 47f, -14f);
        ap.quadTo(40f, -3f, 26f, -3f);
        ap.lineTo(-26f, -3f);
        ap.quadTo(-42f, -6f, -43f, -17f);
        ap.quadTo(-43f, -23f, -40f, -26f);
        ap.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(gSleigh);
        c.drawPath(ap, paint);
        paint.setShader(null);
        line(c, 0xFFF2C14E, 2.6f, -38f, -14f, 40f, -14f);
        for (int k = 0; k < 5; k++) oval(c, 0xFFFFE08A, -32f + k * 14f - 1.8f, -21.8f, -32f + k * 14f + 1.8f, -18.2f);

        int gold = 0xFFE8B83C;
        ap.reset();
        ap.moveTo(-52f, 6f);
        ap.lineTo(30f, 6f);
        ap.quadTo(54f, 6f, 54f, -12f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.4f);
        paint.setColor(gold);
        c.drawPath(ap, paint);
        paint.setStrokeWidth(1.2f);
        paint.setColor(0xFFFFF0B0);
        ap.offset(0f, -1.2f);
        c.drawPath(ap, paint);
        paint.setStyle(Paint.Style.FILL);
        line(c, gold, 3f, -24f, -4f, -24f, 6f);
        line(c, gold, 3f, 16f, -4f, 16f, 6f);

        ovalS(c, gCoat, -16f, -54f, 10f, -22f);
        poly(c, 0xFF20242C, -16f, -38f, 10f, -38f, 10f, -34f, -16f, -34f);
        poly(c, 0xFFF2C14E, -4f, -39f, 2f, -39f, 2f, -33f, -4f, -33f);

        oval(c, 0xFFFFD2AE, -12f, -66f, 6f, -48f);
        oval(c, 0xFFFAFAFA, -14f, -58f, 8f, -44f);
        oval(c, 0xFFFAFAFA, -15f, -52f, 6f, -40f);
        oval(c, 0xFFFAFAFA, -9f, -49f, 3f, -38f);
        oval(c, 0xFFFAFAFA, -10f, -58f, -2f, -53f);
        oval(c, 0xFFFAFAFA, -2f, -58f, 6f, -53f);
        oval(c, 0xFFF29A9A, -5f, -58f, -1f, -54f);
        oval(c, Color.argb(120, 255, 110, 130), -11f, -57f, -6f, -53f);
        oval(c, Color.argb(120, 255, 110, 130), 1f, -57f, 6f, -53f);
        oval(c, 0xFF2A1A10, -8f, -62f, -5.4f, -59.4f);
        oval(c, 0xFF2A1A10, -1f, -62f, 1.6f, -59.4f);

        ap.reset();
        ap.moveTo(-14f, -64f);
        ap.lineTo(8f, -64f);
        ap.quadTo(6f, -78f, -6f, -86f);
        ap.quadTo(-12f, -76f, -14f, -64f);
        ap.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFD6303A);
        c.drawPath(ap, paint);
        oval(c, 0xFFFAFAFA, -16f, -69f, 10f, -61f);
        oval(c, Color.argb(60, 255, 255, 255), -14f, -92f, 0f, -78f);
        oval(c, 0xFFFAFAFA, -12f, -90f, -2f, -80f);

        c.save();
        c.translate(4f, -44f);
        c.rotate(-40f + 22f * wv);
        line(c, 0xFFC8242A, 6f, 0f, 0f, 16f, -4f);
        oval(c, 0xFFFAFAFA, 12f, -10f, 22f, 0f);
        c.restore();

        c.restore();
    }

    private void drawPenguin(Canvas c, Shelf s) {
        float t = evT;
        float e;
        if (t < 0.9f) e = t / 0.9f;
        else if (t > evDur - 0.9f) e = (evDur - t) / 0.9f;
        else e = 1f;
        e = Math.max(0f, Math.min(1f, e));
        e = e * e * (3f - 2f * e);
        int col = Math.max(0, Math.min(s.cols - 1, (int) ((evX - s.x0) / step)));
        float baseY = s.r.top - hAt(s, col) * 0.85f;
        float look = (float) Math.sin(t * 1.7f);
        float wave = (t > 1.4f && t < evDur - 1.2f) ? (float) Math.sin(t * 10f) : 0f;

        c.save();
        c.translate(evX, baseY + (1f - e) * 52f * d);
        c.scale(d * 0.95f, d * 0.95f);

        oval(c, 0xFFFFA726, -11f, -3f, -2f, 1.5f);
        oval(c, 0xFFFFA726, 2f, -3f, 11f, 1.5f);

        c.save();
        c.translate(-13f, -32f);
        c.rotate(18f);
        oval(c, 0xFF1F2A3C, -3.2f, 0f, 3.2f, 15f);
        c.restore();
        c.save();
        c.translate(13f, -32f);
        c.rotate(-(40f + 22f * wave));
        oval(c, 0xFF1F2A3C, -3.2f, 0f, 3.2f, 15f);
        c.restore();

        ovalS(c, gPeng, -14f, -50f, 14f, -2f);
        oval(c, 0xFFF3F7FB, -9.5f, -38f, 9.5f, -4f);
        oval(c, 0xFFD6303A, -12.5f, -37f, 12.5f, -32f);
        poly(c, 0xFFB5232D, 6f, -34f, 11f, -34f, 12f, -22f, 7f, -22f);

        oval(c, Color.WHITE, -8f, -47f, -2f, -41f);
        oval(c, Color.WHITE, 2f, -47f, 8f, -41f);
        oval(c, 0xFF111111, -6f + look, -45.5f, -3.2f + look, -42.7f);
        oval(c, 0xFF111111, 4f + look, -45.5f, 6.8f + look, -42.7f);
        oval(c, Color.argb(110, 255, 120, 140), -12f, -42f, -7f, -38f);
        oval(c, Color.argb(110, 255, 120, 140), 7f, -42f, 12f, -38f);
        poly(c, 0xFFFFA726, -3f, -41.5f, 3f, -41.5f, 0f, -37f);

        ap.reset();
        ap.moveTo(-10f, -48f);
        ap.lineTo(10f, -48f);
        ap.quadTo(9f, -60f, 3f, -66f);
        ap.quadTo(-5f, -58f, -10f, -48f);
        ap.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFD6303A);
        c.drawPath(ap, paint);
        oval(c, 0xFFFAFAFA, -11f, -52f, 11f, -45f);
        oval(c, 0xFFFAFAFA, -0.5f, -69f, 6.5f, -62f);
        c.restore();
    }
}
