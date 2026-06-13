package cn.guangdian.armorstats.source;

import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.parser.PDCAttributeReader;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 装备属性源
 * 从玩家装备获取属性（通过 PDC 直接读取 + 直接调用 GuangDianSocket）
 * 
 * 修复：直接调用 GuangDianSocket 获取宝石属性
 * 优化：添加宝石属性缓存，提升性能
 */
public class EquipmentAttributeSource implements AttributeSource {

    private static final int PRIORITY = 100; // 最高优先级
    
    // 宝石属性缓存：宝石ID → 属性Map
    private static final Map<String, Map<String, AttributeValue>> GEM_CACHE = new ConcurrentHashMap<>();
    
    private boolean enabled = true;
    private Logger logger;

    public EquipmentAttributeSource() {
        // 使用 PDC 直接读取 + 直接调用 GuangDianSocket
    }

    /**
     * 设置日志器
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * 记录日志
     */
    private void log(String message) {
        if (logger != null) {
            logger.info("[EquipmentAttributeSource] " + message);
        }
    }

    @Override
    public String getName() {
        return "装备";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public Map<String, Double> getAttributes(Player player) {
        Map<String, Double> total = new HashMap<>();
        
        PlayerInventory inv = player.getInventory();
        
        // 主手
        addWeaponAttributes(total, inv.getItemInMainHand());
        
        // 副手
        addEquipmentAttributes(total, inv.getItemInOffHand());
        
        // 头盔
        addEquipmentAttributes(total, inv.getHelmet());
        
        // 胸甲
        addEquipmentAttributes(total, inv.getChestplate());
        
        // 护腿
        addEquipmentAttributes(total, inv.getLeggings());
        
        // 靴子
        addEquipmentAttributes(total, inv.getBoots());
        
        return total;
    }

    /**
     * 添加武器属性（主手武器有特殊处理）
     */
    private void addWeaponAttributes(Map<String, Double> total, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        
        // 使用 PDCAttributeReader 读取属性
        Map<String, AttributeValue> attrs = PDCAttributeReader.readFromPDC(item);
        
        // 额外从 GuangDianSocket 获取宝石属性
        Map<String, AttributeValue> gemAttrs = getGemAttributes(item);
        attrs.putAll(gemAttrs);
        
        if (attrs.isEmpty()) {
            return;
        }
        
        // 武器属性
        AttributeValue attackVal = attrs.get("攻击力");
        if (attackVal != null) {
            if (attackVal instanceof AttributeValue.RangeValue) {
                AttributeValue.RangeValue range = (AttributeValue.RangeValue) attackVal;
                mergeAdd(total, "攻击力最小", range.getMin());
                mergeAdd(total, "攻击力最大", range.getMax());
            } else if (attackVal instanceof AttributeValue.SingleValue) {
                AttributeValue.SingleValue single = (AttributeValue.SingleValue) attackVal;
                mergeAdd(total, "攻击力最小", single.getValue());
                mergeAdd(total, "攻击力最大", single.getValue());
            }
        }
        
        // 其他属性
        addCommonAttributes(total, attrs);
    }

    /**
     * 添加装备属性
     */
    private void addEquipmentAttributes(Map<String, Double> total, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        
        // 使用 PDCAttributeReader 读取属性
        Map<String, AttributeValue> attrs = PDCAttributeReader.readFromPDC(item);
        
        // 额外从 GuangDianSocket 获取宝石属性
        Map<String, AttributeValue> gemAttrs = getGemAttributes(item);
        attrs.putAll(gemAttrs);
        
        if (attrs.isEmpty()) {
            return;
        }
        
        // 防御属性
        AttributeValue defenseVal = attrs.get("防御力");
        if (defenseVal != null) {
            if (defenseVal instanceof AttributeValue.RangeValue) {
                AttributeValue.RangeValue range = (AttributeValue.RangeValue) defenseVal;
                mergeAdd(total, "防御力最小", range.getMin());
                mergeAdd(total, "防御力最大", range.getMax());
            } else if (defenseVal instanceof AttributeValue.SingleValue) {
                AttributeValue.SingleValue single = (AttributeValue.SingleValue) defenseVal;
                mergeAdd(total, "防御力最小", single.getValue());
                mergeAdd(total, "防御力最大", single.getValue());
            }
        }
        
        // 其他属性
        addCommonAttributes(total, attrs);
    }
    
    /**
     * 从 GuangDianSocket 获取宝石属性
     * 
     * @param item 物品
     * @return 宝石属性映射
     */
    private Map<String, AttributeValue> getGemAttributes(ItemStack item) {
        Map<String, AttributeValue> attrs = new HashMap<>();
        
        if (item == null || !item.hasItemMeta()) {
            return attrs;
        }
        
        try {
            // 获取 GuangDianSocket 插件
            Plugin socketPlugin = Bukkit.getPluginManager().getPlugin("GuangDianSocket");
            if (socketPlugin == null || !socketPlugin.isEnabled()) {
                if (logger != null) {
                    logger.warning("[宝石属性] GuangDianSocket 插件未启用");
                }
                return attrs;
            }
            
            // 获取 GemStorage
            cn.guangdian.socket.GuangDianSocket socket = (cn.guangdian.socket.GuangDianSocket) socketPlugin;
            cn.guangdian.socket.storage.GemStorage gemStorage = socket.getGemStorage();
            
            if (gemStorage == null) {
                if (logger != null) {
                    logger.warning("[宝石属性] GemStorage 为空");
                }
                return attrs;
            }
            
            // 加载宝石数据
            List<cn.guangdian.socket.model.GemData> gems = gemStorage.loadGems(item);
            if (gems == null || gems.isEmpty()) {
                return attrs;
            }
            
            if (logger != null) {
                logger.info("[宝石属性] 找到 " + gems.size() + " 个宝石");
            }
            
            // 聚合所有宝石属性
            for (cn.guangdian.socket.model.GemData gem : gems) {
                String gemId = gem.getItemId();
                
                if (logger != null) {
                    logger.info("[宝石属性] 处理宝石: " + gemId);
                }
                
                // 使用缓存获取宝石属性
                Map<String, AttributeValue> gemAttrs = GEM_CACHE.computeIfAbsent(gemId, id -> {
                    Map<String, AttributeValue> result = new HashMap<>();
                    Map<String, cn.guangdian.socket.model.AttributeValue> socketAttrs = 
                        cn.guangdian.socket.parser.SocketParser.parseGemAttributesById(id);
                    
                    if (logger != null) {
                        logger.info("[宝石属性] 解析宝石 " + id + " 属性: " + socketAttrs.size() + " 个");
                    }
                    
                    for (Map.Entry<String, cn.guangdian.socket.model.AttributeValue> entry : socketAttrs.entrySet()) {
                        String attrName = entry.getKey();
                        cn.guangdian.socket.model.AttributeValue gemValue = entry.getValue();
                        
                        // 正确处理范围值和单值
                        if (gemValue instanceof cn.guangdian.socket.model.AttributeValue.RangeValue) {
                            // 范围值
                            cn.guangdian.socket.model.AttributeValue.RangeValue rangeValue = 
                                (cn.guangdian.socket.model.AttributeValue.RangeValue) gemValue;
                            double min = rangeValue.getMin();
                            double max = rangeValue.getMax();
                            if (min > 0 || max > 0) {
                                result.put(attrName, AttributeValue.ofRange(min, max));
                                if (logger != null) {
                                    logger.info("[宝石属性] " + attrName + ": " + min + "-" + max);
                                }
                            }
                        } else {
                            // 单值
                            double value = gemValue.getValue();
                            if (value > 0) {
                                result.put(attrName, AttributeValue.of(value));
                                if (logger != null) {
                                    logger.info("[宝石属性] " + attrName + ": " + value);
                                }
                            }
                        }
                    }
                    
                    return result;
                });
                
                // 合并到总属性
                for (Map.Entry<String, AttributeValue> entry : gemAttrs.entrySet()) {
                    attrs.merge(entry.getKey(), entry.getValue(), AttributeValue::merge);
                }
            }
            
            if (logger != null && !attrs.isEmpty()) {
                logger.info("[宝石属性] 总属性: " + attrs.size() + " 个");
            }
            
        } catch (Exception e) {
            // 如果出错，返回空Map
            if (logger != null) {
                logger.warning("获取宝石属性失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return attrs;
    }
    
    /**
     * 清除宝石属性缓存
     */
    public static void clearCache() {
        GEM_CACHE.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public static int getCacheSize() {
        return GEM_CACHE.size();
    }

    /**
     * 添加通用属性（所有装备共享）
     */
    private void addCommonAttributes(Map<String, Double> total, Map<String, AttributeValue> attrs) {
        // 生命属性
        addValue(total, attrs, "生命上限", "生命上限");
        addValue(total, attrs, "生命回复", "生命回复");
        
        // 暴击属性
        addValue(total, attrs, "暴击几率", "暴击几率");
        addValue(total, attrs, "暴击伤害", "暴击伤害");
        
        // 生命偷取
        addValue(total, attrs, "吸血几率", "吸血几率");
        addValue(total, attrs, "吸血倍率", "吸血倍率");
        
        // 闪避与格挡
        addValue(total, attrs, "闪避", "闪避");
        addValue(total, attrs, "招架", "招架");
        
        // 移动速度
        addValue(total, attrs, "移动速度", "移动速度");
        
        // 减伤
        addValue(total, attrs, "减伤", "减伤");
        
        // PVP属性
        addRangeValue(total, attrs, "PVP攻击力", "PVP攻击力最小", "PVP攻击力最大");
        addRangeValue(total, attrs, "PVP防御力", "PVP防御力最小", "PVP防御力最大");
        
        // 暴击抵抗
        addValue(total, attrs, "暴击抵抗", "暴击抵抗");
        addValue(total, attrs, "暴伤抵抗", "暴伤抵抗");
        
        // 吸血抵抗
        addValue(total, attrs, "吸血抵抗", "吸血抵抗");
        
        // 护甲系统
        addValue(total, attrs, "护甲值", "护甲值");
        addValue(total, attrs, "护甲强度", "护甲强度");
        addValue(total, attrs, "护甲穿透", "护甲穿透");
        addValue(total, attrs, "防御穿透", "防御穿透");
        
        // 伤害反弹
        addValue(total, attrs, "伤害反弹", "伤害反弹");
        addValue(total, attrs, "反伤比例", "反伤比例");
        
        // 状态效果
        addValue(total, attrs, "中毒", "中毒");
        addValue(total, attrs, "冰冻", "冰冻");
        addValue(total, attrs, "致盲", "致盲");
        addValue(total, attrs, "燃烧", "燃烧");
        addValue(total, attrs, "灼烧", "灼烧");
        
        // 环境抗性
        addValue(total, attrs, "火焰抗性", "火焰抗性");
        addValue(total, attrs, "摔落抗性", "摔落抗性");
        addValue(total, attrs, "溺水抗性", "溺水抗性");
        addValue(total, attrs, "中毒抗性", "中毒抗性");
        addValue(total, attrs, "凋零抗性", "凋零抗性");
        addValue(total, attrs, "岩浆抗性", "岩浆抗性");
        addValue(total, attrs, "魔法抗性", "魔法抗性");
        addValue(total, attrs, "爆炸抗性", "爆炸抗性");
        addValue(total, attrs, "弹射物抗性", "弹射物抗性");
        
        // 其他属性
        addValue(total, attrs, "击退抗性", "击退抗性");
        addValue(total, attrs, "经验加成", "经验加成");
        addValue(total, attrs, "生命恢复", "生命恢复");
        addValue(total, attrs, "躲避反伤", "躲避反伤");
        addValue(total, attrs, "躲避反弹比例", "躲避反弹比例");
    }

    /**
     * 添加单个属性值
     */
    private void addValue(Map<String, Double> total, Map<String, AttributeValue> attrs, 
                          String attrKey, String mapKey) {
        AttributeValue val = attrs.get(attrKey);
        if (val != null) {
            double value = val.getValue();
            if (value > 0) {
                mergeAdd(total, mapKey, value);
            }
        }
    }

    /**
     * 添加范围属性值
     */
    private void addRangeValue(Map<String, Double> total, Map<String, AttributeValue> attrs,
                               String attrKey, String minKey, String maxKey) {
        AttributeValue val = attrs.get(attrKey);
        if (val != null) {
            if (val instanceof AttributeValue.RangeValue) {
                AttributeValue.RangeValue range = (AttributeValue.RangeValue) val;
                if (range.getMin() > 0 || range.getMax() > 0) {
                    mergeAdd(total, minKey, range.getMin());
                    mergeAdd(total, maxKey, range.getMax());
                }
            } else if (val instanceof AttributeValue.SingleValue) {
                AttributeValue.SingleValue single = (AttributeValue.SingleValue) val;
                if (single.getValue() > 0) {
                    mergeAdd(total, minKey, single.getValue());
                    mergeAdd(total, maxKey, single.getValue());
                }
            }
        }
    }

    /**
     * 合并添加属性值
     */
    private void mergeAdd(Map<String, Double> map, String key, double value) {
        map.merge(key, value, Double::sum);
    }

    @Override
    public PlayerStats getPlayerStats(Player player) {
        PlayerStats stats = new PlayerStats();
        Map<String, Double> attrs = getAttributes(player);
        
        // 攻击属性
        Double attackMin = attrs.get("攻击力最小");
        Double attackMax = attrs.get("攻击力最大");
        if (attackMin != null && attackMax != null) {
            stats.setMinAttack(attackMin);
            stats.setMaxAttack(attackMax);
        }
        
        // 防御属性
        Double defenseMin = attrs.get("防御力最小");
        Double defenseMax = attrs.get("防御力最大");
        if (defenseMin != null && defenseMax != null) {
            stats.setDefenseMin(defenseMin);
            stats.setDefenseMax(defenseMax);
        }
        
        // 生命属性
        Double maxHealth = attrs.get("生命上限");
        if (maxHealth != null) {
            stats.setMaxHealth(maxHealth);
        }
        Double healthRegen = attrs.get("生命回复");
        if (healthRegen != null) {
            stats.setHealthRegen(healthRegen);
        }
        
        // 暴击属性
        Double critChance = attrs.get("暴击几率");
        if (critChance != null) {
            stats.setCritChancePercent(critChance);
        }
        Double critDamage = attrs.get("暴击伤害");
        if (critDamage != null) {
            stats.setCritDamagePercent(critDamage);
        }
        
        // 暴击抵抗
        Double critResist = attrs.get("暴击抵抗");
        if (critResist != null) {
            stats.setCritResistPercent(critResist);
        }
        Double critDamageResist = attrs.get("暴伤抵抗");
        if (critDamageResist != null) {
            stats.setCritDamageResistPercent(critDamageResist);
        }
        
        // 生命偷取
        Double lifestealChance = attrs.get("吸血几率");
        if (lifestealChance != null) {
            stats.setLifestealPercent(lifestealChance);
        }
        Double lifestealMultiplier = attrs.get("吸血倍率");
        if (lifestealMultiplier != null) {
            stats.setLifestealMultiplier(lifestealMultiplier);
        }
        Double lifestealResist = attrs.get("吸血抵抗");
        if (lifestealResist != null) {
            stats.setLifestealResistPercent(lifestealResist);
        }
        
        // 闪避与格挡
        Double dodgeChance = attrs.get("闪避");
        if (dodgeChance != null) {
            stats.setDodgePercent(dodgeChance);
        }
        Double parryChance = attrs.get("招架");
        if (parryChance != null) {
            stats.setParryPercent(parryChance);
        }
        
        // 移动速度
        Double moveSpeed = attrs.get("移动速度");
        if (moveSpeed != null) {
            stats.setMoveSpeedPercent(moveSpeed);
        }
        
        // 减伤
        Double damageReduction = attrs.get("减伤");
        if (damageReduction != null) {
            stats.setDamageReductionBonus(damageReduction);
        }
        
        // PVP属性
        Double pvpAttackMin = attrs.get("PVP攻击力最小");
        Double pvpAttackMax = attrs.get("PVP攻击力最大");
        if (pvpAttackMin != null && pvpAttackMax != null) {
            stats.setPvpMinAttack(pvpAttackMin);
            stats.setPvpMaxAttack(pvpAttackMax);
        }
        Double pvpDefenseMin = attrs.get("PVP防御力最小");
        Double pvpDefenseMax = attrs.get("PVP防御力最大");
        if (pvpDefenseMin != null && pvpDefenseMax != null) {
            stats.setPvpDefenseMin(pvpDefenseMin);
            stats.setPvpDefenseMax(pvpDefenseMax);
        }
        
        // 护甲系统
        Double armor = attrs.get("护甲值");
        if (armor != null) {
            stats.setArmorPercent(armor);
        }
        Double armorStrength = attrs.get("护甲强度");
        if (armorStrength != null) {
            stats.setArmorStrength(armorStrength);
        }
        Double armorPenetration = attrs.get("护甲穿透");
        if (armorPenetration != null) {
            stats.setArmorPenetration(armorPenetration);
        }
        Double defensePenetration = attrs.get("防御穿透");
        if (defensePenetration != null) {
            stats.setDefensePenetration(defensePenetration);
        }
        
        // 伤害反弹
        Double damageReflect = attrs.get("伤害反弹");
        if (damageReflect != null) {
            stats.setDamageReflectPercent(damageReflect);
        }
        Double reflectRatio = attrs.get("反伤比例");
        if (reflectRatio != null) {
            stats.setReflectPercent(reflectRatio);
        }
        
        // 状态效果
        Double poisonChance = attrs.get("中毒");
        if (poisonChance != null) {
            stats.setPoisonPercent(poisonChance);
        }
        Double freezeChance = attrs.get("冰冻");
        if (freezeChance != null) {
            stats.setFreezePercent(freezeChance);
        }
        Double blindChance = attrs.get("致盲");
        if (blindChance != null) {
            stats.setBlindPercent(blindChance);
        }
        Double burnChance = attrs.get("燃烧");
        if (burnChance != null) {
            stats.setBurnPercent(burnChance);
        }
        Double scorchChance = attrs.get("灼烧");
        if (scorchChance != null) {
            stats.setScorchPercent(scorchChance);
        }
        
        // 环境抗性
        Double fireResist = attrs.get("火焰抗性");
        if (fireResist != null) {
            stats.setFireResistPercent(fireResist);
        }
        Double fallResist = attrs.get("摔落抗性");
        if (fallResist != null) {
            stats.setFallResistPercent(fallResist);
        }
        Double drowningResist = attrs.get("溺水抗性");
        if (drowningResist != null) {
            stats.setDrowningResistPercent(drowningResist);
        }
        Double poisonResist = attrs.get("中毒抗性");
        if (poisonResist != null) {
            stats.setPoisonResistPercent(poisonResist);
        }
        Double witherResist = attrs.get("凋零抗性");
        if (witherResist != null) {
            stats.setWitherResistPercent(witherResist);
        }
        Double lavaResist = attrs.get("岩浆抗性");
        if (lavaResist != null) {
            stats.setLavaResistPercent(lavaResist);
        }
        Double magicResist = attrs.get("魔法抗性");
        if (magicResist != null) {
            stats.setMagicResistPercent(magicResist);
        }
        Double explosionResist = attrs.get("爆炸抗性");
        if (explosionResist != null) {
            stats.setExplosionResistPercent(explosionResist);
        }
        Double projectileResist = attrs.get("弹射物抗性");
        if (projectileResist != null) {
            stats.setProjectileResistPercent(projectileResist);
        }
        
        // 其他属性
        Double expBonus = attrs.get("经验加成");
        if (expBonus != null) {
            stats.setExpBonusPercent(expBonus);
        }
        Double healthRegenPercent = attrs.get("生命恢复");
        if (healthRegenPercent != null) {
            stats.setHealthRegenPercent(healthRegenPercent);
        }
        
        // 躲避反伤
        Double dodgeReflectChance = attrs.get("躲避反伤");
        if (dodgeReflectChance != null) {
            stats.setDodgeReflectPercent(dodgeReflectChance);
        }
        Double dodgeReflectRatio = attrs.get("躲避反弹比例");
        if (dodgeReflectRatio != null) {
            stats.setDodgeReflectRatio(dodgeReflectRatio);
        }
        
        // 击退抗性
        Double knockbackResist = attrs.get("击退抗性");
        if (knockbackResist != null) {
            stats.setKnockbackResistPercent(knockbackResist);
        }
        
        return stats;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
