package cn.guangdian.expcontrol.service;

import cn.guangdian.expcontrol.GuangDianExpControl;
import cn.guangdian.expcontrol.api.ExpControlService;
import org.bukkit.entity.Player;

/**
 * 经验控制服务实现
 */
public class ExpControlServiceImpl implements ExpControlService {
    
    private final GuangDianExpControl plugin;
    
    public ExpControlServiceImpl(GuangDianExpControl plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public int giveExp(Player player, int amount) {
        return giveExp(player, amount, "system");
    }
    
    @Override
    public int giveExp(Player player, int amount, String source) {
        if (player == null || amount <= 0) {
            return 0;
        }
        
        // 直接给予经验，绕过拦截
        int beforeExp = getTotalExp(player);
        player.giveExp(amount);
        int afterExp = getTotalExp(player);
        int actualGiven = afterExp - beforeExp;
        
        // 记录日志
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format("[经验发放] 玩家: %s, 数量: %d, 来源: %s", 
                player.getName(), actualGiven, source));
        }
        
        return actualGiven;
    }
    
    @Override
    public void setExp(Player player, int amount) {
        if (player == null || amount < 0) {
            return;
        }
        
        // 设置总经验
        player.setTotalExperience(amount);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format("[经验设置] 玩家: %s, 数量: %d", 
                player.getName(), amount));
        }
    }
    
    @Override
    public void setLevel(Player player, int level) {
        if (player == null || level < 0) {
            return;
        }
        
        player.setLevel(level);
        player.setExp(0);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format("[等级设置] 玩家: %s, 等级: %d", 
                player.getName(), level));
        }
    }
    
    @Override
    public int getExp(Player player) {
        if (player == null) {
            return 0;
        }
        return getTotalExp(player);
    }
    
    @Override
    public int getLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return player.getLevel();
    }
    
    @Override
    public int getExpToLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return player.getExpToLevel();
    }
    
    @Override
    public void resetExp(Player player) {
        if (player == null) {
            return;
        }
        
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format("[经验重置] 玩家: %s", player.getName()));
        }
    }
    
    @Override
    public int takeExp(Player player, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }
        
        int currentExp = getTotalExp(player);
        int toTake = Math.min(amount, currentExp);
        
        if (toTake > 0) {
            player.setTotalExperience(currentExp - toTake);
        }
        
        return toTake;
    }
    
    @Override
    public boolean canGainExp(Player player) {
        if (player == null) {
            return false;
        }
        
        // 检查玩家是否有绕过权限
        return player.hasPermission("guangdian.expcontrol.bypass");
    }
    
    @Override
    public int calculateTotalExpForLevel(int level) {
        // Minecraft 1.21+ 经验公式
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }
    
    /**
     * 获取玩家的总经验值
     */
    private int getTotalExp(Player player) {
        return player.getTotalExperience();
    }
}
