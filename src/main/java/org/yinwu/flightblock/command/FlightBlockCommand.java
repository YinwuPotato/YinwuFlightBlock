package org.yinwu.flightblock.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.yinwu.flightblock.YinwuFlightBlockPlugin;
import org.yinwu.flightblock.config.ConfigManager;
import org.yinwu.flightblock.flight.FlightBlockItem;
import org.yinwu.flightblock.flight.FlightBlockManager;
import org.yinwu.flightblock.flight.FlightBlockPos;
import org.yinwu.flightblock.flight.FlightTracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FlightBlockCommand implements CommandExecutor, TabCompleter {

    private final YinwuFlightBlockPlugin plugin;
    private final ConfigManager config;
    private final FlightBlockManager manager;
    private final FlightTracker tracker;

    public FlightBlockCommand(YinwuFlightBlockPlugin plugin, ConfigManager config,
                              FlightBlockManager manager, FlightTracker tracker) {
        this.plugin = plugin;
        this.config = config;
        this.manager = manager;
        this.tracker = tracker;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(config.msg("usage"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "remove" -> handleRemove(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(config.msg("usage"));
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(config.msg("usage"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(config.msg("player-not-found", args[1]));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException e) {
                sender.sendMessage(config.msg("usage"));
                return;
            }
        }
        ItemStack item = FlightBlockItem.create(manager.blockMaterial(), amount);
        target.getScheduler().run(plugin, t -> {
            if (target.isOnline()) target.getInventory().addItem(item);
        }, null);
        sender.sendMessage(config.msg("give-success", target.getName(), amount));
    }

    private void handleList(CommandSender sender) {
        List<FlightBlockPos> all = new ArrayList<>(manager.snapshot());
        if (all.isEmpty()) {
            sender.sendMessage(config.msg("no-blocks"));
            return;
        }
        sender.sendMessage(color("&e飞行方块 (" + all.size() + "):"));
        for (FlightBlockPos pos : all) {
            String suffix = manager.isShowingRange(pos) ? " [显示范围]" : "";
            sender.sendMessage(color("  &7" + pos.world() + " " + pos.x() + "," + pos.y() + "," + pos.z() + suffix));
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        World world;
        int idx = 1;
        if (args.length >= 5) {
            world = Bukkit.getWorld(args[1]);
            if (world == null) {
                sender.sendMessage(config.msg("world-not-found", args[1]));
                return;
            }
            idx = 2;
        } else if (sender instanceof Player player) {
            world = player.getWorld();
        } else {
            sender.sendMessage(config.msg("usage"));
            return;
        }
        if (args.length < idx + 3) {
            sender.sendMessage(config.msg("usage"));
            return;
        }
        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(args[idx]);
            y = Integer.parseInt(args[idx + 1]);
            z = Integer.parseInt(args[idx + 2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(config.msg("usage"));
            return;
        }
        FlightBlockPos pos = new FlightBlockPos(world.getUID(), x, y, z);
        Location loc = new Location(world, x, y, z);
        Bukkit.getRegionScheduler().run(plugin, loc, task -> {
            if (loc.getBlock().getType() == manager.blockMaterial()) {
                loc.getBlock().setType(Material.AIR);
            }
            if (manager.isTracked(pos)) {
                manager.remove(pos);
                tracker.onRangeToggled();
                sendTo(sender, config.msg("removed", x + "," + y + "," + z));
            } else {
                sendTo(sender, config.msg("block-not-found"));
            }
        });
    }

    private void handleReload(CommandSender sender) {
        config.reload();
        manager.reload();
        tracker.onRangeToggled();
        plugin.refreshRecipe();
        sender.sendMessage(config.msg("reloaded"));
    }

    /** 在区域回调里回发消息：玩家 sender 必须派发回玩家线程。 */
    private void sendTo(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            player.getScheduler().run(plugin, t -> {
                if (player.isOnline()) player.sendMessage(message);
            }, null);
        } else {
            sender.sendMessage(message);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("give", "list", "remove", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return filter(Bukkit.getWorlds().stream().map(World::getName).toList(), args[1]);
        }
        return Collections.emptyList();
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private List<String> filter(List<String> candidates, String prefix) {
        String lower = prefix.toLowerCase();
        return candidates.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }
}
