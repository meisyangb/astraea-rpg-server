package cn.guangdian.enhance.storage;

import cn.guangdian.enhance.GuangDianEnhance;
import cn.guangdian.enhance.data.EnhanceData;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.util.TextStripper;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class EnhanceStorage {

    private static final NamespacedKey LEVEL_KEY = new NamespacedKey("guangdianenhance", "level");
    private static final NamespacedKey MAX_LEVEL_KEY = new NamespacedKey("guangdianenhance", "max_level");
    private static final NamespacedKey ATTEMPTS_KEY = new NamespacedKey("guangdianenhance", "attempts");
    
    private final GuangDianEnhance plugin;
    private final MiniMessageService miniMessage;

    public EnhanceStorage(GuangDianEnhance plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();
    }

    public int getLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        
        Integer level = meta.getPersistentDataContainer()
            .get(LEVEL_KEY, PersistentDataType.INTEGER);
        
        return level != null ? level : 0;
    }

    public int getMaxLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return plugin.getEnhanceConfig().getMaxLevel();
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return plugin.getEnhanceConfig().getMaxLevel();
        }
        
        Integer maxLevel = meta.getPersistentDataContainer()
            .get(MAX_LEVEL_KEY, PersistentDataType.INTEGER);
        
        return maxLevel != null ? maxLevel : plugin.getEnhanceConfig().getMaxLevel();
    }

    public EnhanceData getEnhanceData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return new EnhanceData();
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return new EnhanceData();
        }
        
        int level = getLevel(item);
        int maxLevel = getMaxLevel(item);
        
        EnhanceData data = new EnhanceData(level, maxLevel);
        
        Integer attempts = meta.getPersistentDataContainer()
            .get(ATTEMPTS_KEY, PersistentDataType.INTEGER);
        if (attempts != null) {
            data.setLastEnhanceTime(System.currentTimeMillis());
        }
        
        return data;
    }

    public ItemStack setLevel(ItemStack item, int level) {
        if (item == null) {
            return null;
        }
        
        int maxLevel = getMaxLevel(item);
        level = Math.max(0, Math.min(level, maxLevel));
        
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return item;
        }
        
        meta.getPersistentDataContainer()
            .set(LEVEL_KEY, PersistentDataType.INTEGER, level);
        
        updateLore(meta, level);
        
        clone.setItemMeta(meta);
        return clone;
    }

    public ItemStack setEnhanceData(ItemStack item, EnhanceData data) {
        if (item == null || data == null) {
            return item;
        }
        
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return item;
        }
        
        meta.getPersistentDataContainer()
            .set(LEVEL_KEY, PersistentDataType.INTEGER, data.getLevel());
        meta.getPersistentDataContainer()
            .set(MAX_LEVEL_KEY, PersistentDataType.INTEGER, data.getMaxLevel());
        meta.getPersistentDataContainer()
            .set(ATTEMPTS_KEY, PersistentDataType.INTEGER, data.getTotalAttempts());
        
        updateLore(meta, data.getLevel());
        
        clone.setItemMeta(meta);
        return clone;
    }

    private void updateLore(ItemMeta meta, int level) {
        List<String> lore = new ArrayList<>();
        
        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                String stripped = TextStripper.stripAll(line);
                if (!stripped.contains("强化等级") && !stripped.contains("【+" + stripped.replaceAll("[^0-9]", "") + "】")) {
                    lore.add(line);
                }
            }
        }
        
        if (level > 0) {
            String prefix = getLevelPrefix(level);
            String color = getLevelColor(level);
            Component enhanceLine = miniMessage.colorize(
                color + "强化等级: " + prefix + level);
            lore.add(0, miniMessage.legacySerialize(enhanceLine));
        }
        
        meta.setLore(lore);
    }

    private String getLevelPrefix(int level) {
        if (level >= 15) return "★";
        if (level >= 10) return "✦";
        if (level >= 5) return "◆";
        return "+";
    }

    private String getLevelColor(int level) {
        if (level >= 15) return "<gradient:#ff0000:#ffff00:#00ff00>";
        if (level >= 12) return "<color:#ff5500>";
        if (level >= 10) return "<red>";
        if (level >= 7) return "<yellow>";
        if (level >= 5) return "<green>";
        if (level >= 3) return "<aqua>";
        return "<white>";
    }

    public boolean hasEnhanceData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return meta.getPersistentDataContainer()
            .has(LEVEL_KEY, PersistentDataType.INTEGER);
    }

    public ItemStack clearEnhanceData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return item;
        }
        
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return item;
        }
        
        meta.getPersistentDataContainer().remove(LEVEL_KEY);
        meta.getPersistentDataContainer().remove(MAX_LEVEL_KEY);
        meta.getPersistentDataContainer().remove(ATTEMPTS_KEY);
        
        updateLore(meta, 0);
        
        clone.setItemMeta(meta);
        return clone;
    }
}
