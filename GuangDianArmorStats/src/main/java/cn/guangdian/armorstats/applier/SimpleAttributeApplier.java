package cn.guangdian.armorstats.applier;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.config.AttributeApplyLogConfig;
import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简化属性应用器
 * 
 * 移动速度使用 ADD_SCALAR (MULTIPLY_BASE) modifier
 * 这是成熟 RPG 插件的标准做法：
 * - baseValue 保持默认 0.1 不变
 * - 通过 ADD_SCALAR modifier 添加百分比加成
 * - 原版装备的 modifier 不会冲突，因为各自独立
 * - 装备变化时原版系统会自动重新计算，不会覆盖我们的 modifier
 */
public class SimpleAttributeApplier {

    private final GuangDianArmorStats plugin;
    private final AttributeApplyLogConfig logConfig;
    
    private final NamespacedKey healthKey;
    private final NamespacedKey speedKey;
    
    private static final double DEFAULT_MAX_HEALTH = 20.0;
    private static final double DEFAULT_MOVE_SPEED = 0.1;
    private static final double MAX_HEALTH_LIMIT = 2000000.0;
    
    // 速度监测
    private final Map<UUID, Double> expectedSpeedPercent = new ConcurrentHashMap<>();
    private int monitorTaskId = -1;
    
    public SimpleAttributeApplier(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        this.logConfig = AttributeApplyLogConfig.getInstance();
        this.healthKey = new NamespacedKey(plugin, "max_health");
        this.speedKey = new NamespacedKey(plugin, "move_speed");
    }
    
    /**
     * 启动速度监测任务
     * 每 20 tick 检查一次，确保 modifier 没有丢失
     */
    public void startSpeedMonitor() {
        if (monitorTaskId != -1) return;
        
        monitorTaskId = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                Double expectedPercent = expectedSpeedPercent.get(uuid);
                if (expectedPercent == null) continue;
                
                // 检查我们的 modifier 是否还在
                AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
                if (speedAttr == null) continue;
                
                boolean hasOurModifier = false;
                for (AttributeModifier mod : speedAttr.getModifiers()) {
                    if (mod.getKey().equals(speedKey)) {
                        hasOurModifier = true;
                        break;
                    }
                }
                
                if (!hasOurModifier && expectedPercent > 0) {
                    plugin.getLogger().warning("[速度监测] " + player.getName() + 
                        " 的速度 modifier 丢失! 重新添加 (" + expectedPercent + "%)");
                    // 先清除可能残留的旧 modifier（避免竞态条件导致重复添加）
                    removeAllModifiers(speedAttr, speedKey);
                    // 重新添加 modifier
                    double modifierValue = expectedPercent / 100.0;
                    AttributeModifier modifier = new AttributeModifier(
                        speedKey,
                        modifierValue,
                        AttributeModifier.Operation.ADD_SCALAR,
                        EquipmentSlotGroup.ANY
                    );
                    speedAttr.addModifier(modifier);
                }
            }
        }, 20L, 20L).getTaskId();
        
        plugin.getLogger().info("[速度监测] 速度监测任务已启动");
    }
    
    public void stopSpeedMonitor() {
        if (monitorTaskId != -1) {
            org.bukkit.Bukkit.getScheduler().cancelTask(monitorTaskId);
            monitorTaskId = -1;
        }
    }
    
    public void applyAll(Player player, PlayerStats stats) {
        applyMaxHealth(player, stats);
        applyMoveSpeed(player, stats);
    }
    
    public void applyMaxHealth(Player player, PlayerStats stats) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        
        removeAllModifiers(attr, healthKey);
        attr.setBaseValue(DEFAULT_MAX_HEALTH);
        
        double bonusHealth = stats.getMaxHealth();
        if (bonusHealth > 0) {
            double effectiveHealth = Math.min(DEFAULT_MAX_HEALTH + bonusHealth, MAX_HEALTH_LIMIT);
            double modifierValue = effectiveHealth - DEFAULT_MAX_HEALTH;
            
            AttributeModifier modifier = new AttributeModifier(
                healthKey,
                modifierValue,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.ANY
            );
            attr.addModifier(modifier);
        }
        
        logConfig.logApply(player.getName() + 
            " 生命上限: " + attr.getValue() + 
            " (基础:" + DEFAULT_MAX_HEALTH + " + 加成:" + bonusHealth + ")");
    }
    
    /**
     * 应用移动速度
     * 
     * 使用 ADD_SCALAR (MULTIPLY_BASE) modifier
     * 
     * 计算公式：
     *   最终速度 = baseValue + (baseValue * modifierValue)
     *   即：最终速度 = baseValue * (1 + modifierValue)
     * 
     * 例如：moveSpeedPercent = 7%
     *   modifierValue = 0.07
     *   最终速度 = 0.1 * (1 + 0.07) = 0.107
     * 
     * 优势：
     * - 不修改 baseValue，原版装备 modifier 不会冲突
     * - 装备变化时原版系统自动重新计算，不会覆盖我们的 modifier
     * - 与生命值的 ADD_NUMBER modifier 方式一致
     */
    public void applyMoveSpeed(Player player, PlayerStats stats) {
        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        
        // 1. 清除旧的 modifier
        removeAllModifiers(attr, speedKey);
        
        // 2. 确保 baseValue 是默认值
        attr.setBaseValue(DEFAULT_MOVE_SPEED);
        
        // 3. 计算并添加新 modifier
        double moveSpeedPercent = stats.getMoveSpeedPercent();
        if (moveSpeedPercent > 0) {
            // ADD_SCALAR: modifierValue 是百分比小数
            // 例如 7% → 0.07
            double modifierValue = moveSpeedPercent / 100.0;
            
            AttributeModifier modifier = new AttributeModifier(
                speedKey,
                modifierValue,
                AttributeModifier.Operation.ADD_SCALAR,
                EquipmentSlotGroup.ANY
            );
            attr.addModifier(modifier);
            
            expectedSpeedPercent.put(player.getUniqueId(), moveSpeedPercent);
        } else {
            expectedSpeedPercent.remove(player.getUniqueId());
        }
        
        logConfig.logApply(player.getName() + 
            " 移动速度: " + attr.getValue() + 
            " (基础:" + DEFAULT_MOVE_SPEED + " + " + moveSpeedPercent + "%)" +
            " walkSpeed: " + player.getWalkSpeed());
    }
    
    /**
     * 清除玩家所有属性 modifier
     */
    public void clearAll(Player player) {
        // 清除生命值 modifier
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            removeAllModifiers(healthAttr, healthKey);
            healthAttr.setBaseValue(DEFAULT_MAX_HEALTH);
        }
        
        // 清除移动速度 modifier
        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            removeAllModifiers(speedAttr, speedKey);
            speedAttr.setBaseValue(DEFAULT_MOVE_SPEED);
        }
        
        expectedSpeedPercent.remove(player.getUniqueId());
    }
    
    /**
     * 完全重置玩家所有属性（用于玩家退出）
     */
    public void resetAll(Player player) {
        UUID uuid = player.getUniqueId();
        
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            removeAllModifiers(healthAttr, healthKey);
            healthAttr.setBaseValue(DEFAULT_MAX_HEALTH);
        }
        
        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            removeAllModifiers(speedAttr, speedKey);
            speedAttr.setBaseValue(DEFAULT_MOVE_SPEED);
        }
        
        expectedSpeedPercent.remove(uuid);
    }
    
    private void removeAllModifiers(AttributeInstance attr, NamespacedKey key) {
        List<AttributeModifier> toRemove = new ArrayList<>();
        for (AttributeModifier modifier : attr.getModifiers()) {
            if (modifier.getKey().equals(key)) {
                toRemove.add(modifier);
            }
        }
        for (AttributeModifier modifier : toRemove) {
            attr.removeModifier(modifier);
            logConfig.logAttributeClear("移除 modifier: " + modifier.getAmount());
        }
    }
}
