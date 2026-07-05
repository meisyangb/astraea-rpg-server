package cn.guangdian.classsystem.manager;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.data.ClassDataHandler;
import cn.guangdian.classsystem.enums.TierAttributePoints;
import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.PlayerClassData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 属性点管理器 - 使用枚举定义阶位属性点
 * 
 * 属性点获取规则：
 * - 阶位提升时获得属性点（使用 TierAttributePoints 枚举）
 * - 不通过转职获得属性点
 * - 阶位越高，属性点越多
 */
public class AttributeManager {
    
    private final GuangDianClass plugin;
    private final ClassDataHandler dataHandler;
    
    private final Map<AttributeType, AttributeConfig> attributeConfigs;
    
    public AttributeManager(GuangDianClass plugin, ClassDataHandler dataHandler) {
        this.plugin = plugin;
        this.dataHandler = dataHandler;
        this.attributeConfigs = new HashMap<>();
        loadConfig();
    }
    
    private void loadConfig() {
        // 加载阶位属性点配置
        TierAttributePoints.loadFromConfig(plugin);
        
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("attributes");
        if (config == null) {
            setDefaultAttributeConfigs();
            return;
        }
        
        ConfigurationSection typesSection = config.getConfigurationSection("types");
        if (typesSection != null) {
            for (AttributeType type : AttributeType.values()) {
                ConfigurationSection typeSection = typesSection.getConfigurationSection(type.getId());
                if (typeSection != null) {
                    AttributeConfig attrConfig = new AttributeConfig();
                    attrConfig.healthPerPoint = typeSection.getDouble("health", 0);
                    attrConfig.attackPerPoint = typeSection.getDouble("attack", 0);
                    attrConfig.defensePerPoint = typeSection.getDouble("defense", 0);
                    attrConfig.critChancePerPoint = typeSection.getDouble("crit-chance", 0);
                    attrConfig.critDamagePerPoint = typeSection.getDouble("crit-damage", 0);
                    attrConfig.dodgePerPoint = typeSection.getDouble("dodge", 0);
                    attrConfig.manaPerPoint = typeSection.getDouble("mana", 0);
                    attributeConfigs.put(type, attrConfig);
                }
            }
        } else {
            setDefaultAttributeConfigs();
        }
    }
    
    private void setDefaultAttributeConfigs() {
        AttributeConfig strength = new AttributeConfig();
        strength.attackPerPoint = 2.0;
        strength.critDamagePerPoint = 0.5;
        attributeConfigs.put(AttributeType.STRENGTH, strength);
        
        AttributeConfig vitality = new AttributeConfig();
        vitality.healthPerPoint = 50.0;
        vitality.defensePerPoint = 1.0;
        attributeConfigs.put(AttributeType.VITALITY, vitality);
        
        AttributeConfig agility = new AttributeConfig();
        agility.critChancePerPoint = 0.2;
        agility.dodgePerPoint = 0.1;
        attributeConfigs.put(AttributeType.AGILITY, agility);
        
        AttributeConfig intelligence = new AttributeConfig();
        intelligence.manaPerPoint = 20.0;
        intelligence.attackPerPoint = 1.5;
        attributeConfigs.put(AttributeType.INTELLIGENCE, intelligence);
        
        AttributeConfig luck = new AttributeConfig();
        luck.critDamagePerPoint = 1.0;
        luck.critChancePerPoint = 0.1;
        attributeConfigs.put(AttributeType.LUCK, luck);
    }
    
    public boolean allocateAttribute(Player player, AttributeType type, int points) {
        if (points <= 0) return false;
        
        PlayerClassData data = dataHandler.getPlayerData(player.getUniqueId());
        if (data == null) return false;
        
        if (data.getAvailableAttributePoints() < points) return false;
        
        return data.allocateAttribute(type, points);
    }
    
    public boolean deallocateAttribute(Player player, AttributeType type, int points) {
        if (points <= 0) return false;
        
        PlayerClassData data = dataHandler.getPlayerData(player.getUniqueId());
        if (data == null) return false;
        
        return data.deallocateAttribute(type, points);
    }
    
    public void resetAttributes(Player player) {
        PlayerClassData data = dataHandler.getPlayerData(player.getUniqueId());
        if (data == null) return;
        
        data.resetAttributes();
    }
    
    /**
     * 阶位提升时给予属性点（使用枚举）
     */
    public void grantLevelUpPoints(Player player, int newTier) {
        PlayerClassData data = dataHandler.getPlayerData(player.getUniqueId());
        if (data == null) return;
        
        // 使用枚举获取该阶位的属性点数量
        int points = TierAttributePoints.getPointsByTier(newTier);
        data.addAttributePoints(points);
    }
    
    /**
     * 转职不给属性点（已移除）
     * @deprecated 转职不再给予属性点，只通过阶位提升获得
     */
    @Deprecated
    public void grantAdvancementPoints(Player player, int advancementLevel) {
        // 转职不给属性点
    }
    
    /**
     * 计算玩家当前阶位应有的总属性点
     */
    public int calculateTotalPointsForTier(int tier) {
        return TierAttributePoints.getTotalPointsForTier(tier);
    }
    
    /**
     * 同步玩家属性点到正确数量
     * 用于修复属性点数量不正确的情况
     */
    public void syncAttributePoints(Player player) {
        PlayerClassData data = dataHandler.getPlayerData(player.getUniqueId());
        if (data == null) return;
        
        int correctTotal = calculateTotalPointsForTier(data.getTier());
        int currentTotal = data.getAttributePoints();
        
        if (currentTotal != correctTotal) {
            data.setAttributePoints(correctTotal);
        }
    }
    
    public void grantAttributePoints(Player player, int points) {
        PlayerClassData data = dataHandler.getPlayerData(player.getUniqueId());
        if (data == null) return;
        
        data.addAttributePoints(points);
    }
    
    public AttributeBonus calculateTotalBonus(Player player) {
        return calculateTotalBonus(player.getUniqueId());
    }
    
    public AttributeBonus calculateTotalBonus(UUID playerId) {
        PlayerClassData data = dataHandler.getPlayerData(playerId);
        if (data == null) return new AttributeBonus();
        
        AttributeBonus total = new AttributeBonus();
        
        for (AttributeType type : AttributeType.values()) {
            int points = data.getAllocatedAttribute(type);
            if (points > 0) {
                AttributeConfig config = attributeConfigs.get(type);
                if (config != null) {
                    total.health += config.healthPerPoint * points;
                    total.attack += config.attackPerPoint * points;
                    total.defense += config.defensePerPoint * points;
                    total.critChance += config.critChancePerPoint * points;
                    total.critDamage += config.critDamagePerPoint * points;
                    total.dodge += config.dodgePerPoint * points;
                    total.mana += config.manaPerPoint * points;
                }
            }
        }
        
        return total;
    }
    
    public int getAvailablePoints(Player player) {
        PlayerClassData data = dataHandler.getPlayerData(player.getUniqueId());
        return data != null ? data.getAvailableAttributePoints() : 0;
    }
    
    public int getAllocatedPoints(Player player, AttributeType type) {
        PlayerClassData data = dataHandler.getPlayerData(player.getUniqueId());
        return data != null ? data.getAllocatedAttribute(type) : 0;
    }
    
    public int getTotalAllocatedPoints(Player player) {
        PlayerClassData data = dataHandler.getPlayerData(player.getUniqueId());
        return data != null ? data.getTotalAllocatedPoints() : 0;
    }
    
    public void reload() {
        attributeConfigs.clear();
        loadConfig();
    }
    
    public static class AttributeConfig {
        public double healthPerPoint = 0;
        public double attackPerPoint = 0;
        public double defensePerPoint = 0;
        public double critChancePerPoint = 0;
        public double critDamagePerPoint = 0;
        public double dodgePerPoint = 0;
        public double manaPerPoint = 0;
    }
    
    public static class AttributeBonus {
        public double health = 0;
        public double attack = 0;
        public double defense = 0;
        public double critChance = 0;
        public double critDamage = 0;
        public double dodge = 0;
        public double mana = 0;
    }
}
