package cn.guangdian.rpgitems.api;

import cn.guangdian.rpgitems.attribute.ItemAttributes;
import cn.guangdian.rpgitems.attribute.PDCAttributeReader;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 物品属性 API 实现
 */
public class ItemAttributeAPIImpl implements ItemAttributeAPI {

    @Override
    public Map<String, Double> getAttributes(ItemStack item) {
        ItemAttributes attrs = getItemAttributes(item);
        if (attrs == null) {
            return new HashMap<>();
        }
        
        Map<String, Double> result = new HashMap<>();
        
        if (attrs.hasAttack()) {
            result.put("攻击力最小", attrs.attackMin());
            result.put("攻击力最大", attrs.attackMax());
            result.put("攻击力", attrs.getAverageAttack());
        }
        
        if (attrs.hasDefense()) {
            result.put("防御力最小", attrs.defenseMin());
            result.put("防御力最大", attrs.defenseMax());
            result.put("防御力", attrs.getAverageDefense());
        }
        
        if (attrs.maxHealth() > 0) {
            result.put("生命上限", attrs.maxHealth());
        }
        
        if (attrs.healthRegen() > 0) {
            result.put("生命恢复", attrs.healthRegen());
        }
        
        if (attrs.critChance() > 0) {
            result.put("暴击几率", attrs.critChance());
        }
        
        if (attrs.critDamage() > 0) {
            result.put("暴击伤害", attrs.critDamage());
        }
        
        if (attrs.lifestealChance() > 0) {
            result.put("吸血几率", attrs.lifestealChance());
        }
        
        if (attrs.lifestealMultiplier() > 0) {
            result.put("吸血倍率", attrs.lifestealMultiplier());
        }
        
        if (attrs.dodgeChance() > 0) {
            result.put("闪避几率", attrs.dodgeChance());
        }
        
        if (attrs.parryChance() > 0) {
            result.put("格挡几率", attrs.parryChance());
        }
        
        if (attrs.moveSpeed() > 0) {
            result.put("移动速度", attrs.moveSpeed());
        }
        
        if (attrs.damageReduction() > 0) {
            result.put("减伤", attrs.damageReduction());
        }
        
        return result;
    }

    @Override
    public ItemAttributes getItemAttributes(ItemStack item) {
        return PDCAttributeReader.readAttributes(item);
    }

    @Override
    public int getLevel(ItemStack item) {
        ItemAttributes attrs = getItemAttributes(item);
        return attrs != null ? attrs.level() : 0;
    }

    @Override
    public String getRequiredClass(ItemStack item) {
        ItemAttributes attrs = getItemAttributes(item);
        return attrs != null ? attrs.requiredClass() : "";
    }

    @Override
    public boolean isRPGItem(ItemStack item) {
        return PDCAttributeReader.isRPGItem(item);
    }

    @Override
    public String getItemId(ItemStack item) {
        return PDCAttributeReader.getItemId(item);
    }

    @Override
    public boolean canEquip(String playerClassId, int playerTier, ItemStack item) {
        ItemAttributes attrs = getItemAttributes(item);
        if (attrs == null) {
            return true;
        }
        
        // 检查等级限制
        if (attrs.level() > 0 && playerTier < attrs.level()) {
            return false;
        }
        
        // 检查职业限制
        String requiredClass = attrs.requiredClass();
        if (requiredClass != null && !requiredClass.isEmpty()) {
            if (!requiredClass.equals(playerClassId)) {
                return false;
            }
        }
        
        return true;
    }
}
