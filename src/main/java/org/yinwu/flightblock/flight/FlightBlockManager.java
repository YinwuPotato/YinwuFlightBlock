package org.yinwu.flightblock.flight;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yinwu.flightblock.YinwuFlightBlockPlugin;
import org.yinwu.flightblock.config.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class FlightBlockManager {

    private final YinwuFlightBlockPlugin plugin;
    private final ConfigManager config;
    private final Set<FlightBlockPos> blocks = ConcurrentHashMap.newKeySet();
    private final Map<FlightBlockPos, Boolean> showRange = new ConcurrentHashMap<>();
    private volatile Material blockMaterial;

    public FlightBlockManager(YinwuFlightBlockPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.blockMaterial = config.blockMaterial();
    }

    public void load() {
        File file = file();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> entry : yaml.getMapList("blocks")) {
            try {
                UUID world = UUID.fromString(String.valueOf(entry.get("world")));
                int x = ((Number) entry.get("x")).intValue();
                int y = ((Number) entry.get("y")).intValue();
                int z = ((Number) entry.get("z")).intValue();
                boolean range = Boolean.parseBoolean(String.valueOf(entry.get("show-range")));
                FlightBlockPos pos = new FlightBlockPos(world, x, y, z);
                blocks.add(pos);
                if (range) showRange.put(pos, true);
            } catch (Exception e) {
                plugin.getLogger().warning("跳过无效的飞行方块记录: " + entry);
            }
        }
        plugin.getLogger().info("已加载 " + blocks.size() + " 个飞行方块");
    }

    public void save() {
        File file = file();
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (FlightBlockPos pos : snapshot()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("world", pos.world().toString());
            entry.put("x", pos.x());
            entry.put("y", pos.y());
            entry.put("z", pos.z());
            entry.put("show-range", showRange.getOrDefault(pos, false));
            list.add(entry);
        }
        yaml.set("blocks", list);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "保存飞行方块数据失败", e);
        }
    }

    private File file() {
        return new File(plugin.getDataFolder(), "flightblocks.yml");
    }

    public void add(FlightBlockPos pos, boolean range) {
        blocks.add(pos);
        if (range) showRange.put(pos, true);
        save();
    }

    public void remove(FlightBlockPos pos) {
        blocks.remove(pos);
        showRange.remove(pos);
        save();
    }

    public void setShowRange(FlightBlockPos pos, boolean on) {
        if (on) {
            blocks.add(pos);
            showRange.put(pos, true);
        } else {
            showRange.remove(pos);
        }
        save();
    }

    public Set<FlightBlockPos> snapshot() {
        return Set.copyOf(blocks);
    }

    public boolean isTracked(FlightBlockPos pos) {
        return blocks.contains(pos);
    }

    public boolean isShowingRange(FlightBlockPos pos) {
        return showRange.getOrDefault(pos, false);
    }

    public boolean anyShowingRange() {
        return !showRange.isEmpty();
    }

    public Material blockMaterial() {
        return blockMaterial;
    }

    /** 周期验证：方块材质不匹配则移除（覆盖爆炸/活塞等不触发 BlockBreakEvent 的破坏）。 */
    public void validateAll() {
        for (FlightBlockPos pos : snapshot()) {
            Location loc = pos.toLocation();
            if (loc == null) continue;
            Bukkit.getRegionScheduler().run(plugin, loc, task -> {
                World world = loc.getWorld();
                if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;
                if (world.getBlockAt(loc).getType() != blockMaterial) remove(pos);
            });
        }
    }

    public void reload() {
        blockMaterial = config.blockMaterial();
        validateAll();
    }
}
