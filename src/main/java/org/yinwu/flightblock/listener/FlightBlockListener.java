package org.yinwu.flightblock.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.yinwu.flightblock.YinwuFlightBlockPlugin;
import org.yinwu.flightblock.config.ConfigManager;
import org.yinwu.flightblock.flight.FlightBlockItem;
import org.yinwu.flightblock.flight.FlightBlockManager;
import org.yinwu.flightblock.flight.FlightBlockPos;
import org.yinwu.flightblock.flight.FlightTracker;

public class FlightBlockListener implements Listener {

    private final YinwuFlightBlockPlugin plugin;
    private final ConfigManager config;
    private final FlightBlockManager manager;
    private final FlightTracker tracker;

    public FlightBlockListener(YinwuFlightBlockPlugin plugin, ConfigManager config,
                               FlightBlockManager manager, FlightTracker tracker) {
        this.plugin = plugin;
        this.config = config;
        this.manager = manager;
        this.tracker = tracker;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (!FlightBlockItem.isFlightBlock(event.getItemInHand())) return;
        manager.add(FlightBlockPos.from(event.getBlock().getLocation()), false);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        FlightBlockPos pos = FlightBlockPos.from(event.getBlock().getLocation());
        if (!manager.isTracked(pos)) return;
        event.setDropItems(false);
        Location dropLoc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        event.getBlock().getWorld().dropItemNaturally(dropLoc, FlightBlockItem.create(manager.blockMaterial(), 1));
        manager.remove(pos);
        tracker.onRangeToggled();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // 只处理主手：一次右键主副手会各触发一次事件，副手再 toggle 会把刚开启的立刻关掉
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        FlightBlockPos pos = FlightBlockPos.from(block.getLocation());
        if (!manager.isTracked(pos)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Bukkit.getRegionScheduler().run(plugin, block.getLocation(), task -> {
            if (!block.getChunk().isLoaded() || block.getType() != manager.blockMaterial()) return;
            boolean on = !manager.isShowingRange(pos);
            manager.setShowRange(pos, on);
            tracker.onRangeToggled();
            int range = config.range();
            player.getScheduler().run(plugin, t -> {
                if (!player.isOnline()) return;
                player.sendMessage(on ? config.msg("range-toggle-on", range) : config.msg("range-toggle-off", range));
                if (!on) tracker.drawOnce(player, block.getLocation());
            }, null);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().run(plugin, t -> tracker.checkPlayer(player), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tracker.onQuit(event.getPlayer());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().run(plugin, t -> tracker.checkPlayer(player), null);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().run(plugin, t -> tracker.checkPlayer(player), null);
    }
}
