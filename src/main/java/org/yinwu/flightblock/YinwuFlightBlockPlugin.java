package org.yinwu.flightblock;

import net.yinwu.lib.plugin.YinwuPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.yinwu.flightblock.command.FlightBlockCommand;
import org.yinwu.flightblock.config.ConfigManager;
import org.yinwu.flightblock.flight.FlightBlockItem;
import org.yinwu.flightblock.flight.FlightBlockManager;
import org.yinwu.flightblock.flight.FlightTracker;
import org.yinwu.flightblock.listener.FlightBlockListener;

public class YinwuFlightBlockPlugin extends YinwuPlugin {

    private static final NamespacedKey RECIPE_KEY = new NamespacedKey("yinwu", "flight_block");

    private ConfigManager config;
    private FlightBlockManager manager;
    private FlightTracker tracker;

    @Override
    public String name() {
        return "YinwuFlightBlock";
    }

    @Override
    public void enable() {
        config = new ConfigManager(this);
        if (!config.enabled()) {
            getLogger().info(name() + " 已在配置中禁用");
            return;
        }
        FlightBlockItem.init(this);
        manager = new FlightBlockManager(this, config);
        manager.load();
        tracker = new FlightTracker(this, config, manager);
        tracker.start();
        getServer().getPluginManager().registerEvents(new FlightBlockListener(this, config, manager, tracker), this);
        FlightBlockCommand cmd = new FlightBlockCommand(this, config, manager, tracker);
        getCommand("flightblock").setExecutor(cmd);
        getCommand("flightblock").setTabCompleter(cmd);
        if (config.recipeEnabled()) {
            registerRecipe();
        }
        getLogger().info(name() + " 已启用（范围 " + config.range() + " 格）");
    }

    @Override
    public void disable() {
        if (tracker != null) tracker.shutdown();
        if (manager != null) manager.save();
    }

    /** reload 时移除旧配方并按新配置重新注册。 */
    public void refreshRecipe() {
        Bukkit.getGlobalRegionScheduler().run(this, task -> {
            Bukkit.removeRecipe(RECIPE_KEY);
            if (config.recipeEnabled()) registerRecipe();
        });
    }

    private void registerRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(RECIPE_KEY, FlightBlockItem.create(config.blockMaterial(), 1));
        recipe.shape("ABA", "CCC");
        recipe.setIngredient('A', Material.IRON_BARS);
        recipe.setIngredient('B', Material.FIREWORK_ROCKET);
        recipe.setIngredient('C', Material.IRON_INGOT);
        Bukkit.addRecipe(recipe);
    }
}
