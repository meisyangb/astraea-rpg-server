package cn.guangdian.raid.manager;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.RaidReward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class LootManager {

    private final GuangDianRaid plugin;
    private final Random random;
    private final boolean mythicMobsEnabled;

    public LootManager(GuangDianRaid plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.mythicMobsEnabled = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
    }

    public RaidReward calculateReward(RaidInstance instance) {
        RaidReward base = instance.getRaid().getBaseReward();

        double intelBonus = 1.0 + (instance.getCollectedIntel() * 0.1);
        double killBonus = 1.0 + Math.min(instance.getKillCount() * 0.01, 0.5);

        long elapsed = (System.currentTimeMillis() - instance.getStartTime()) / 1000;
        int totalLimit = instance.getRaid().getTotalTimeLimit();
        double timeBonus = 1.0;
        if (elapsed < totalLimit * 0.5) {
            timeBonus = base.getTimeBonusMultiplier();
        } else if (elapsed < totalLimit * 0.75) {
            timeBonus = 1.0 + (base.getTimeBonusMultiplier() - 1.0) * 0.5;
        }

        int playerCount = instance.getTeam().size();
        double teamBonus = 1.0 + (playerCount - 1) * 0.15;

        double totalMultiplier = intelBonus * killBonus * timeBonus * teamBonus * 
            instance.getCurrentDifficulty().getRewardMultiplier();

        return base.multiply(totalMultiplier);
    }

    public void giveRewardItem(Player player, RaidReward.ItemReward reward) {
        ItemStack item = createRewardItem(reward);
        if (item != null) {
            var leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
            player.sendMessage(Component.text("获得奖励: " + reward.getItemType())
                .color(NamedTextColor.GREEN));
        }
    }

    private ItemStack createRewardItem(RaidReward.ItemReward reward) {
        String type = reward.getItemType();
        
        if (type.startsWith("mythicmobs:") && mythicMobsEnabled) {
            return createMythicMobsItem(type.substring(11), reward.getAmount());
        }

        try {
            Material material = Material.valueOf(type.toUpperCase());
            return new ItemStack(material, reward.getAmount());
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.DIAMOND, reward.getAmount());
        }
    }

    private ItemStack createMythicMobsItem(String itemName, int amount) {
        if (!mythicMobsEnabled) {
            return null;
        }

        try {
            Object mythicBukkit = Class.forName("io.lumine.mythic.bukkit.MythicBukkit")
                .getMethod("inst").invoke(null);
            Object itemManager = mythicBukkit.getClass().getMethod("getItemManager").invoke(mythicBukkit);
            Object optionalItem = itemManager.getClass().getMethod("getItem", String.class)
                .invoke(itemManager, itemName);
            
            if (optionalItem instanceof java.util.Optional<?> opt && opt.isPresent()) {
                Object mythicItem = opt.get();
                return (ItemStack) mythicItem.getClass().getMethod("generateItemStack", int.class)
                    .invoke(mythicItem, amount);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("创建MythicMobs物品失败: " + itemName);
        }
        return null;
    }
}
