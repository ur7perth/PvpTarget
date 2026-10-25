package com.pvptarget;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

public class PvpTargetClient implements ClientModInitializer {
    public static KeyBinding menuKey;

    private static boolean visible;
    private static float sx, sy, dist;

    @Override
    public void onInitializeClient() {
        Config.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvptarget.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_0, "key.categories.pvptarget"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new TargetMenuScreen());
            }
        });

        // Compute the on-screen position of the target while world matrices are available
        WorldRenderEvents.END.register(ctx -> {
            visible = false;
            MinecraftClient mc = MinecraftClient.getInstance();
            Config cfg = Config.get();
            if (!cfg.enabled || mc.player == null || mc.currentScreen != null) return;
            if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
            if (!(hit.getEntity() instanceof PlayerEntity target)) return;
            if (target == mc.player || !target.isAlive() || target.isSpectator()) return;
            if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;

            float td = ctx.tickCounter().getTickDelta(false);
            Vec3d p = target.getLerpedPos(td).add(0, target.getHeight() * 0.5, 0);
            Vec3d cam = ctx.camera().getPos();

            Vector4f v = new Vector4f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z), 1f);
            Matrix4f view = ctx.matrixStack().peek().getPositionMatrix();
            view.transform(v);
            ctx.projectionMatrix().transform(v);
            if (v.w <= 0.01f) return;

            float nx = v.x / v.w;
            float ny = v.y / v.w;
            if (Math.abs(nx) > 1.2f || Math.abs(ny) > 1.2f) return;

            sx = (nx * 0.5f + 0.5f) * mc.getWindow().getScaledWidth();
            sy = (1f - (ny * 0.5f + 0.5f)) * mc.getWindow().getScaledHeight();
            dist = (float) cam.distanceTo(p);
            visible = true;
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!visible || mc.options.hudHidden) return;
            Config cfg = Config.get();
            float pulse = 1f + 0.1f * (float) Math.sin((System.currentTimeMillis() % 800L) / 800.0 * Math.PI * 2);
            float r = Math.max(8f, Math.min(30f, 130f / (dist + 2f))) * pulse;
            int argb = 0xE6000000 | cfg.rgb();
            ctx.draw();
            ShapeRenderer.draw(ctx.getMatrices().peek().getPositionMatrix(), cfg.shape, sx, sy, r, argb);
        });
    }
}
