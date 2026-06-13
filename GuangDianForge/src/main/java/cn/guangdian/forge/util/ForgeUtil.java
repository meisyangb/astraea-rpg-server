package cn.guangdian.forge.util;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.model.ForgeRecipe;
import cn.guangdian.forge.model.PlayerForgeData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 锻造工具类
 * 只支持 RPGItems 物品作为锻造结果
 */
public class ForgeUtil {

    /**
     * 计算最终锻造成功率
     */
    public static double calcSuccessRate(ForgeRecipe recipe, PlayerForgeData data, FileConfiguration config) {
        double base = recipe.getBaseSuccessRate();
        double perLevel = config.getDouble("success-rate-per-forge-level", 0.02);
        int level = data.getForgeLevel();
        
        // 等级超过图纸要求的部分才有加成
        int bonus = Math.max(0, level - recipe.getRequiredForgeLevel());
        double rate = base + (bonus * perLevel);
        
        return Math.min(rate, 0.95); // 上限95%
    }

    /**
     * 构建产物物品（只返回 RPGItems 物品）
     */
    public static ItemStack buildResult(ForgeRecipe recipe, GuangDianForge plugin) {
        var hook = plugin.getRPGItemsHook();
        if (hook == null || !hook.isEnabled()) {
            plugin.getLogger().warning("RPGItems 未启用，无法锻造物品: " + recipe.getResultRPGItem());
            return null;
        }
        
        ItemStack result = hook.getRPGItem(recipe.getResultRPGItem());
        if (result == null) {
            plugin.getLogger().warning("无法获取 RPGItems 物品: " + recipe.getResultRPGItem());
            return null;
        }
        
        return result;
    }

    /**
     * 添加锻造经验并检测升级
     */
    public static void addExp(PlayerForgeData data, long amount, Player player, GuangDianForge plugin) {
        data.setForgeExp(data.getForgeExp() + amount);
        
        int oldLevel = data.getForgeLevel();
        
        // 检查升级
        var section = plugin.getConfig().getConfigurationSection("level-thresholds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    long threshold = section.getLong(key);
                    
                    if (data.getForgeExp() >= threshold && level > data.getForgeLevel()) {
                        data.setForgeLevel(level);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        if (data.getForgeLevel() > oldLevel) {
            player.sendMessage(Component.text("恭喜! 你的锻造等级提升到 " + data.getForgeLevel(), NamedTextColor.GOLD));
        }
    }
}