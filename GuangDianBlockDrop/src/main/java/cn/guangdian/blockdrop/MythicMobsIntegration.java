package cn.guangdian.blockdrop;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class MythicMobsIntegration {

    private static boolean enabled = false;

    public static void initialize() {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            enabled = true;
            GuangDianBlockDrop.getInstance().getLogger().info("MythicMobs 集成已启用!");
        } else {
            enabled = false;
            GuangDianBlockDrop.getInstance().getLogger().warning("MythicMobs 未安装, MythicMobs 物品掉落将不可用!");
        }
    }

    public static ItemStack getMythicItem(String itemName, int amount) {
        if (!enabled) {
            GuangDianBlockDrop.getInstance().getLogger().warning("MythicMobs 未启用, 无法获取物品: " + itemName);
            return null;
        }

        try {
            Optional<MythicItem> mythicItemOpt = MythicBukkit.inst().getItemManager().getItem(itemName);
            if (mythicItemOpt.isPresent()) {
                MythicItem mythicItem = mythicItemOpt.get();
                var abstractItem = mythicItem.generateItemStack(amount);
                if (abstractItem instanceof BukkitItemStack bukkitItem) {
                    return bukkitItem.getItemStack();
                }
                GuangDianBlockDrop.getInstance().getLogger().warning(
                    "MythicMobs 物品类型转换失败: " + itemName);
            } else {
                GuangDianBlockDrop.getInstance().getLogger().warning("未找到 MythicMobs 物品: " + itemName);
            }
        } catch (Exception e) {
            GuangDianBlockDrop.getInstance().getLogger().warning(
                "获取 MythicMobs 物品异常: " + itemName + " - " + e.getMessage());
        }

        return null;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}