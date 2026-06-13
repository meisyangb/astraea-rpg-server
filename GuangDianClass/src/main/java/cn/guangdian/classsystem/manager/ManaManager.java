package cn.guangdian.classsystem.manager;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魔力值管理器
 * 
 * 管理玩家的魔力值：
 * - 计算最大魔力值（基于智力属性）
 * - 消耗魔力值
 * - 恢复魔力值
 * - 显示魔力值
 */
public class ManaManager {

    private final GuangDianClass plugin;
    private final Map<UUID, Double> currentMana = new ConcurrentHashMap<>();
    private final Map<UUID, Double> maxMana = new ConcurrentHashMap<>();
    
    // 魔力值恢复速率（每秒恢复的魔力值）
    private static final double MANA_REGEN_RATE = 1.0;
    // 基础魔力值
    private static final double BASE_MANA = 50.0;

    public ManaManager(GuangDianClass plugin) {
        this.plugin = plugin;
        startManaRegenTask();
    }

    /**
     * 获取玩家的当前魔力值
     */
    public double getCurrentMana(Player player) {
        return currentMana.getOrDefault(player.getUniqueId(), 0.0);
    }

    /**
     * 获取玩家的最大魔力值
     */
    public double getMaxMana(Player player) {
        // 如果已经计算过，直接返回
        Double cached = maxMana.get(player.getUniqueId());
        if (cached != null) {
            return cached;
        }
        
        // 计算最大魔力值
        double max = calculateMaxMana(player);
        maxMana.put(player.getUniqueId(), max);
        return max;
    }

    /**
     * 计算玩家的最大魔力值
     * 
     * 公式: 初始魔力值 + 职业魔力值
     */
    public double calculateMaxMana(Player player) {
        PlayerClassData classData = plugin.getPlayerData(player);
        if (classData == null) {
            return BASE_MANA;
        }
        
        // 获取职业配置
        cn.guangdian.classsystem.model.GameClass gameClass = 
            plugin.getClassManager().getClass(classData.getClassId());
        
        if (gameClass == null) {
            return BASE_MANA;
        }
        
        // 获取职业魔力值
        Double classMana = gameClass.getStats().get("mana");
        double manaValue = (classMana != null) ? classMana : 0.0;
        
        // 计算最大魔力值 = 初始魔力值 + 职业魔力值
        return BASE_MANA + manaValue;
    }

    /**
     * 设置玩家的当前魔力值
     */
    public void setCurrentMana(Player player, double mana) {
        double max = getMaxMana(player);
        double current = Math.max(0, Math.min(mana, max));
        currentMana.put(player.getUniqueId(), current);
    }

    /**
     * 消耗魔力值
     * 
     * @return 是否消耗成功
     */
    public boolean consumeMana(Player player, double amount) {
        double current = getCurrentMana(player);
        
        if (current < amount) {
            // 魔力值不足
            return false;
        }
        
        setCurrentMana(player, current - amount);
        return true;
    }

    /**
     * 恢复魔力值
     */
    public void restoreMana(Player player, double amount) {
        double current = getCurrentMana(player);
        setCurrentMana(player, current + amount);
    }

    /**
     * 检查玩家是否有足够的魔力值
     */
    public boolean hasEnoughMana(Player player, double amount) {
        return getCurrentMana(player) >= amount;
    }

    /**
     * 初始化玩家的魔力值
     */
    public void initializeMana(Player player) {
        double max = calculateMaxMana(player);
        maxMana.put(player.getUniqueId(), max);
        
        // 如果是第一次初始化，设置为最大值；否则确保当前魔力值不超过最大值
        currentMana.putIfAbsent(player.getUniqueId(), max);
        double current = currentMana.get(player.getUniqueId());
        if (current > max) {
            currentMana.put(player.getUniqueId(), max);
        }
    }

    /**
     * 更新玩家的最大魔力值（当属性改变时）
     */
    public void updateMaxMana(Player player) {
        double newMax = calculateMaxMana(player);
        double oldMax = maxMana.getOrDefault(player.getUniqueId(), newMax);
        
        maxMana.put(player.getUniqueId(), newMax);
        
        // 如果最大魔力值增加了，按比例增加当前魔力值
        if (newMax > oldMax) {
            double current = getCurrentMana(player);
            double ratio = current / oldMax;
            setCurrentMana(player, newMax * ratio);
        }
        // 如果最大魔力值减少了，确保当前魔力值不超过最大值
        else if (newMax < oldMax) {
            double current = getCurrentMana(player);
            if (current > newMax) {
                setCurrentMana(player, newMax);
            }
        }
    }

    /**
     * 清理玩家的魔力值数据
     */
    public void clearManaData(Player player) {
        currentMana.remove(player.getUniqueId());
        maxMana.remove(player.getUniqueId());
    }

    /**
     * 显示魔力值信息
     */
    public void displayManaInfo(Player player) {
        double current = getCurrentMana(player);
        double max = getMaxMana(player);
        double percentage = (current / max) * 100;
        
        player.sendMessage(Component.text("§b━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(Component.text("§d§l魔力值信息"));
        player.sendMessage(Component.text("§7当前魔力: §b" + String.format("%.1f", current) + 
            " §7/ §b" + String.format("%.1f", max)));
        player.sendMessage(Component.text("§7魔力百分比: §e" + String.format("%.1f%%", percentage)));
        player.sendMessage(Component.text("§7恢复速率: §a" + MANA_REGEN_RATE + " §7/ 秒"));
        player.sendMessage(Component.text("§b━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * 获取ActionBar格式的魔力值显示
     */
    public String getActionBarMana(Player player) {
        double current = getCurrentMana(player);
        double max = getMaxMana(player);
        double percentage = (current / max) * 100;
        
        // 创建魔力条
        StringBuilder bar = new StringBuilder();
        int filledBars = (int) (percentage / 10);
        int emptyBars = 10 - filledBars;
        
        bar.append("§d魔力: ");
        bar.append("§b");
        for (int i = 0; i < filledBars; i++) {
            bar.append("█");
        }
        bar.append("§7");
        for (int i = 0; i < emptyBars; i++) {
            bar.append("█");
        }
        bar.append(" §f").append(String.format("%.0f", current)).append("/").append(String.format("%.0f", max));
        
        return bar.toString();
    }

    /**
     * 启动魔力值恢复任务
     */
    private void startManaRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // 恢复魔力值
                    restoreMana(player, MANA_REGEN_RATE);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每秒执行一次
    }
}
