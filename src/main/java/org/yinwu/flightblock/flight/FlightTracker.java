package org.yinwu.flightblock.flight;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.yinwu.flightblock.YinwuFlightBlockPlugin;
import org.yinwu.flightblock.config.ConfigManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlightTracker {

    private final YinwuFlightBlockPlugin plugin;
    private final ConfigManager config;
    private final FlightBlockManager manager;
    private final Set<UUID> granted = ConcurrentHashMap.newKeySet();
    private volatile ScheduledTask outlineTask;
    private volatile boolean shuttingDown;

    public FlightTracker(YinwuFlightBlockPlugin plugin, ConfigManager config, FlightBlockManager manager) {
        this.plugin = plugin;
        this.config = config;
        this.manager = manager;
    }

    public void start() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            if (shuttingDown) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().run(plugin, t -> checkPlayer(player), null);
            }
        }, 1L, config.checkInterval());

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            if (shuttingDown) return;
            manager.validateAll();
        }, 1L, Math.max(20L, config.checkInterval() * 5L));
    }

    /** 必须在玩家实体线程调用。 */
    public void checkPlayer(Player player) {
        if (shuttingDown || !player.isOnline()) return;
        GameMode mode = player.getGameMode();
        boolean inRange = false;
        if (!flightNative(mode)) {
            Location loc = player.getLocation();
            UUID world = loc.getWorld().getUID();
            int range = config.range();
            for (FlightBlockPos pos : manager.snapshot()) {
                if (pos.world().equals(world) && pos.withinCube(loc.getX(), loc.getY(), loc.getZ(), range)) {
                    inRange = true;
                    break;
                }
            }
        }
        boolean marked = FlightBlockItem.hasGrant(player);
        if (inRange) {
            if (!marked) {
                player.setAllowFlight(true);
                FlightBlockItem.setGrant(player, true);
                granted.add(player.getUniqueId());
                player.sendActionBar(config.msg("flight-enabled"));
            }
        } else if (marked) {
            revoke(player);
            // 创造/旁观切走时静默清标记，不打扰原生飞行、不发「已禁用」
            if (!flightNative(mode)) {
                player.sendActionBar(config.msg("flight-disabled"));
            }
        }
    }

    /** 创造/旁观模式下飞行是原生的，本插件不应 setAllowFlight/setFlying 触碰。 */
    private static boolean flightNative(GameMode mode) {
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    }

    private void revoke(Player player) {
        if (!flightNative(player.getGameMode())) {
            player.setAllowFlight(false);
            if (player.isFlying()) player.setFlying(false);
        }
        FlightBlockItem.setGrant(player, false);
        granted.remove(player.getUniqueId());
    }

    /** 自包含回收：不碰插件字段、不发消息，disable 后执行也安全。 */
    private void revokeQuietly(Player player) {
        if (!flightNative(player.getGameMode())) {
            player.setAllowFlight(false);
            if (player.isFlying()) player.setFlying(false);
        }
        FlightBlockItem.setGrant(player, false);
    }

    public void onQuit(Player player) {
        granted.remove(player.getUniqueId());
    }

    public void shutdown() {
        shuttingDown = true;
        if (outlineTask != null && !outlineTask.isCancelled()) {
            outlineTask.cancel();
            outlineTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(plugin, task -> {
                if (FlightBlockItem.hasGrant(player)) revokeQuietly(player);
            }, null);
        }
    }

    public void onRangeToggled() {
        if (manager.anyShowingRange()) {
            startOutlineIfNeeded();
        } else if (outlineTask != null && !outlineTask.isCancelled()) {
            outlineTask.cancel();
            outlineTask = null;
        }
    }

    private void startOutlineIfNeeded() {
        if (outlineTask != null && !outlineTask.isCancelled()) return;
        outlineTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,
                task -> drawOutlines(), 1L, config.outlineInterval());
    }

    private void drawOutlines() {
        if (shuttingDown) return;
        for (FlightBlockPos pos : manager.snapshot()) {
            if (!manager.isShowingRange(pos)) continue;
            Location loc = pos.toLocation();
            if (loc == null) continue;
            Bukkit.getRegionScheduler().run(plugin, loc, task -> {
                World world = loc.getWorld();
                if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;
                if (world.getBlockAt(loc).getType() != manager.blockMaterial()) return;
                int range = config.range();
                for (UUID id : granted) {
                    Player player = Bukkit.getPlayer(id);
                    if (player == null || !player.isOnline()) continue;
                    player.getScheduler().run(plugin, t -> drawCube(player, loc, range), null);
                }
            });
        }
    }

    /** 玩家线程调用：一次性画范围轮廓（关闭时给点击玩家）。 */
    public void drawOnce(Player player, Location center) {
        if (shuttingDown) return;
        drawCube(player, center, config.range());
    }

    private void drawCube(Player player, Location center, int range) {
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        int step = config.outlineStep();
        int x0 = x - range;
        int y0 = y - range;
        int z0 = z - range;
        int x1 = x + range;
        int y1 = y + range;
        int z1 = z + range;
        Particle particle = config.outlineParticle();
        Particle.DustOptions dust = particle == Particle.DUST
                ? new Particle.DustOptions(config.outlineParticleColor(), config.outlineParticleSize())
                : null;
        for (int i = 0; i <= range * 2; i += step) {
            int xi = x0 + i;
            int yi = y0 + i;
            int zi = z0 + i;
            // 沿 X 方向的 4 条边（立方体居中，与 withinCube 判定一致）
            drawParticle(player, particle, dust, xi, y0, z0);
            drawParticle(player, particle, dust, xi, y0, z1);
            drawParticle(player, particle, dust, xi, y1, z0);
            drawParticle(player, particle, dust, xi, y1, z1);
            // 沿 Z 方向的 4 条边
            drawParticle(player, particle, dust, x0, y0, zi);
            drawParticle(player, particle, dust, x0, y1, zi);
            drawParticle(player, particle, dust, x1, y0, zi);
            drawParticle(player, particle, dust, x1, y1, zi);
            // 沿 Y 方向的 4 条边
            drawParticle(player, particle, dust, x0, yi, z0);
            drawParticle(player, particle, dust, x0, yi, z1);
            drawParticle(player, particle, dust, x1, yi, z0);
            drawParticle(player, particle, dust, x1, yi, z1);
        }
    }

    /** DUST 粒子需携带 DustOptions（颜色/大小），其余粒子直接撒。 */
    private void drawParticle(Player player, Particle particle, Particle.DustOptions dust, int px, int py, int pz) {
        if (dust != null) {
            player.spawnParticle(particle, px, py, pz, 1, 0, 0, 0, 0, dust);
        } else {
            player.spawnParticle(particle, px, py, pz, 1, 0, 0, 0, 0);
        }
    }
}
