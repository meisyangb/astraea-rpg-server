package cn.guangdian.classsystem.manager;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.enums.Sequence;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 魔力管理器 - 管理玩家魔力系统
 * 
 * 魔力基于序列计算：
 * - 序列9: 30 魔力
 * - 序列8: 50 魔力
 * - 序列7: 80 魔力
 * - 序列6: 100 魔力
 * - 序列5: 150 魔力
 * - 序列4: 200 魔力
 * - 序列3: 300 魔力
 * - 序列2: 400 魔力
 * - 序列1: 500 魔力
 * - 序列0: 1000 魔力（真神）
 */
public class ManaManager {
    
    private final GuangDianClass plugin;
    private final Map<UUID, Double> playerMana;
    private final Map<UUID, Long> lastRegenTime;
    
    public ManaManager(GuangDianClass plugin) {
        this.plugin = plugin;
        this.playerMana = new HashMap<>();
        this.lastRegenTime = new HashMap<>();
    }
    
    /**
     * 获取序列对应的最大魔力值
     */
    public int getMaxManaBySequence(Sequence sequence) {
        switch (sequence) {
            case SEQ_0: return 1000;
            case SEQ_1: return 500;
            case SEQ_2: return 400;
            case SEQ_3: return 300;
            case SEQ_4: return 200;
            case SEQ_5: return 150;
            case SEQ_6: return 100;
            case SEQ_7: return 80;
            case SEQ_8: return 50;
            case SEQ_9: return 30;
            default: return 30;
        }
    }
    
    /**
     * 获取玩家的最大魔力值
     */
    public int getMaxMana(Player player) {
        PlayerClassData data = plugin.getPlayerData(player);
        if (data == null) return 30;
        
        GameClass gameClass = plugin.getClassManager().getClass(data.getClassId());
        if (gameClass == null) return 30;
        
        Sequence sequence = Sequence.fromLevel(gameClass.getSequence());
        return getMaxManaBySequence(sequence);
    }
    
    /**
     * 获取玩家当前魔力值
     */
    public double getMana(Player player) {
        return playerMana.getOrDefault(player.getUniqueId(), (double) getMaxMana(player));
    }
    
    /**
     * 获取玩家当前魔力值（别名方法）
     */
    public double getCurrentMana(Player player) {
        return getMana(player);
    }
    
    /**
     * 检查玩家是否有足够的魔力
     */
    public boolean hasEnoughMana(Player player, int amount) {
        return getMana(player) >= amount;
    }
    
    /**
     * 检查玩家是否有足够的魔力（double版本）
     */
    public boolean hasEnoughMana(Player player, double amount) {
        return getMana(player) >= amount;
    }
    
    /**
     * 设置玩家魔力值
     */
    public void setMana(Player player, double mana) {
        int maxMana = getMaxMana(player);
        double actualMana = Math.max(0, Math.min(mana, maxMana));
        playerMana.put(player.getUniqueId(), actualMana);
    }
    
    /**
     * 消耗魔力
     */
    public boolean consumeMana(Player player, double amount) {
        double currentMana = getMana(player);
        if (currentMana < amount) return false;
        
        setMana(player, currentMana - amount);
        return true;
    }
    
    /**
     * 恢复魔力
     */
    public void restoreMana(Player player, double amount) {
        double currentMana = getMana(player);
        setMana(player, currentMana + amount);
    }
    
    /**
     * 魔力自然恢复（每秒恢复一定比例）
     */
    public void regenMana(Player player) {
        long now = System.currentTimeMillis();
        Long lastRegen = lastRegenTime.get(player.getUniqueId());
        
        // 每秒恢复一次
        if (lastRegen != null && now - lastRegen < 1000) return;
        
        lastRegenTime.put(player.getUniqueId(), now);
        
        // 恢复 5% 最大魔力
        int maxMana = getMaxMana(player);
        double regenAmount = maxMana * 0.05;
        restoreMana(player, regenAmount);
    }
    
    /**
     * 重置玩家魔力（满魔力）
     */
    public void resetMana(Player player) {
        setMana(player, getMaxMana(player));
    }
    
    /**
     * 清除玩家魔力数据
     */
    public void clearMana(Player player) {
        playerMana.remove(player.getUniqueId());
        lastRegenTime.remove(player.getUniqueId());
    }
    
    /**
     * 初始化玩家魔力（登录时调用）
     */
    public void initializeMana(Player player) {
        int maxMana = getMaxMana(player);
        playerMana.put(player.getUniqueId(), (double) maxMana);
        lastRegenTime.put(player.getUniqueId(), System.currentTimeMillis());
    }
}