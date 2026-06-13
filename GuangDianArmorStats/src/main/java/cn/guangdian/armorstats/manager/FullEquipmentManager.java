package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class FullEquipmentManager {

    private final EquipmentSlot[] ALL_SLOTS = {
        EquipmentSlot.HAND,        // 主手
        EquipmentSlot.OFF_HAND,    // 副手
        EquipmentSlot.HEAD,        // 头部
        EquipmentSlot.CHEST,       // 胸部
        EquipmentSlot.LEGS,        // 腿部
        EquipmentSlot.FEET         // 脚部
    };

    private final StatsManager statsManager;

    public FullEquipmentManager(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    public PlayerStats refreshAllEquipment(Player player) {
        PlayerStats totalStats = new PlayerStats();

        for (EquipmentSlot slot : ALL_SLOTS) {
            ItemStack item = getEquipmentItem(player, slot);
            if (item != null && statsManager.hasParsableAttributes(item)) {
                PlayerStats itemStats = parseItemAttributes(item);
                totalStats.addPlayerStats(itemStats);
            }
        }

        return totalStats;
    }

    private ItemStack getEquipmentItem(Player player, EquipmentSlot slot) {
        switch (slot) {
            case HAND:
                return player.getInventory().getItemInMainHand();
            case OFF_HAND:
                return player.getInventory().getItemInOffHand();
            case HEAD:
                return player.getInventory().getHelmet();
            case CHEST:
                return player.getInventory().getChestplate();
            case LEGS:
                return player.getInventory().getLeggings();
            case FEET:
                return player.getInventory().getBoots();
            default:
                return null;
        }
    }

    private PlayerStats parseItemAttributes(ItemStack item) {
        PlayerStats stats = new PlayerStats();
        statsManager.addItemAttributes(stats, null, item);
        return stats;
    }

    public boolean hasEquipmentChanged(Player player, EquipmentSlot slot) {
        // 实现装备变化检测逻辑
        return true;
    }

    public EquipmentSlot[] getAllSlots() {
        return ALL_SLOTS;
    }
}