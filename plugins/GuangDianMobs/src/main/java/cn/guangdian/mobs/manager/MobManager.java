package cn.guangdian.mobs.manager;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.CustomMob;
import cn.guangdian.mobs.model.MobAI;
import cn.guangdian.mobs.model.MobOptions;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

/**
 * 怪物管理器
 */
public class MobManager {

    private static final String MOB_ID_KEY = "custom_mob_id";

    private final GuangDianMobs plugin;
    private final Map<String, CustomMob> mobTemplates = new HashMap<>();
    private final NamespacedKey mobIdKey;

    public MobManager(GuangDianMobs plugin) {
        this.plugin = plugin;
        this.mobIdKey = new NamespacedKey(plugin, MOB_ID_KEY);
    }

    /**
     * 加载怪物配置
     */
    public void loadMobs() {
        mobTemplates.clear();

        File file = new File(plugin.getDataFolder(), "mobs.yml");
        if (!file.exists()) {
            plugin.saveResource("mobs.yml", false);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("mobs");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection mobSection = section.getConfigurationSection(id);
            if (mobSection == null) continue;

            try {
                CustomMob mob = parseMob(id, mobSection);
                if (mob.isValid()) {
                    mobTemplates.put(id, mob);
                    plugin.getLogger().info("加载怪物: " + id + " - " + mob.getDisplayName());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载怪物失败: " + id + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("共加载 " + mobTemplates.size() + " 个怪物");
    }

    /**
     * 解析怪物配置
     */
    private CustomMob parseMob(String id, ConfigurationSection section) {
        CustomMob mob = new CustomMob(id);

        mob.setDisplayName(section.getString("display-name", id));

        // 解析实体类型，处理无效类型
        String entityTypeStr = section.getString("type", "ZOMBIE").toUpperCase();
        try {
            mob.setEntityType(org.bukkit.entity.EntityType.valueOf(entityTypeStr));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("怪物 " + id + " 使用了无效的实体类型: " + entityTypeStr + "，使用默认类型 ZOMBIE");
            mob.setEntityType(org.bukkit.entity.EntityType.ZOMBIE);
        }
        mob.setMaxHealth(section.getDouble("health", 20.0));
        mob.setDamage(section.getDouble("damage", 3.0));
        mob.setDefense(section.getDouble("defense", 0.0));
        mob.setMoveSpeed(section.getDouble("speed", 0.23));
        mob.setAttackSpeed(section.getDouble("attack-speed", 1.0));
        mob.setFollowRange(section.getDouble("follow-range", 32.0));
        mob.setLevel(section.getInt("level", 1));
        mob.setDropTable(section.getString("drop-table"));
        mob.setSkills(section.getStringList("skills"));

        // 加载等级修饰器
        ConfigurationSection levelMods = section.getConfigurationSection("level-modifiers");
        if (levelMods != null) {
            Map<String, Double> modifiers = new HashMap<>();
            for (String key : levelMods.getKeys(false)) {
                modifiers.put(key, levelMods.getDouble(key));
            }
            mob.setLevelModifiers(modifiers);
        }

        // 加载伤害修饰器
        ConfigurationSection damageMods = section.getConfigurationSection("damage-modifiers");
        if (damageMods != null) {
            Map<String, Double> modifiers = new HashMap<>();
            for (String key : damageMods.getKeys(false)) {
                modifiers.put(key.toUpperCase(), damageMods.getDouble(key));
            }
            mob.setDamageModifiers(modifiers);
        }

        // 加载选项
        ConfigurationSection options = section.getConfigurationSection("options");
        if (options != null) {
            MobOptions mobOptions = new MobOptions();
            mobOptions.setAlwaysShowName(options.getBoolean("always-show-name", false));
            mobOptions.setPreventOtherDrops(options.getBoolean("prevent-other-drops", false));
            mobOptions.setPreventSlimeSplit(options.getBoolean("prevent-slime-split", false));
            mobOptions.setMovementSpeed(options.getDouble("movement-speed", -1));
            mobOptions.setKnockbackResistance(options.getDouble("knockback-resistance", -1));
            mobOptions.setMaxCombatDistance(options.getDouble("max-combat-distance", -1));
            mobOptions.setSize(options.getInt("size", -1));
            mobOptions.setShowBossBar(options.getBoolean("show-boss-bar", false));
            mobOptions.setBossBarColor(options.getString("boss-bar-color", "RED"));
            mobOptions.setBossBarStyle(options.getString("boss-bar-style", "SOLID"));
            mob.setOptions(mobOptions);
        }

        // 加载装备
        ConfigurationSection equipment = section.getConfigurationSection("equipment");
        if (equipment != null) {
            // 这里可以加载 MythicMobs 物品或原版物品
            // 简化实现，实际需要根据配置加载
        }

        // 加载AI配置
        ConfigurationSection aiSection = section.getConfigurationSection("ai");
        if (aiSection != null) {
            MobAI ai = new MobAI();
            ai.setTargetSelectors(aiSection.getStringList("target-selectors"));
            ai.setAiGoals(aiSection.getStringList("goals"));

            // 加载AI设置
            ConfigurationSection aiSettings = aiSection.getConfigurationSection("settings");
            if (aiSettings != null) {
                MobAI.AISettings settings = new MobAI.AISettings();
                settings.setCanSwim(aiSettings.getBoolean("can-swim", true));
                settings.setCanBreakDoors(aiSettings.getBoolean("can-break-doors", false));
                settings.setCanOpenDoors(aiSettings.getBoolean("can-open-doors", false));
                settings.setCanPickUpItems(aiSettings.getBoolean("can-pick-up-items", false));
                settings.setAvoidWater(aiSettings.getBoolean("avoid-water", false));
                settings.setAvoidSun(aiSettings.getBoolean("avoid-sun", false));
                settings.setCanFly(aiSettings.getBoolean("can-fly", false));
                settings.setCanClimb(aiSettings.getBoolean("can-climb", false));
                settings.setFollowRange(aiSettings.getDouble("follow-range", 32.0));
                settings.setWanderSpeed(aiSettings.getDouble("wander-speed", 1.0));
                settings.setAttackSpeed(aiSettings.getDouble("attack-speed", 1.0));
                settings.setRetreatHealthPercent(aiSettings.getDouble("retreat-health-percent", 0.0));
                ai.setSettings(settings);
            }

            // 加载仇恨设置
            ConfigurationSection threatSettings = aiSection.getConfigurationSection("threat");
            if (threatSettings != null) {
                MobAI.ThreatSettings settings = new MobAI.ThreatSettings();
                settings.setThreatRadius(threatSettings.getDouble("radius", 16.0));
                settings.setThreatDecayRate(threatSettings.getDouble("decay-rate", 1.0));
                settings.setUseThreatTable(threatSettings.getBoolean("use-threat-table", true));
                settings.setTauntImmune(threatSettings.getBoolean("taunt-immune", false));
                settings.setIgnoreTargetsOutOfRange(threatSettings.getBoolean("ignore-out-of-range", true));
                settings.setTargetSwitchThreshold(threatSettings.getDouble("switch-threshold", 1.2));
                ai.setThreatSettings(settings);
            }

            mob.setAi(ai);
        }

        return mob;
    }

    /**
     * 生成自定义怪物
     */
    public LivingEntity spawnMob(String mobId, Location location) {
        return spawnMob(mobId, location, -1);
    }

    /**
     * 生成自定义怪物（指定等级）
     */
    public LivingEntity spawnMob(String mobId, Location location, int level) {
        CustomMob template = mobTemplates.get(mobId);
        if (template == null) {
            plugin.getLogger().warning("怪物模板不存在: " + mobId);
            return null;
        }

        // 生成实体
        Entity entity = location.getWorld().spawnEntity(location, template.getEntityType());
        if (!(entity instanceof LivingEntity)) {
            entity.remove();
            return null;
        }

        LivingEntity living = (LivingEntity) entity;

        // 应用属性（使用指定等级）
        applyMobAttributes(living, template, level);

        // 保存怪物ID到PDC
        PersistentDataContainer pdc = living.getPersistentDataContainer();
        pdc.set(mobIdKey, PersistentDataType.STRING, mobId);

        // 保存等级到PDC
        if (level > 0) {
            pdc.set(new NamespacedKey(plugin, "mob_level"), PersistentDataType.INTEGER, level);
        }

        // 创建Boss血条
        if (template.getOptions().isShowBossBar()) {
            plugin.getBossBarManager().createBossBar(living, template);
        }

        // 应用AI
        if (template.getAi() != null) {
            plugin.getAIController().applyAI(living, template);
        }

        return living;
    }

    /**
     * 应用怪物属性
     */
    private void applyMobAttributes(LivingEntity entity, CustomMob template) {
        applyMobAttributes(entity, template, -1);
    }

    /**
     * 应用怪物属性（指定等级）
     */
    private void applyMobAttributes(LivingEntity entity, CustomMob template, int level) {
        MobOptions options = template.getOptions();

        // 计算实际等级
        int actualLevel = level > 0 ? level : template.getLevel();

        // 设置名称（使用MiniMessage解析颜色）
        if (template.getDisplayName() != null) {
            entity.customName(MiniMessage.miniMessage().deserialize(template.getDisplayName()));
            entity.setCustomNameVisible(options.isAlwaysShowName());
        }

        // 设置血量（应用等级修饰器）
        double health = template.calculateAttribute("health", actualLevel);
        var maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(health);
            entity.setHealth(health);
        }

        // 设置移动速度
        double speed = options.getMovementSpeed() > 0 ? options.getMovementSpeed() : template.getMoveSpeed();
        var moveSpeedAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveSpeedAttr != null) {
            moveSpeedAttr.setBaseValue(speed);
        }

        // 设置攻击速度
        if (entity.getAttribute(Attribute.ATTACK_SPEED) != null) {
            entity.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(template.getAttackSpeed());
        }

        // 设置追踪范围
        if (entity.getAttribute(Attribute.FOLLOW_RANGE) != null) {
            double followRange = options.getMaxCombatDistance() > 0 ? options.getMaxCombatDistance() : template.getFollowRange();
            entity.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(followRange);
        }

        // 设置击退抗性
        if (entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null && options.getKnockbackResistance() >= 0) {
            entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(options.getKnockbackResistance());
        }

        // 设置史莱姆大小
        if (entity instanceof Slime slime && options.getSize() > 0) {
            slime.setSize(options.getSize());
        }

        // 设置装备
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            if (template.getHelmet() != null) equipment.setHelmet(template.getHelmet());
            if (template.getChestplate() != null) equipment.setChestplate(template.getChestplate());
            if (template.getLeggings() != null) equipment.setLeggings(template.getLeggings());
            if (template.getBoots() != null) equipment.setBoots(template.getBoots());
            if (template.getMainHand() != null) equipment.setItemInMainHand(template.getMainHand());
            if (template.getOffHand() != null) equipment.setItemInOffHand(template.getOffHand());
        }
    }

    /**
     * 获取怪物模板
     */
    public CustomMob getMobTemplate(String id) {
        return mobTemplates.get(id);
    }

    /**
     * 获取所有怪物模板
     */
    public Collection<CustomMob> getAllMobs() {
        return mobTemplates.values();
    }

    /**
     * 获取怪物数量
     */
    public int getMobCount() {
        return mobTemplates.size();
    }

    /**
     * 从实体获取怪物ID
     */
    public String getMobIdFromEntity(LivingEntity entity) {
        if (entity == null) return null;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.get(mobIdKey, PersistentDataType.STRING);
    }

    /**
     * 检查实体是否是自定义怪物
     */
    public boolean isCustomMob(LivingEntity entity) {
        return getMobIdFromEntity(entity) != null;
    }
}
