package cn.guangdian.decompose.manager;

import cn.guangdian.decompose.GuangDianDecompose;
import cn.guangdian.decompose.model.DecomposeRule;
import cn.guangdian.rpgcore.sound.SoundService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

        String rpgId = plugin.getRPGItemsHook().getRPGItemId(item);
        if (rpgId == null) {
            return DecomposeResult.failure(plugin.getConfig().getString("messages.not-rpg-item", "此物品无法分解!"));
        }

        DecomposeRule rule = findRule(item, rpgId);
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

        plugin.debug("[DecomposeManager] 成功创建 " + rewards.size() + "/" + rule.getMaterials().size() + " 个材料物品");
        
        if (rewards.isEmpty()) {
            return DecomposeResult.failure("无法创建分解材料，请联系管理员！");
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
            String template = plugin.getConfig().getString("messages.success", "分解成功！获得材料: {materials}");
            message = plugin.getMiniMessage().replaceUnified(template, "materials", materialsStr.toString());
        }

        return DecomposeResult.success(message, rewards);
    }

    private DecomposeRule findRule(ItemStack item, String rpgId) {
        // 从 PDC 获取阶位进行匹配
        String tier = plugin.getRPGItemsHook().getItemTier(item);
        if (tier != null && !tier.isEmpty()) {
            DecomposeRule.TierRule tierRule = plugin.getRuleManager().matchTierRule(tier);
            if (tierRule != null) {
                plugin.debug("使用阶位规则: " + tierRule.getName() + " (阶位: " + tier + ")");
                return convertTierRuleToRule(tierRule);
            }
        }

        plugin.debug("未找到阶位规则，物品Tier: " + tier);
        return null;
    }

    private DecomposeRule convertTierRuleToRule(DecomposeRule.TierRule tierRule) {
        DecomposeRule rule = new DecomposeRule(tierRule.getName());
        for (DecomposeRule.MaterialReward material : tierRule.getMaterials()) {
            rule.addMaterial(material);
        }
        rule.setChance(tierRule.getChance());
        rule.setMessage(tierRule.getMessage());
        return rule;
    }

    private ItemStack createRewardItem(DecomposeRule.MaterialReward material) {
        plugin.debug("[DecomposeManager] 创建奖励物品: " + material.getItemId() + " x" + material.getAmount() + " (类型: " + (material.isRPGItem() ? "RPGItem" : "Vanilla") + ")");
        
        if (material.isRPGItem()) {
            ItemStack item = plugin.getRPGItemsHook().getRPGItem(material.getItemId(), material.getAmount());
            if (item == null) {
                plugin.getLogger().warning("[DecomposeManager] 无法创建RPGItems物品: " + material.getItemId());
            }
            return item;
        } else if (material.isVanillaItem()) {
            try {
                Material mat = Material.valueOf(material.getItemId().toUpperCase());
                return new ItemStack(mat, material.getAmount());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("未知的原版材料: " + material.getItemId());
                return null;
            }
        }
        plugin.getLogger().warning("[DecomposeManager] 未知的材料类型: " + material.getItemId());
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
