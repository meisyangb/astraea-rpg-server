package cn.guangdian.mobs.manager;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.DropTable;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 掉落管理器
 */
public class DropManager {

    private final GuangDianMobs plugin;
    private final Map<String, DropTable> dropTables = new HashMap<>();

    public DropManager(GuangDianMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载掉落配置
     */
    public void loadDrops() {
        dropTables.clear();

        File file = new File(plugin.getDataFolder(), "drops.yml");
        if (!file.exists()) {
            plugin.saveResource("drops.yml", false);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("drop-tables");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection dropSection = section.getConfigurationSection(id);
            if (dropSection == null) continue;

            try {
                DropTable dropTable = parseDropTable(id, dropSection);
                if (dropTable.isValid()) {
                    dropTables.put(id, dropTable);
                    plugin.getLogger().info("加载掉落表: " + id);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载掉落表失败: " + id + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("共加载 " + dropTables.size() + " 个掉落表");
    }

    /**
     * 解析掉落表配置
     */
    private DropTable parseDropTable(String id, ConfigurationSection section) {
        DropTable dropTable = new DropTable(id);

        dropTable.setDisplayName(section.getString("display-name", id));
        dropTable.setExpMin(section.getDouble("experience", 0));
        dropTable.setExpMax(section.getDouble("experience", 0));
        dropTable.setMoneyMin(section.getDouble("money", 0));
        dropTable.setMoneyMax(section.getDouble("money", 0));

        // 解析物品 - 支持列表格式
        List<Map<?, ?>> itemsList = section.getMapList("items");
        for (Map<?, ?> itemMap : itemsList) {
            String itemId = String.valueOf(itemMap.get("item"));
            if (itemId == null || itemId.isEmpty()) continue;

            // 解析数量范围 (格式: "1-3" 或 "5")
            Object amountObj = itemMap.get("amount");
            String amountStr = amountObj != null ? amountObj.toString() : "1";
            int amountMin = 1, amountMax = 1;
            if (amountStr.contains("-")) {
                String[] parts = amountStr.split("-");
                try {
                    amountMin = Integer.parseInt(parts[0].trim());
                    amountMax = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    amountMin = amountMax = 1;
                }
            } else {
                try {
                    amountMin = amountMax = Integer.parseInt(amountStr.trim());
                } catch (NumberFormatException e) {
                    amountMin = amountMax = 1;
                }
            }

            double chance = 1.0;
            Object chanceObj = itemMap.get("chance");
            if (chanceObj instanceof Number) {
                chance = ((Number) chanceObj).doubleValue();
            } else if (chanceObj instanceof String) {
                try {
                    chance = Double.parseDouble((String) chanceObj);
                } catch (NumberFormatException ignored) {}
            }

            dropTable.addItem(itemId, amountMin, amountMax, chance);
        }

        return dropTable;
    }

    /**
     * 处理怪物死亡掉落
     */
    public void handleMobDeath(org.bukkit.entity.LivingEntity entity, String dropTableId, Player killer) {
        DropTable dropTable = dropTables.get(dropTableId);
        if (dropTable == null) return;

        Location loc = entity.getLocation();

        // 掉落物品
        for (DropTable.DropItem dropItem : dropTable.getItems()) {
            if (ThreadLocalRandom.current().nextDouble() <= dropItem.getChance()) {
                ItemStack item = createItem(dropItem);
                if (item != null) {
                    entity.getWorld().dropItemNaturally(loc, item);
                }
            }
        }

        // 给予经验
        if (killer != null && dropTable.getExpMax() > 0) {
            int exp = (int) getRandomValue(dropTable.getExpMin(), dropTable.getExpMax());
            if (exp > 0) {
                killer.giveExp(exp);
            }
        }

        // 给予金钱（需要经济插件支持）
        if (killer != null && dropTable.getMoneyMax() > 0) {
            double money = getRandomValue(dropTable.getMoneyMin(), dropTable.getMoneyMax());
            if (money > 0) {
                giveMoney(killer, money);
            }
        }
    }

    /**
     * 创建掉落物品
     */
    private ItemStack createItem(DropTable.DropItem dropItem) {
        int amount = ThreadLocalRandom.current().nextInt(dropItem.getAmountMin(), dropItem.getAmountMax() + 1);

        // 只支持原版物品
        Material material = Material.matchMaterial(dropItem.getItemId());
        if (material == null) return null;
        return new ItemStack(material, amount);
    }

    /**
     * 给予玩家金钱
     */
    private void giveMoney(Player player, double amount) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return;

        ExternalServiceIntegration externalServices = rpgCore.getExternalServices();
        if (externalServices != null && externalServices.isVaultEnabled()) {
            externalServices.deposit(player, amount);
        }
    }

    /**
     * 获取随机值
     */
    private double getRandomValue(double min, double max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * 获取掉落表
     */
    public DropTable getDropTable(String id) {
        return dropTables.get(id);
    }

    /**
     * 获取所有掉落表
     */
    public Collection<DropTable> getAllDropTables() {
        return dropTables.values();
    }
}
