package com.pvptarget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class TargetMenuScreen extends Screen {
    private static final int WHEEL_R = 50;
    private static final int CELL = 24;
    private static final int SLIDER_W = 135;

    private int left, top, gx, gy, wx, wy, sx0, sy0;
    private boolean dragWheel, dragBri;

    public TargetMenuScreen() {
        super(Text.literal("PvP Target"));
    }

    private Text label() {
        boolean on = Config.get().enabled;
        return Text.literal("PvP Target: " + (on ? "ON" : "OFF")).formatted(on ? Formatting.GREEN : Formatting.RED);
    }

    @Override
    protected void init() {
        left = width / 2 - 160;
        top = height / 2 - 92;
        gx = left + 5;
        gy = top + 52;
        wx = left + 242;
        wy = top + 104;
        sx0 = left + 175;
        sy0 = top + 160;

        addDrawableChild(ButtonWidget.builder(label(), b -> {
            Config cfg = Config.get();
            cfg.enabled = !cfg.enabled;
            b.setMessage(label());
            Config.save();
        }).dimensions(left + 5, top + 5, 310, 20).build());
    }

    private void panel(DrawContext c, int x, int y, int w, int h) {
        c.fill(x, y, x + w, y + h, 0xCC101018);
        c.drawBorder(x, y, w, h, 0xFF5A5A6E);
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
        super.renderBackground(c, mx, my, delta);
        panel(c, left, top, 320, 30);
        panel(c, left, top + 36, 155, 148);
        panel(c, left + 165, top + 36, 155, 148);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        Config cfg = Config.get();

        c.drawText(textRenderer, "Shape: " + ShapeRenderer.NAMES[cfg.shape], left + 8, top + 40, 0xFFFFFF, true);
        c.drawText(textRenderer, "Color", left + 173, top + 40, 0xFFFFFF, true);

        // grid cell backgrounds
        for (int i = 0; i < ShapeRenderer.COUNT; i++) {
            int x = gx + (i % 6) * CELL;
            int y = gy + (i / 6) * CELL;
            boolean hover = mx >= x && mx < x + CELL && my >= y && my < y + CELL;
            int bg = i == cfg.shape ? 0xFF2F5F2F : hover ? 0xFF3A3A4A : 0xFF22222C;
            c.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, bg);
            if (i == cfg.shape) c.drawBorder(x + 1, y + 1, CELL - 2, CELL - 2, 0xFF66FF66);
        }
        c.drawBorder(sx0 - 1, sy0 - 1, SLIDER_W + 2, 12, 0xFFAAAAAA);
        c.draw(); // flush before raw geometry

        Matrix4f m = c.getMatrices().peek().getPositionMatrix();
        int argb = 0xFF000000 | cfg.rgb();

        for (int i = 0; i < ShapeRenderer.COUNT; i++) {
            float x = gx + (i % 6) * CELL + CELL / 2f;
            float y = gy + (i / 6) * CELL + CELL / 2f;
            ShapeRenderer.draw(m, i, x, y, 9f, argb);
        }

        ShapeRenderer.wheel(m, wx, wy, WHEEL_R, cfg.bri);
        float ang = cfg.hue * (float) (2 * Math.PI);
        float mxk = wx + MathHelper.cos(ang) * cfg.sat * WHEEL_R;
        float myk = wy + MathHelper.sin(ang) * cfg.sat * WHEEL_R;
        ShapeRenderer.draw(m, 1, mxk, myk, 7f, 0xFF000000);
        ShapeRenderer.draw(m, 1, mxk, myk, 6f, 0xFFFFFFFF);

        int full = 0xFF000000 | net.minecraft.util.math.MathHelper.hsvToRgb(cfg.hue, cfg.sat, 1f);
        ShapeRenderer.gradient(m, sx0, sy0, sx0 + SLIDER_W, sy0 + 10, 0xFF000000, full);

        int bx = sx0 + Math.round(cfg.bri * SLIDER_W);
        c.fill(bx - 1, sy0 - 2, bx + 1, sy0 + 12, 0xFFFFFFFF);
    }

    private void applyWheel(double mx, double my) {
        double dx = mx - wx, dy = my - wy;
        double a = Math.atan2(dy, dx);
        if (a < 0) a += 2 * Math.PI;
        Config cfg = Config.get();
        cfg.hue = (float) (a / (2 * Math.PI));
        cfg.sat = (float) Math.min(1.0, Math.sqrt(dx * dx + dy * dy) / WHEEL_R);
    }

    private void applyBri(double mx) {
        Config.get().bri = MathHelper.clamp((float) ((mx - sx0) / SLIDER_W), 0f, 1f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;

        // shape grid
        int col = (int) Math.floor((mx - gx) / CELL);
        int row = (int) Math.floor((my - gy) / CELL);
        if (col >= 0 && col < 6 && row >= 0 && row < 5) {
            int i = row * 6 + col;
            if (i < ShapeRenderer.COUNT) {
                Config.get().shape = i;
                Config.save();
                return true;
            }
        }
        // wheel
        double dx = mx - wx, dy = my - wy;
        if (dx * dx + dy * dy <= (WHEEL_R + 2) * (WHEEL_R + 2)) {
            dragWheel = true;
            applyWheel(mx, my);
            return true;
        }
        // brightness slider
        if (mx >= sx0 - 3 && mx <= sx0 + SLIDER_W + 3 && my >= sy0 - 3 && my <= sy0 + 13) {
            dragBri = true;
            applyBri(mx);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragWheel) { applyWheel(mx, my); return true; }
        if (dragBri) { applyBri(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragWheel || dragBri) Config.save();
        dragWheel = false;
        dragBri = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (PvpTargetClient.menuKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        Config.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
