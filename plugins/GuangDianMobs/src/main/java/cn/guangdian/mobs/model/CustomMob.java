package cn.guangdian.mobs.model;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 自定义怪物数据模型
 */
public class CustomMob {

    private String id;                    // 怪物ID
    private String displayName;           // 显示名称
    private EntityType entityType;        // 实体类型
    private double maxHealth;             // 最大血量
    private double damage;                // 基础伤害
    private double defense;               // 防御力
    private double moveSpeed;             // 移动速度
    private double attackSpeed;           // 攻击速度
    private double followRange;           // 追踪范围
    private int level;                    // 怪物等级
    private String dropTable;             // 掉落表ID
    private List<String> skills;          // 技能列表
    private Map<String, Object> metadata; // 元数据

    // 等级修饰器
    private Map<String, Double> levelModifiers; // 等级修饰器: health, damage, defense 等

    // 伤害修饰器
    private Map<String, Double> damageModifiers; // 伤害类型修饰器: LIGHTNING, FIRE 等

    // 选项
    private MobOptions options;           // 怪物选项

    // AI配置
    private MobAI ai;                     // AI配置

    // 装备
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack mainHand;
    private ItemStack offHand;

    public CustomMob(String id) {
        this.id = id;
        this.entityType = EntityType.ZOMBIE;
        this.maxHealth = 20.0;
        this.damage = 3.0;
        this.defense = 0.0;
        this.moveSpeed = 0.23;
        this.attackSpeed = 1.0;
        this.followRange = 32.0;
        this.level = 1;
        this.skills = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.levelModifiers = new HashMap<>();
        this.damageModifiers = new HashMap<>();
        this.options = new MobOptions();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }

    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }

    public double getDefense() { return defense; }
    public void setDefense(double defense) { this.defense = defense; }

    public double getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(double moveSpeed) { this.moveSpeed = moveSpeed; }

    public double getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(double attackSpeed) { this.attackSpeed = attackSpeed; }

    public double getFollowRange() { return followRange; }
    public void setFollowRange(double followRange) { this.followRange = followRange; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getDropTable() { return dropTable; }
    public void setDropTable(String dropTable) { this.dropTable = dropTable; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public ItemStack getHelmet() { return helmet; }
    public void setHelmet(ItemStack helmet) { this.helmet = helmet; }

    public ItemStack getChestplate() { return chestplate; }
    public void setChestplate(ItemStack chestplate) { this.chestplate = chestplate; }

    public ItemStack getLeggings() { return leggings; }
    public void setLeggings(ItemStack leggings) { this.leggings = leggings; }

    public ItemStack getBoots() { return boots; }
    public void setBoots(ItemStack boots) { this.boots = boots; }

    public ItemStack getMainHand() { return mainHand; }
    public void setMainHand(ItemStack mainHand) { this.mainHand = mainHand; }

    public ItemStack getOffHand() { return offHand; }
    public void setOffHand(ItemStack offHand) { this.offHand = offHand; }

    public Map<String, Double> getLevelModifiers() { return levelModifiers; }
    public void setLevelModifiers(Map<String, Double> levelModifiers) { this.levelModifiers = levelModifiers; }

    public Map<String, Double> getDamageModifiers() { return damageModifiers; }
    public void setDamageModifiers(Map<String, Double> damageModifiers) { this.damageModifiers = damageModifiers; }

    public MobOptions getOptions() { return options; }
    public void setOptions(MobOptions options) { this.options = options; }

    public MobAI getAi() { return ai; }
    public void setAi(MobAI ai) { this.ai = ai; }

    /**
     * 根据等级计算属性值
     */
    public double calculateAttribute(String attribute, int targetLevel) {
        double baseValue = switch (attribute.toLowerCase()) {
            case "health" -> maxHealth;
            case "damage" -> damage;
            case "defense" -> defense;
            case "speed" -> moveSpeed;
            default -> 0.0;
        };

        double modifier = levelModifiers.getOrDefault(attribute.toLowerCase(), 0.0);
        int levelDiff = targetLevel - this.level;

        return baseValue + (modifier * levelDiff);
    }

    /**
     * 获取伤害修饰器
     */
    public double getDamageModifier(String damageType) {
        return damageModifiers.getOrDefault(damageType.toUpperCase(), 1.0);
    }

    /**
     * 验证怪物配置是否有效
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() &&
               displayName != null && !displayName.isEmpty() &&
               entityType != null;
    }

    @Override
    public String toString() {
        return "CustomMob{" +
                "id='" + id + '\'' +
                ", name='" + displayName + '\'' +
                ", type=" + entityType +
                ", hp=" + maxHealth +
                ", dmg=" + damage +
                ", lvl=" + level +
                '}';
    }
}
