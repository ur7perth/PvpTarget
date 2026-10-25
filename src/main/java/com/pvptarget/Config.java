package com.pvptarget;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pvptarget.json");
    private static Config inst;

    public boolean enabled = true;
    public int shape = 9;
    public float hue = 0f;
    public float sat = 1f;
    public float bri = 1f;

    public static Config get() {
        if (inst == null) load();
        return inst;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                inst = GSON.fromJson(Files.readString(FILE), Config.class);
            }
        } catch (Exception ignored) {
        }
        if (inst == null) inst = new Config();
        inst.shape = MathHelper.clamp(inst.shape, 0, ShapeRenderer.COUNT - 1);
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(get()));
        } catch (Exception ignored) {
        }
    }

    /** 0xRRGGBB */
    public int rgb() {
        return MathHelper.hsvToRgb(hue, sat, bri);
    }
}
