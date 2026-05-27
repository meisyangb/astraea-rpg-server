package cn.guangdian.decompose.manager;

import cn.guangdian.decompose.GuangDianDecompose;
import cn.guangdian.decompose.model.DecomposeRule;
import cn.guangdian.rpgcore.sound.SoundService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DecomposeManager {

    private final GuangDianDecompose plugin;
    private final Random random;

    public DecomposeManager(GuangDianDecompose plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    public DecomposeResult decompose(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return DecomposeResult.failure("无效的物品!");
        }

        String mythicId = plugin.getMythicMobsHook().getMythicItemId(item);
        if (mythicId == null) {
            return DecomposeResult.failure(plugin.getConfig().getString("messages.not-mythic-item", "此物品无法分解!"));
        }

        DecomposeRule rule = plugin.getRuleManager().getRule(mythicId);
        if (rule == null) {
            return DecomposeResult.failure(plugin.getConfig().getString("messages.no-decompose-rule", "该装备没有配置分解规则!"));
        }

        if (random.nextDouble() > rule.getChance()) {
            return DecomposeResult.failure("分解失败，物品已损坏!");
        }

        List<ItemStack> rewards = new ArrayList<>();
        for (DecomposeRule.MaterialReward material : rule.getMaterials()) {
            ItemStack rewardItem = createRewardItem(material);
            if (rewardItem != null) {
                rewards.add(rewardItem);
            }
        }

        if (!hasInventorySpace(player, rewards.size())) {
            return DecomposeResult.failure(plugin.getConfig().getString("messages.inventory-full", "背包已满，无法获得分解材料!"));
        }

        item.setAmount(item.getAmount() - 1);

        for (ItemStack reward : rewards) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }

        playSound(player, "success");

        String message;
        if (rule.hasCustomMessage()) {
            message = rule.getMessage();
        } else {
            StringBuilder materialsStr = new StringBuilder();
            for (int i = 0; i < rule.getMaterials().size(); i++) {
                DecomposeRule.MaterialReward mat = rule.getMaterials().get(i);
                if (i > 0) materialsStr.append(", ");
                materialsStr.append(mat.getItemId()).append("x").append(mat.getAmount());
            }
            message = plugin.getConfig().getString("messages.success", "分解成功！获得材料: {materials}")
                    .replace("{materials}", materialsStr.toString());
        }

        return DecomposeResult.success(message, rewards);
    }

    private ItemStack createRewardItem(DecomposeRule.MaterialReward material) {
        if (material.isMythicItem()) {
            return plugin.getMythicMobsHook().getMythicItem(material.getItemId(), material.getAmount());
        } else if (material.isVanillaItem()) {
            try {
                Material mat = Material.valueOf(material.getItemId().toUpperCase());
                return new ItemStack(mat, material.getAmount());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("未知的原版材料: " + material.getItemId());
                return null;
            }
        }
        return null;
    }

    private boolean hasInventorySpace(Player player, int slotsNeeded) {
        Inventory inv = player.getInventory();
        int emptySlots = 0;
        for (ItemStack item : inv.getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        return emptySlots >= slotsNeeded;
    }

    private void playSound(Player player, String soundType) {
        String soundName = plugin.getConfig().getString("sounds." + soundType, "");
        if (soundName.isEmpty()) return;

        // 使用 RPGCore SoundService
        SoundService soundService = plugin.getSoundService();
        if (soundService != null) {
            soundService.playSound(player, soundName, 1.0f, 1.0f);
        }
    }

    public static class DecomposeResult {
        private final boolean success;
        private final String message;
        private final List<ItemStack> rewards;

        private DecomposeResult(boolean success, String message, List<ItemStack> rewards) {
            this.success = success;
            this.message = message;
            this.rewards = rewards;
        }

        public static DecomposeResult success(String message, List<ItemStack> rewards) {
            return new DecomposeResult(true, message, rewards);
        }

        public static DecomposeResult failure(String message) {
            return new DecomposeResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public List<ItemStack> getRewards() {
            return rewards;
        }
    }
}
