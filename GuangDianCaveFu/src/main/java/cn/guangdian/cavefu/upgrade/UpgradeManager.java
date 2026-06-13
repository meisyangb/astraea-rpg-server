package cn.guangdian.cavefu.upgrade;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveLevel;
import cn.guangdian.cavefu.cave.CaveManager;
import cn.guangdian.cavefu.config.ConfigManager;
import cn.guangdian.cavefu.hook.RPGItemsHook;
import cn.guangdian.cavefu.storage.DataManager;
import cn.guangdian.cavefu.world.CaveWorldManager;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 升级管理器
 */
public class UpgradeManager {
    private final GuangDianCaveFu plugin;
    private final ConfigManager configManager;
    private final CaveManager caveManager;
    private final DataManager dataManager;
    private final CaveWorldManager worldManager;

    public UpgradeManager(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.caveManager = plugin.getCaveManager();
        this.dataManager = plugin.getDataManager();
        this.worldManager = plugin.getWorldManager();
    }

    /**
     * 检查升级条件
     * @return 是否满足升级条件
     */
    public boolean canUpgrade(Player player, Cave cave) {
        CaveLevel nextLevel = configManager.getNextLevel(cave.getLevel());
        if (nextLevel == null) {
            return false;
        }

        return checkItems(player, nextLevel.getUpgradeCost());
    }

    /**
     * 执行升级
     */
    public boolean upgrade(Player player, Cave cave) {
        CaveLevel nextLevel = configManager.getNextLevel(cave.getLevel());
        if (nextLevel == null) {
            return false;
        }

        // 检查并扣除物品
        if (!consumeItems(player, nextLevel.getUpgradeCost())) {
            return false;
        }

        // 获取旧大小
        CaveLevel currentLevel = configManager.getLevel(cave.getLevel());
        int oldSize = currentLevel != null ? currentLevel.getSize() : 4;

        // 升级洞府
        cave.setLevel(nextLevel.getLevel());
        dataManager.save();

        // 扩展平台
        worldManager.expandPlatform(cave, oldSize, nextLevel.getSize());

        return true;
    }

    /**
     * 检查物品是否满足需求
     */
    public boolean checkItems(Player player, List<String> requirements) {
        for (String req : requirements) {
            if (!checkItem(player, req)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查单个物品需求
     * 格式: rpgitem:物品名:数量 或 vanilla:材料名:数量
     */
    private boolean checkItem(Player player, String requirement) {
        String[] parts = requirement.split(":");
        if (parts.length < 3) return true;

        String type = parts[0].toLowerCase();
        String itemName = parts[1];
        int amount = Integer.parseInt(parts[2]);

        if (type.equals("rpgitem")) {
            // RPGItems 物品
            int count = countRPGItem(player, itemName);
            return count >= amount;
        } else if (type.equals("vanilla")) {
            // 原版物品
            int count = countVanillaItem(player, itemName);
            return count >= amount;
        }

        return true;
    }

    /**
     * 扣除物品
     */
    private boolean consumeItems(Player player, List<String> requirements) {
        // 先检查是否全部满足
        if (!checkItems(player, requirements)) {
            return false;
        }

        // 逐个扣除
        for (String req : requirements) {
            if (!consumeItem(player, req)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 扣除单个物品
     */
    private boolean consumeItem(Player player, String requirement) {
        String[] parts = requirement.split(":");
        if (parts.length < 3) return true;

        String type = parts[0].toLowerCase();
        String itemName = parts[1];
        int amount = Integer.parseInt(parts[2]);

        if (type.equals("rpgitem")) {
            return consumeRPGItem(player, itemName, amount);
        } else if (type.equals("vanilla")) {
            return consumeVanillaItem(player, itemName, amount);
        }

        return true;
    }

    /**
     * 统计RPGItems物品数量
     */
    private int countRPGItem(Player player, String itemId) {
        int total = 0;
        RPGItemsHook rpgItemsHook = plugin.getRPGItemsHook();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;

            String rpgId = rpgItemsHook.getRPGItemId(item);
            if (rpgId != null && rpgId.equals(itemId)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /**
     * 扣除RPGItems物品
     */
    private boolean consumeRPGItem(Player player, String itemId, int amount) {
        int remaining = amount;
        RPGItemsHook rpgItemsHook = plugin.getRPGItemsHook();

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || !item.hasItemMeta()) continue;

            String rpgId = rpgItemsHook.getRPGItemId(item);
            if (rpgId != null && rpgId.equals(itemId)) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }

        player.updateInventory();
        return remaining == 0;
    }

    /**
     * 统计原版物品数量
     */
    private int countVanillaItem(Player player, String materialName) {
        try {
            org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialName);
            if (material == null) return 0;

            int total = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    total += item.getAmount();
                }
            }
            return total;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 扣除原版物品
     */
    private boolean consumeVanillaItem(Player player, String materialName, int amount) {
        try {
            org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialName);
            if (material == null) return false;

            int remaining = amount;
            ItemStack[] contents = player.getInventory().getContents();

            for (int i = 0; i < contents.length && remaining > 0; i++) {
                ItemStack item = contents[i];
                if (item != null && item.getType() == material) {
                    int itemAmount = item.getAmount();
                    if (itemAmount <= remaining) {
                        remaining -= itemAmount;
                        player.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(itemAmount - remaining);
                        remaining = 0;
                    }
                }
            }

            player.updateInventory();
            return remaining == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取升级所需物品描述
     */
    public String getUpgradeCostDescription(int level) {
        CaveLevel caveLevel = configManager.getLevel(level);
        if (caveLevel == null) return "";

        StringBuilder sb = new StringBuilder();
        for (String cost : caveLevel.getUpgradeCost()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(cost.replace("rpgitem:", "").replace("vanilla:", ""));
        }

        return sb.toString();
    }
}