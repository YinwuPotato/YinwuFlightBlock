package org.yinwu.flightblock.flight;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public record FlightBlockPos(UUID world, int x, int y, int z) {

    public static FlightBlockPos from(Location loc) {
        return new FlightBlockPos(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /** 仅供交给 RegionScheduler 使用；世界未加载时返回 null。 */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z);
    }

    /** 立方体范围：以方块中心为心，每轴 ±range 格内均生效（与轮廓画法一致）。 */
    public boolean withinCube(double px, double py, double pz, int range) {
        double r = range;
        return Math.abs(px - (x + 0.5)) <= r
                && Math.abs(py - (y + 0.5)) <= r
                && Math.abs(pz - (z + 0.5)) <= r;
    }
}
