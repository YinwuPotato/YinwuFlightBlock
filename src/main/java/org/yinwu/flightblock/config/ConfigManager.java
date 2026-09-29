package org.yinwu.flightblock.config;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.yinwu.flightblock.YinwuFlightBlockPlugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConfigManager {

    private final YinwuFlightBlockPlugin plugin;
    private final ConcurrentMap<String, Object> cache = new ConcurrentHashMap<>();
    private volatile FileConfiguration config;

    public ConfigManager(YinwuFlightBlockPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        cache.clear();
        cache.put("enabled", config.getBoolean("enabled", true));
        cache.put("block-material", parseMaterial());
        cache.put("range", config.getInt("range", 32));
        cache.put("check-interval", Math.max(1, config.getInt("check-interval", 20)));
        cache.put("outline-interval", Math.max(1, config.getInt("outline-interval", 20)));
        cache.put("outline-step", Math.max(1, config.getInt("outline-step", 1)));
        cache.put("outline-particle", parseParticle());
        cache.put("outline-particle-size", (float) Math.max(0.1, config.getDouble("outline-particle-size", 1.5)));
        cache.put("outline-particle-color", parseColor());
        cache.put("recipe-enabled", config.getBoolean("recipe-enabled", true));
    }

    private Material parseMaterial() {
        String name = config.getString("block-material", "LIGHT_BLUE_STAINED_GLASS");
        Material material = Material.matchMaterial(name);
        if (material == null || !material.isBlock()) {
            plugin.getLogger().warning("无效的 block-material: " + name + "，回退到 LIGHT_BLUE_STAINED_GLASS");
            return Material.LIGHT_BLUE_STAINED_GLASS;
        }
        return material;
    }

    private Particle parseParticle() {
        String name = config.getString("outline-particle", "DUST");
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的 outline-particle: " + name + "，回退到 DUST");
            return Particle.DUST;
        }
    }

    private Color parseColor() {
        String hex = config.getString("outline-particle-color", "87CEFA").replace("#", "");
        try {
            return Color.fromRGB(Integer.parseInt(hex, 16));
        } catch (Exception e) {
            plugin.getLogger().warning("无效的 outline-particle-color: " + hex + "，回退到浅蓝");
            return Color.fromRGB(0x87, 0xCE, 0xFA);
        }
    }

    public boolean enabled() {
        return (Boolean) cache.getOrDefault("enabled", true);
    }

    public Material blockMaterial() {
        return (Material) cache.get("block-material");
    }

    public int range() {
        return (Integer) cache.get("range");
    }

    public int checkInterval() {
        return (Integer) cache.get("check-interval");
    }

    public int outlineInterval() {
        return (Integer) cache.get("outline-interval");
    }

    public int outlineStep() {
        return (Integer) cache.get("outline-step");
    }

    public Particle outlineParticle() {
        return (Particle) cache.get("outline-particle");
    }

    public float outlineParticleSize() {
        return (Float) cache.get("outline-particle-size");
    }

    public Color outlineParticleColor() {
        return (Color) cache.get("outline-particle-color");
    }

    public boolean recipeEnabled() {
        return (Boolean) cache.get("recipe-enabled");
    }

    public String msg(String key) {
        return ChatColor.translateAlternateColorCodes('&', config.getString("messages." + key, ""));
    }

    public String msg(String key, Object... args) {
        try {
            return String.format(msg(key), args);
        } catch (Exception e) {
            return msg(key);
        }
    }
}
