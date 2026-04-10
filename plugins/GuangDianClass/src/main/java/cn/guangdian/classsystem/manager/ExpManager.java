package cn.guangdian.classsystem.manager;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.data.ClassDataHandler;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ExpManager {
    
    private final GuangDianClass plugin;
    private final ClassManager classManager;
    private final ClassDataHandler dataHandler;
    
    public ExpManager(GuangDianClass plugin, ClassManager classManager, ClassDataHandler dataHandler) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.dataHandler = dataHandler;
    }
    
    public boolean addExp(UUID playerId, long amount) {
        PlayerClassData data = dataHandler.getPlayerData(playerId);
        if (data == null) return false;
        
        data.addExp(amount);
        
        checkAndProcessTierUp(playerId, data);
        
        return true;
    }
    
    public boolean addExp(Player player, long amount) {
        return addExp(player.getUniqueId(), amount);
    }
    
    public boolean setExp(UUID playerId, long amount) {
        PlayerClassData data = dataHandler.getPlayerData(playerId);
        if (data == null) return false;
        
        data.setExp(amount);
        data.setLastUpdateTime(System.currentTimeMillis());
        
        return true;
    }
    
    public boolean setExp(Player player, long amount) {
        return setExp(player.getUniqueId(), amount);
    }
    
    private void checkAndProcessTierUp(UUID playerId, PlayerClassData data) {
        while (classManager.canTierUp(data)) {
            processTierUp(playerId, data);
        }
    }
    
    private void processTierUp(UUID playerId, PlayerClassData data) {
        int oldTier = data.getTier();
        int newTier = oldTier + 1;
        
        long requiredExp = classManager.getExpRequiredForTier(newTier);
        if (data.getExp() < requiredExp) return;
        
        data.setExp(data.getExp() - requiredExp);
        data.setTier(newTier);
        data.setLastUpdateTime(System.currentTimeMillis());
        
        plugin.getLogger().info("玩家 " + playerId + " 阶位提升至 " + newTier + " 阶");
    }
    
    public double getExpProgress(PlayerClassData data) {
        int currentTier = data.getTier();
        int maxTier = plugin.getConfig().getInt("settings.max-tier", 9);
        
        if (currentTier >= maxTier) return 1.0;
        
        long currentExp = data.getExp();
        long requiredExp = classManager.getExpRequiredForNextTier(currentTier);
        
        if (requiredExp <= 0) return 1.0;
        
        return Math.min(1.0, (double) currentExp / requiredExp);
    }
    
    public String getAdvancementStatus(PlayerClassData data) {
        GameClass currentClass = classManager.getClass(data.getClassId());
        if (currentClass == null) return "未知";
        
        int advancementLevel = data.getAdvancementLevel();
        
        return switch (advancementLevel) {
            case 0 -> "未转职";
            case 1 -> "一转";
            case 2 -> "二转";
            case 3 -> "三转";
            case 4 -> "神级";
            default -> "未知";
        };
    }
    
    public boolean canAdvance(PlayerClassData data) {
        GameClass currentClass = classManager.getClass(data.getClassId());
        if (currentClass == null) return false;
        
        if (currentClass.getNextClasses().isEmpty()) return false;
        
        int nextAdvancement = data.getAdvancementLevel() + 1;
        int requiredTier = classManager.getAdvancementTier(nextAdvancement);
        
        return data.getTier() >= requiredTier;
    }
}
