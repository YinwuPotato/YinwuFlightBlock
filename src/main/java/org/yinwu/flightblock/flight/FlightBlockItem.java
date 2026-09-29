package org.yinwu.flightblock.flight;

import net.yinwu.lib.item.ItemBuilder;
import net.yinwu.lib.item.NamespacedKeyCache;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class FlightBlockItem {

    private static NamespacedKeyCache ITEM_KEY;
    private static NamespacedKeyCache GRANT_KEY;

    private FlightBlockItem() {
    }

    public static void init(Plugin plugin) {
        ITEM_KEY = new NamespacedKeyCache(plugin, "flight_block");
        GRANT_KEY = new NamespacedKeyCache(plugin, "flight_granted");
    }

    public static ItemStack create(Material material, int amount) {
        return ItemBuilder.of(material).amount(amount)
                .name("&b飞行方块")
                .lore("&7放置后，范围内玩家获得飞行能力", "&7右键切换显示范围")
                .pdc(ITEM_KEY, PersistentDataType.BOOLEAN, true)
                .build();
    }

    public static boolean isFlightBlock(ItemStack item) {
        return item != null && item.getType().isBlock() && ItemBuilder.hasPdc(item, ITEM_KEY);
    }

    public static boolean hasGrant(Player player) {
        return player.getPersistentDataContainer().has(GRANT_KEY.key(), PersistentDataType.BOOLEAN);
    }

    public static void setGrant(Player player, boolean on) {
        if (on) {
            player.getPersistentDataContainer().set(GRANT_KEY.key(), PersistentDataType.BOOLEAN, true);
        } else {
            player.getPersistentDataContainer().remove(GRANT_KEY.key());
        }
    }
}
