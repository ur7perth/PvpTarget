package com.pvptarget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public final class ShapeRenderer {
    public static final int COUNT = 30;
    public static final String[] NAMES = {
            "Circle", "Ring", "Triangle", "Inv Triangle", "Square", "Diamond",
            "Pentagon", "Hexagon", "Octagon", "Star 5", "Star 4", "Star 6",
            "Star 8", "Plus", "X", "Crosshair", "Heart", "Arrow Down",
            "Arrow Up", "Chevron", "Corners", "Square Outline", "Diamond Outline", "Triangle Outline",
            "Bullseye", "Double Chevron", "Lightning", "Shield", "Dot Ring", "Sparkle"
    };

    private final BufferBuilder b;
    private final Matrix4f m;
    private final float cx, cy, r;
    private final int col;

    private ShapeRenderer(BufferBuilder b, Matrix4f m, float cx, float cy, float r, int col) {
        this.b = b; this.m = m; this.cx = cx; this.cy = cy; this.r = r; this.col = col;
    }

    // ---------- public API ----------

    public static void draw(Matrix4f m, int id, float cx, float cy, float r, int argb) {
        setup();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        new ShapeRenderer(b, m, cx, cy, r, argb).build(id);
        finish(b);
    }

    /** HSV color wheel: center = gray(bri), rim = pure hue. */
    public static void wheel(Matrix4f m, float cx, float cy, float R, float bri) {
        setup();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        int seg = 96;
        int center = 0xFF000000 | MathHelper.hsvToRgb(0f, 0f, bri);
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (i * 2 * Math.PI / seg);
            float a1 = (float) ((i + 1) * 2 * Math.PI / seg);
            int c0 = 0xFF000000 | MathHelper.hsvToRgb(a0 / (float) (2 * Math.PI), 1f, bri);
            int c1 = 0xFF000000 | MathHelper.hsvToRgb(a1 / (float) (2 * Math.PI), 1f, bri);
            b.vertex(m, cx, cy, 0).color(center);
            b.vertex(m, cx + MathHelper.cos(a0) * R, cy + MathHelper.sin(a0) * R, 0).color(c0);
            b.vertex(m, cx + MathHelper.cos(a1) * R, cy + MathHelper.sin(a1) * R, 0).color(c1);
        }
        finish(b);
    }

    /** Horizontal gradient rectangle. */
    public static void gradient(Matrix4f m, float x1, float y1, float x2, float y2, int left, int right) {
        setup();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        b.vertex(m, x1, y1, 0).color(left);
        b.vertex(m, x1, y2, 0).color(left);
        b.vertex(m, x2, y2, 0).color(right);
        b.vertex(m, x1, y1, 0).color(left);
        b.vertex(m, x2, y2, 0).color(right);
        b.vertex(m, x2, y1, 0).color(right);
        finish(b);
    }

    private static void setup() {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
    }

    private static void finish(BufferBuilder b) {
        BuiltBuffer bb = b.endNullable();
        if (bb != null) BufferRenderer.drawWithGlobalProgram(bb);
        RenderSystem.enableCull();
    }

    // ---------- primitives (unit coordinates -1..1, y down) ----------

    private void v(float x, float y) {
        b.vertex(m, cx + x * r, cy + y * r, 0f).color(col);
    }

    private void tri(float a, float bb, float c, float d, float e, float f) {
        v(a, bb); v(c, d); v(e, f);
    }

    private void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        tri(x1, y1, x2, y2, x3, y3);
        tri(x1, y1, x3, y3, x4, y4);
    }

    private void line(float x1, float y1, float x2, float y2, float t) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = -dy / len * t / 2f, ny = dx / len * t / 2f;
        quad(x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny);
    }

    private void fan(float[] p) {
        int n = p.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            tri(0, 0, p[2 * i], p[2 * i + 1], p[2 * j], p[2 * j + 1]);
        }
    }

    private void outline(float[] p, float t) {
        int n = p.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            line(p[2 * i], p[2 * i + 1], p[2 * j], p[2 * j + 1], t);
        }
    }

    private void ring(float ro, float ri) {
        int seg = 40;
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (i * 2 * Math.PI / seg), a1 = (float) ((i + 1) * 2 * Math.PI / seg);
            float c0 = MathHelper.cos(a0), s0 = MathHelper.sin(a0), c1 = MathHelper.cos(a1), s1 = MathHelper.sin(a1);
            quad(c0 * ro, s0 * ro, c1 * ro, s1 * ro, c1 * ri, s1 * ri, c0 * ri, s0 * ri);
        }
    }

    private void circle(float rad) {
        fan(poly(32, 0f, rad));
    }

    private static float[] poly(int n, float rot, float rad) {
        float[] p = new float[n * 2];
        for (int i = 0; i < n; i++) {
            float a = rot + (float) (i * 2 * Math.PI / n);
            p[2 * i] = MathHelper.cos(a) * rad;
            p[2 * i + 1] = MathHelper.sin(a) * rad;
        }
        return p;
    }

    private static float[] star(int n, float inner, float rot) {
        float[] p = new float[n * 4];
        for (int i = 0; i < n * 2; i++) {
            float a = rot + (float) (i * Math.PI / n);
            float rad = (i % 2 == 0) ? 1f : inner;
            p[2 * i] = MathHelper.cos(a) * rad;
            p[2 * i + 1] = MathHelper.sin(a) * rad;
        }
        return p;
    }

    private static final float UP = (float) (-Math.PI / 2);
    private static final float DOWN = (float) (Math.PI / 2);

    // ---------- the 30 shapes ----------

    private void build(int id) {
        switch (id) {
            case 0 -> circle(0.9f);
            case 1 -> ring(0.9f, 0.62f);
            case 2 -> fan(poly(3, UP, 0.95f));
            case 3 -> fan(poly(3, DOWN, 0.95f));
            case 4 -> quad(-0.75f, -0.75f, 0.75f, -0.75f, 0.75f, 0.75f, -0.75f, 0.75f);
            case 5 -> fan(poly(4, UP, 0.95f));
            case 6 -> fan(poly(5, UP, 0.95f));
            case 7 -> fan(poly(6, 0f, 0.95f));
            case 8 -> fan(poly(8, (float) (Math.PI / 8), 0.95f));
            case 9 -> fan(star(5, 0.4f, UP));
            case 10 -> fan(star(4, 0.38f, UP));
            case 11 -> fan(star(6, 0.55f, UP));
            case 12 -> fan(star(8, 0.55f, UP));
            case 13 -> {
                quad(-0.2f, -0.9f, 0.2f, -0.9f, 0.2f, 0.9f, -0.2f, 0.9f);
                quad(-0.9f, -0.2f, 0.9f, -0.2f, 0.9f, 0.2f, -0.9f, 0.2f);
            }
            case 14 -> {
                line(-0.8f, -0.8f, 0.8f, 0.8f, 0.35f);
                line(-0.8f, 0.8f, 0.8f, -0.8f, 0.35f);
            }
            case 15 -> {
                ring(0.6f, 0.48f);
                line(0, -1f, 0, -0.35f, 0.12f);
                line(0, 1f, 0, 0.35f, 0.12f);
                line(-1f, 0, -0.35f, 0, 0.12f);
                line(1f, 0, 0.35f, 0, 0.12f);
                circle(0.1f);
            }
            case 16 -> {
                int n = 48;
                float[] p = new float[n * 2];
                for (int i = 0; i < n; i++) {
                    double t = i * 2 * Math.PI / n;
                    double x = 16 * Math.pow(Math.sin(t), 3);
                    double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
                    p[2 * i] = (float) (x / 17.0);
                    p[2 * i + 1] = (float) (-y / 17.0 + 0.05);
                }
                fan(p);
            }
            case 17 -> {
                quad(-0.22f, -0.9f, 0.22f, -0.9f, 0.22f, 0.15f, -0.22f, 0.15f);
                tri(-0.75f, 0.1f, 0.75f, 0.1f, 0f, 0.95f);
            }
            case 18 -> {
                quad(-0.22f, 0.9f, 0.22f, 0.9f, 0.22f, -0.15f, -0.22f, -0.15f);
                tri(-0.75f, -0.1f, 0.75f, -0.1f, 0f, -0.95f);
            }
            case 19 -> {
                line(-0.8f, -0.4f, 0f, 0.4f, 0.3f);
                line(0f, 0.4f, 0.8f, -0.4f, 0.3f);
            }
            case 20 -> {
                for (int sxn = -1; sxn <= 1; sxn += 2) {
                    for (int syn = -1; syn <= 1; syn += 2) {
                        line(sxn * 0.9f, syn * 0.9f, sxn * 0.9f, syn * 0.4f, 0.18f);
                        line(sxn * 0.99f, syn * 0.9f, sxn * 0.4f, syn * 0.9f, 0.18f);
                    }
                }
            }
            case 21 -> outline(poly(4, (float) (Math.PI / 4), 1.0f), 0.2f);
            case 22 -> outline(poly(4, UP, 0.9f), 0.2f);
            case 23 -> outline(poly(3, UP, 0.95f), 0.2f);
            case 24 -> {
                ring(0.95f, 0.8f);
                ring(0.6f, 0.45f);
                circle(0.25f);
            }
            case 25 -> {
                line(-0.8f, -0.7f, 0f, -0.1f, 0.25f);
                line(0f, -0.1f, 0.8f, -0.7f, 0.25f);
                line(-0.8f, 0f, 0f, 0.6f, 0.25f);
                line(0f, 0.6f, 0.8f, 0f, 0.25f);
            }
            case 26 -> {
                tri(0.3f, -1f, -0.4f, 0.1f, 0.1f, 0.1f);
                tri(-0.1f, -0.1f, 0.45f, -0.1f, -0.25f, 1f);
                quad(0.3f, -1f, 0.1f, 0.1f, -0.4f, 0.1f, 0.1f, -0.6f);
            }
            case 27 -> fan(new float[]{-0.75f, -0.8f, 0.75f, -0.8f, 0.75f, 0.1f, 0f, 0.95f, -0.75f, 0.1f});
            case 28 -> {
                ring(0.9f, 0.7f);
                circle(0.35f);
            }
            case 29 -> {
                fan(star(4, 0.15f, UP));
                circle(0.18f);
            }
            default -> circle(0.9f);
        }
    }
                  }
