package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.cache.EquipmentCacheManager;
import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.debug.DebugLogManager;
import cn.guangdian.armorstats.parser.LoreParser;
import cn.guangdian.armorstats.parser.GemParser;
import cn.guangdian.armorstats.parser.SkillParser;
import cn.guangdian.armorstats.storage.AsyncExecutorService;
import cn.guangdian.armorstats.storage.PlayerDataStorage;
import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.concurrency.PlayerLockManager;
import cn.guangdian.rpgcore.concurrency.LockTimeoutException;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * 玩家属性管理器
 * 
 * 重构: 使用 RPGCore 统一框架
 * - PlayerLockManager: 玩家级锁保护并发操作
 * - AsyncExecutor: 统一异步执行器
 */
public class StatsManager {

    private final Map<UUID, PlayerStats> playerStatsMap = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> playerSkillsMap = new ConcurrentHashMap<>();
    
    // 已完成登录加载的玩家集合（用于防止登录时重复刷新）
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    
    // 分离缓存：防具属性（持久）和武器属性（临时）
    private final Map<UUID, PlayerStats> armorStatsCache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerStats> weaponStatsCache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerStats> accessoryStatsCache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerStats> offHandStatsCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> armorSkillsCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> weaponSkillsCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> accessorySkillsCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> offHandSkillsCache = new ConcurrentHashMap<>();
    
    // 武器物品变更检测缓存（避免重复解析）
    private final Map<UUID, String> weaponItemHashCache = new ConcurrentHashMap<>();
    
    // 智能装备识别和动态缓存
    private final SmartAttributeDetector attributeDetector = new SmartAttributeDetector();
    private final DynamicCacheManager cacheManager = new DynamicCacheManager();
    private final FullEquipmentManager equipmentManager;
    
    // 装备缓存管理器（优化装备解析性能）
    private EquipmentCacheManager equipmentCacheManager;
    
    // 玩家数据持久化存储
    private PlayerDataStorage dataStorage;
    
    // RPGCore 框架组件
    private PlayerLockManager lockManager;
    private AsyncExecutor asyncExecutor;
    
    private Map<String, String> attributePatterns;
    private Map<String, String> gemPatterns;
    private String gemSocketPattern;
    private double defenseDivisor;
    private double maxDamageReduction;
    private double minDamage;
    private boolean dodgeEnabled;
    private double maxDodge;
    private boolean critResistEnabled;
    private double critResistDamageReduction;
    private boolean healthDisplayScaleEnabled;
    private int healthDisplayMaxRows;

    // 装备类型识别配置
    private List<String> weaponLoreKeywords = new ArrayList<>();
    private List<String> weaponFirstLineKeywords = new ArrayList<>();
    private Set<Material> weaponMaterials = new HashSet<>();
    private List<String> armorLoreKeywords = new ArrayList<>();
    private List<String> armorFirstLineKeywords = new ArrayList<>();
    private Set<Material> armorMaterials = new HashSet<>();

    private static final double MAX_HEALTH_LIMIT = 2000000.0;
    private final NamespacedKey healthKey;
    private final NamespacedKey speedKey;
    private final GuangDianArmorStats plugin;

    public StatsManager(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        this.healthKey = new NamespacedKey(plugin, "max_health");
        this.speedKey = new NamespacedKey(plugin, "move_speed");
        this.dataStorage = new PlayerDataStorage(plugin);
        this.equipmentManager = new FullEquipmentManager(this);
        this.equipmentCacheManager = null; // 将在配置加载后初始化
        
        // 初始化 RPGCore 框架组件
        initRPGCoreComponents();
        
        loadConfig();
        plugin.getLogger().info("StatsManager initialized with RPGCore framework");
    }
    
    /**
     * 初始化 RPGCore 框架组件
     */
    private void initRPGCoreComponents() {
        // 获取玩家锁管理器
        if (RPGCore.getInstance() != null) {
            this.lockManager = RPGCore.getInstance().getLockManager();
            this.asyncExecutor = RPGCore.getInstance().getAsyncExecutor();
            plugin.getLogger().info("已连接 RPGCore 框架: PlayerLockManager, AsyncExecutor");
        } else {
            plugin.getLogger().warning("RPGCore 不可用，创建本地锁管理器");
            // 创建本地锁管理器作为降级方案
            this.lockManager = new PlayerLockManager(plugin.getLogger(), 3000);
        }
    }
    
    /**
     * 设置装备缓存管理器（由主插件类调用）
     */
    public void setEquipmentCacheManager(EquipmentCacheManager cacheManager) {
        this.equipmentCacheManager = cacheManager;
    }
    
    /**
     * 设置 RPGCore 异步执行器
     */
    public void setRPGCoreAsyncExecutor(cn.guangdian.rpgcore.api.AsyncExecutor executor) {
        this.asyncExecutor = executor;
        // 使用 RPGCore AsyncExecutor 初始化存储
        this.dataStorage = new PlayerDataStorage(plugin, executor);
    }
    
    /**
     * 设置本地异步执行器（降级用）
     */
    public void setAsyncExecutor(AsyncExecutorService executor) {
        this.dataStorage = new PlayerDataStorage(plugin, executor);
    }

    public void loadConfig() {
        var configManager = plugin.getConfigManager();
        
        var attrConfig = configManager.getAttributes();
        if (attrConfig != null) {
            ConfigurationSection attrSection = attrConfig.getConfigurationSection("attributes");
            if (attrSection != null) {
                attributePatterns = new ConcurrentHashMap<>();
                for (String key : attrSection.getKeys(false)) {
                    attributePatterns.put(key, attrSection.getString(key, ""));
                }
                LoreParser.initializePatterns(attributePatterns);
            }
        }

        var gemsConfig = configManager.getGems();
        if (gemsConfig != null) {
            ConfigurationSection gemSection = gemsConfig.getConfigurationSection("gems");
            if (gemSection != null) {
                gemPatterns = new ConcurrentHashMap<>();
                for (String key : gemSection.getKeys(false)) {
                    gemPatterns.put(key, gemSection.getString(key, ""));
                }
            }
            gemSocketPattern = gemsConfig.getString("gem_socket", "可镶嵌<([^>]+)>");
            Map<String, String> socketMap = new HashMap<>();
            socketMap.put("socket", gemSocketPattern);
            GemParser.initialize(socketMap, gemPatterns);
        }

        var damageConfig = configManager.getDamage();
        if (damageConfig != null) {
            defenseDivisor = damageConfig.getDouble("defense_divisor", 15000);
            maxDamageReduction = damageConfig.getDouble("max_damage_reduction", 0.95);
            minDamage = damageConfig.getDouble("min_damage", 1.0);
        }

        var mainConfig = configManager.getConfig("config");
        if (mainConfig != null) {
            var dodgeSection = mainConfig.getConfigurationSection("dodge");
            if (dodgeSection != null) {
                dodgeEnabled = dodgeSection.getBoolean("enabled", true);
                maxDodge = dodgeSection.getDouble("max_dodge", 0.8);
            }

            var critSection = mainConfig.getConfigurationSection("crit_resist");
            if (critSection != null) {
                critResistEnabled = critSection.getBoolean("enabled", true);
                critResistDamageReduction = critSection.getDouble("damage_reduction", 0.01);
            }

            var healthDisplaySection = mainConfig.getConfigurationSection("health_display");
            if (healthDisplaySection != null) {
                healthDisplayScaleEnabled = healthDisplaySection.getBoolean("enable_scale", true);
                healthDisplayMaxRows = Math.max(1, Math.min(2, healthDisplaySection.getInt("max_rows", 2)));
            } else {
                healthDisplayScaleEnabled = true;
                healthDisplayMaxRows = 2;
            }

            // 加载装备类型识别配置
            loadEquipmentIdentificationConfig(mainConfig);
        }
    }

    /**
     * 加载装备类型识别配置
     */
    private void loadEquipmentIdentificationConfig(org.bukkit.configuration.file.FileConfiguration config) {
        ConfigurationSection idSection = config.getConfigurationSection("equipment_identification");
        if (idSection == null) {
            plugin.getLogger().warning("未找到装备识别配置，使用默认值");
            // 默认配置 - 优先检测Lore第一行
            weaponLoreKeywords = Arrays.asList("攻击力", "暴击几率");
            weaponFirstLineKeywords = Arrays.asList("近战武器", "远程武器", "武器");
            armorLoreKeywords = Arrays.asList("防御力", "生命上限");
            armorFirstLineKeywords = Arrays.asList("防具"); // 首行包含"防具"
            loadDefaultArmorMaterials();
            return;
        }

        // 加载武器识别配置
        ConfigurationSection weaponSection = idSection.getConfigurationSection("weapon");
        if (weaponSection != null) {
            weaponLoreKeywords = weaponSection.getStringList("lore_keywords");
            weaponFirstLineKeywords = weaponSection.getStringList("lore_first_line_keywords");
            weaponMaterials = parseMaterialList(weaponSection.getStringList("materials"));
        }

        // 加载防具识别配置
        ConfigurationSection armorSection = idSection.getConfigurationSection("armor");
        if (armorSection != null) {
            armorLoreKeywords = armorSection.getStringList("lore_keywords");
            armorFirstLineKeywords = armorSection.getStringList("lore_first_line_keywords");
            armorMaterials = parseMaterialList(armorSection.getStringList("materials"));
        }

        plugin.getLogger().info("装备识别配置加载完成:");
        plugin.getLogger().info("  武器Lore关键词: " + weaponLoreKeywords);
        plugin.getLogger().info("  武器首行关键词: " + weaponFirstLineKeywords);
        plugin.getLogger().info("  武器材质: " + weaponMaterials.size() + " 种");
        plugin.getLogger().info("  防具Lore关键词: " + armorLoreKeywords);
        plugin.getLogger().info("  防具首行关键词: " + armorFirstLineKeywords);
        plugin.getLogger().info("  防具材质: " + armorMaterials.size() + " 种");
    }

    /**
     * 加载默认防具材质
     */
    private void loadDefaultArmorMaterials() {
        armorMaterials = new HashSet<>();
        for (Material mat : Material.values()) {
            String name = mat.name();
            if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
                armorMaterials.add(mat);
            }
        }
        armorMaterials.add(Material.TURTLE_HELMET);
    }

    /**
     * 解析材质列表
     */
    private Set<Material> parseMaterialList(List<String> materialNames) {
        Set<Material> materials = new HashSet<>();
        for (String name : materialNames) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
                materials.add(mat);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("未知的材质: " + name);
            }
        }
        return materials;
    }

    /**
     * 从存储加载玩家数据（登录时调用）
     * 1. 恢复防具属性（从存储）
     * 2. 解析当前武器（触发武器解析逻辑）
     * 
     * 重构: 使用 PlayerLockManager 保护并发操作
     */
    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 调试日志：开始加载
        DebugLogManager.log(player, DebugLogManager.Level.INFO, 
            DebugLogManager.Operation.LOAD_START, "开始加载玩家数据");
        
        try {
            // 使用玩家锁保护整个加载过程
            lockManager.executeWithLock(uuid, () -> {
                PlayerDataStorage.PlayerData savedData = dataStorage.loadPlayerData(uuid);
                
                if (savedData != null && savedData.armorStats != null) {
                    plugin.getLogger().info("从存储恢复玩家防具属性: " + player.getName());
                    
                    // 调试日志：从存储恢复
                    Map<String, Object> details = new LinkedHashMap<>();
                    details.put("source", "storage");
                    details.put("health", savedData.health);
                    details.put("maxHealth", savedData.maxHealth);
                    DebugLogManager.log(player, DebugLogManager.Level.DEBUG, 
                        DebugLogManager.Operation.LOAD_COMPLETE, "从存储恢复数据", details);
                    
                    // 恢复防具缓存
                    armorStatsCache.put(uuid, savedData.armorStats);
                    armorSkillsCache.put(uuid, savedData.armorSkills != null ? savedData.armorSkills : new ArrayList<>());
                    
                    // 应用属性
                    clearPlayerAttributesInternal(player);
                    
                    // 解析当前武器（不重新解析防具）
                    refreshWeaponCacheInternal(player, uuid);
                    
                    // 合并属性
                    PlayerStats stats = mergeStats(uuid);
                    
                    applyMaxHealth(player, stats, savedData.health > 0 ? savedData.health : player.getHealth(), savedData.maxHealth);
                    applyMoveSpeed(player, stats);
                } else {
                    plugin.getLogger().info("无存储数据，完整解析玩家属性: " + player.getName());
                    
                    // 调试日志：无存储数据，完整解析
                    DebugLogManager.log(player, DebugLogManager.Level.DEBUG, 
                        DebugLogManager.Operation.PARSE_START, "无存储数据，执行完整解析");
                    
                    refreshFullStatsInternal(player);
                }
                
                // 标记玩家已完成登录加载
                loadedPlayers.add(uuid);
            });
        } catch (LockTimeoutException e) {
            plugin.getLogger().severe("加载玩家数据超时: " + player.getName() + " - " + e.getMessage());
            // 降级处理：直接执行无锁版本
            refreshFullStats(player);
            // 标记玩家已完成登录加载
            loadedPlayers.add(uuid);
        }
    }
    
    /**
     * 检查玩家是否已完成登录加载
     */
    public boolean isPlayerLoaded(UUID uuid) {
        return loadedPlayers.contains(uuid);
    }
    
    /**
     * 保存玩家数据（退出时调用）
     * 只保存防具属性和血量，武器属性不保存
     * 
     * 重构: 使用 PlayerLockManager 保护并发操作
     */
    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 调试日志：开始保存
        DebugLogManager.log(player, DebugLogManager.Level.DEBUG, 
            DebugLogManager.Operation.SAVE_START, "开始保存玩家数据");
        
        try {
            // 使用玩家锁保护保存操作
            lockManager.executeWithLock(uuid, () -> {
                double health = player.getHealth();
                double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                PlayerStats armorStats = armorStatsCache.get(uuid);
                List<String> armorSkills = armorSkillsCache.get(uuid);
                
                // 调试日志：保存详情
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("health", health);
                details.put("maxHealth", maxHealth);
                details.put("hasArmorStats", armorStats != null);
                details.put("thread", Thread.currentThread().getName());
                DebugLogManager.log(player, DebugLogManager.Level.DEBUG, 
                    DebugLogManager.Operation.SAVE_COMPLETE, "玩家数据保存中", details);
                
                // 使用 RPGCore AsyncExecutor 异步保存
                if (asyncExecutor != null) {
                    asyncExecutor.submitPlayerSave(uuid, () -> {
                        savePlayerDataSync(uuid, health, maxHealth, armorStats, armorSkills);
                    });
                } else {
                    // 降级：同步保存
                    savePlayerDataSync(uuid, health, maxHealth, armorStats, armorSkills);
                }
            });
        } catch (LockTimeoutException e) {
            plugin.getLogger().warning("保存玩家数据超时，强制保存: " + player.getName());
            // 强制保存
            forceSavePlayerData(uuid, player);
        }
    }
    
    /**
     * 同步保存玩家数据
     */
    private void savePlayerDataSync(UUID uuid, double health, double maxHealth, 
                                     PlayerStats armorStats, List<String> armorSkills) {
        dataStorage.savePlayerData(uuid, health, maxHealth, armorStats, armorSkills, null);
        plugin.getLogger().info("已保存玩家数据: " + uuid);
    }
    
    /**
     * 强制保存（超时降级）
     */
    private void forceSavePlayerData(UUID uuid, Player player) {
        double health = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        PlayerStats armorStats = armorStatsCache.get(uuid);
        List<String> armorSkills = armorSkillsCache.get(uuid);
        dataStorage.savePlayerData(uuid, health, maxHealth, armorStats, armorSkills, null);
    }
    
    /**
     * 完整刷新所有属性
     * 用于：首次登录无存储数据、重生
     * 
     * 重构: 使用 PlayerLockManager 保护并发操作
     */
    public void refreshFullStats(Player player) {
        UUID uuid = player.getUniqueId();
        
        try {
            lockManager.executeWithLock(uuid, () -> {
                refreshFullStatsInternal(player);
            });
        } catch (LockTimeoutException e) {
            plugin.getLogger().warning("刷新属性超时，强制执行: " + player.getName());
            refreshFullStatsInternal(player);
        }
    }
    
    /**
     * 完整刷新属性（内部方法，需在锁内调用）
     */
    private void refreshFullStatsInternal(Player player) {
        // 调试日志：开始完整刷新
        DebugLogManager.log(player, DebugLogManager.Level.DEBUG, 
            DebugLogManager.Operation.STATS_RESET, "开始完整刷新属性");
        
        double savedHealth = player.getHealth();
        double savedMaxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();

        clearPlayerAttributesInternal(player);

        UUID uuid = player.getUniqueId();
        
        refreshArmorCacheInternal(player, uuid);
        refreshWeaponCacheInternal(player, uuid);
        
        PlayerStats stats = mergeStats(uuid);

        applyMaxHealth(player, stats, savedHealth, savedMaxHealth);
        applyMoveSpeed(player, stats);
        
        // 调试日志：刷新完成
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("maxHealth", stats.getMaxHealth());
        details.put("minAttack", stats.getMinAttack());
        details.put("maxAttack", stats.getMaxAttack());
        details.put("defenseMin", stats.getDefenseMin());
        details.put("defenseMax", stats.getDefenseMax());
        DebugLogManager.log(player, DebugLogManager.Level.DEBUG, 
            DebugLogManager.Operation.STATS_APPLY, "完整刷新完成", details);
    }
    
    /**
     * 仅刷新防具属性（穿戴/卸下防具时）
     * 武器属性从缓存恢复
     * 
     * 重构: 使用 PlayerLockManager 保护并发操作
     */
    public void refreshArmorOnly(Player player) {
        UUID uuid = player.getUniqueId();
        
        plugin.getLogger().info("[属性刷新] 开始刷新防具属性: " + player.getName());
        
        try {
            lockManager.executeWithLock(uuid, () -> {
                double savedHealth = player.getHealth();
                double savedMaxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();

                plugin.getLogger().info("[属性刷新] 保存血量: " + savedHealth + "/" + savedMaxHealth);

                clearPlayerAttributesInternal(player);

                refreshArmorCacheInternal(player, uuid);
                
                PlayerStats stats = mergeStats(uuid);
                
                plugin.getLogger().info("[属性刷新] 合并后属性 - 生命上限: " + stats.getMaxHealth() + 
                    ", 防御: " + stats.getDefenseMin() + "-" + stats.getDefenseMax());

                applyMaxHealth(player, stats, savedHealth, savedMaxHealth);
                applyMoveSpeed(player, stats);
                
                double newMaxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                plugin.getLogger().info("[属性刷新] 应用后最大血量: " + newMaxHealth);
            });
        } catch (LockTimeoutException e) {
            plugin.getLogger().warning("刷新防具属性超时: " + player.getName());
        }
    }
    
    /**
     * 仅刷新武器属性（切换主手物品时）
     * 防具属性从缓存恢复
     * 注意：切换武器不改变血量，只更新属性modifier
     * 
     * 重构: 使用 PlayerLockManager 保护并发操作
     * 优化: 变更检测，避免重复解析
     */
    public void refreshWeaponOnly(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 变更检测：计算当前物品哈希
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        String currentHash = computeItemHash(mainHand) + ":" + computeItemHash(offHand);
        
        // 比较哈希，如果没变化则跳过
        String lastHash = weaponItemHashCache.get(uuid);
        if (currentHash.equals(lastHash)) {
            return; // 物品没变化，跳过刷新
        }
        weaponItemHashCache.put(uuid, currentHash);
        
        try {
            lockManager.executeWithLock(uuid, () -> {
                refreshWeaponCacheInternal(player, uuid);
                
                PlayerStats stats = mergeStats(uuid);

                // 只更新属性modifier，不设置血量
                applyMaxHealthModifier(player, stats);
                applyMoveSpeed(player, stats);
            });
        } catch (LockTimeoutException e) {
            plugin.getLogger().warning("刷新武器属性超时: " + player.getName());
        }
    }
    
    /**
     * 计算物品的唯一标识哈希（用于变更检测）
     */
    private String computeItemHash(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "AIR";
        }
        return item.getType().name() + ":" + 
               item.getAmount() + ":" + 
               (item.hasItemMeta() ? item.getItemMeta().hashCode() : "0");
    }
    
    /**
     * 仅更新最大血量modifier，不改变当前血量
     * 用于切换武器等不需要改变血量的场景
     */
    private void applyMaxHealthModifier(Player player, PlayerStats stats) {
        double bonusHealth = stats.getMaxHealth();
        double effectiveMaxHealth = Math.min(20.0 + bonusHealth, MAX_HEALTH_LIMIT);

        AttributeInstance attributeInstance = player.getAttribute(Attribute.MAX_HEALTH);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(healthKey);

            if (effectiveMaxHealth > 20.0) {
                AttributeModifier modifier = new AttributeModifier(
                    healthKey,
                    effectiveMaxHealth - 20.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY
                );
                attributeInstance.addModifier(modifier);
            }
            
            applyHealthDisplayScale(player, attributeInstance.getValue());
        }
    }
    
    /**
     * 解析并缓存防具和配饰属性（内部方法，需在锁内调用）
     */
    private void refreshArmorCacheInternal(Player player, UUID uuid) {
        // 调试日志：开始解析防具
        DebugLogManager.log(player, DebugLogManager.Level.DEBUG, 
            DebugLogManager.Operation.PARSE_START, "开始解析防具属性");
        
        PlayerStats armorStats = armorStatsCache.computeIfAbsent(uuid, k -> new PlayerStats());
        List<String> armorSkills = armorSkillsCache.computeIfAbsent(uuid, k -> new ArrayList<>());
        PlayerStats accessoryStats = accessoryStatsCache.computeIfAbsent(uuid, k -> new PlayerStats());
        List<String> accessorySkills = accessorySkillsCache.computeIfAbsent(uuid, k -> new ArrayList<>());
        
        armorStats.reset();
        armorSkills.clear();
        accessoryStats.reset();
        accessorySkills.clear();
        
        ItemStack[] armor = player.getInventory().getArmorContents();
        int armorCount = 0;
        int accessoryCount = 0;
        
        plugin.getLogger().info("[防具解析] 玩家: " + player.getName() + ", 防具槽数: " + armor.length);
        
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item != null && item.getType() != Material.AIR) {
                plugin.getLogger().info("[防具解析] 槽位 " + i + ": " + item.getType().name());
                
                SmartAttributeDetector.EquipmentCategory category = attributeDetector.categorize(item);
                
                // 调试日志：识别装备类型
                Map<String, Object> itemDetails = new LinkedHashMap<>();
                itemDetails.put("material", item.getType().name());
                itemDetails.put("category", category.name());
                itemDetails.put("hasLore", item.hasItemMeta() && item.getItemMeta().hasLore());
                DebugLogManager.log(player, DebugLogManager.Level.TRACE, 
                    DebugLogManager.Operation.PARSE_LORE, "识别装备: " + category.name(), itemDetails);
                
                plugin.getLogger().info("[防具解析] 槽位 " + i + " 类型: " + category.name());
                
                if (category == SmartAttributeDetector.EquipmentCategory.ARMOR) {
                    addItemAttributes(armorStats, armorSkills, item);
                    armorCount++;
                    plugin.getLogger().info("[防具解析] 槽位 " + i + " 添加到防具属性, 当前生命上限: " + armorStats.getMaxHealth());
                } else if (category == SmartAttributeDetector.EquipmentCategory.ACCESSORY) {
                    addItemAttributes(accessoryStats, accessorySkills, item);
                    accessoryCount++;
                }
                
                // 记录使用情况，用于动态缓存
                cacheManager.recordUsage(uuid, item);
            }
        }
        
        plugin.getLogger().info("[防具解析] 完成 - 防具数: " + armorCount + 
            ", 生命上限: " + armorStats.getMaxHealth() + 
            ", 防御: " + armorStats.getDefenseMin() + "-" + armorStats.getDefenseMax());
        
        // 调试日志：防具解析完成
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("armorCount", armorCount);
        details.put("accessoryCount", accessoryCount);
        details.put("defenseMin", armorStats.getDefenseMin());
        details.put("defenseMax", armorStats.getDefenseMax());
        details.put("maxHealth", armorStats.getMaxHealth());
        DebugLogManager.log(player, DebugLogManager.Level.DEBUG, 
            DebugLogManager.Operation.PARSE_COMPLETE, "防具解析完成", details);
    }
    
    /**
     * 解析并缓存武器属性（主手）（内部方法，需在锁内调用）
     * 使用智能装备识别
     */
    private void refreshWeaponCacheInternal(Player player, UUID uuid) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        // 处理主手
        SmartAttributeDetector.EquipmentCategory mainHandCategory = attributeDetector.categorize(mainHand);
        
        if (mainHandCategory == SmartAttributeDetector.EquipmentCategory.WEAPON) {
            PlayerStats weaponStats = weaponStatsCache.computeIfAbsent(uuid, k -> new PlayerStats());
            List<String> weaponSkills = weaponSkillsCache.computeIfAbsent(uuid, k -> new ArrayList<>());
            
            weaponStats.reset();
            weaponSkills.clear();
            
            addItemAttributes(weaponStats, weaponSkills, mainHand);
            
            cacheManager.recordUsage(uuid, mainHand);
        } else {
            weaponStatsCache.remove(uuid);
            weaponSkillsCache.remove(uuid);
        }
        
        // 处理副手
        SmartAttributeDetector.EquipmentCategory offHandCategory = attributeDetector.categorize(offHand);
        if (offHandCategory != SmartAttributeDetector.EquipmentCategory.UNKNOWN) {
            PlayerStats offHandStats = offHandStatsCache.computeIfAbsent(uuid, k -> new PlayerStats());
            List<String> offHandSkills = offHandSkillsCache.computeIfAbsent(uuid, k -> new ArrayList<>());
            
            offHandStats.reset();
            offHandSkills.clear();
            
            addItemAttributes(offHandStats, offHandSkills, offHand);
            
            cacheManager.recordUsage(uuid, offHand);
        } else {
            offHandStatsCache.remove(uuid);
            offHandSkillsCache.remove(uuid);
        }
    }
    
    /**
     * 合并所有装备属性
     */
    private PlayerStats mergeStats(UUID uuid) {
        PlayerStats stats = playerStatsMap.computeIfAbsent(uuid, k -> new PlayerStats());
        stats.reset();
        
        PlayerStats armorStats = armorStatsCache.get(uuid);
        PlayerStats weaponStats = weaponStatsCache.get(uuid);
        PlayerStats accessoryStats = accessoryStatsCache.get(uuid);
        PlayerStats offHandStats = offHandStatsCache.get(uuid);
        
        if (armorStats != null) {
            stats.addPlayerStats(armorStats);
        }
        if (weaponStats != null) {
            stats.addPlayerStats(weaponStats);
        }
        if (accessoryStats != null) {
            stats.addPlayerStats(accessoryStats);
        }
        if (offHandStats != null) {
            stats.addPlayerStats(offHandStats);
        }
        
        // 合并技能列表
        mergeSkills(uuid);
        
        // 更新回血跟踪
        if (plugin.getRegenTask() != null) {
            plugin.getRegenTask().updatePlayerRegen(uuid, stats.getHealthRegen());
        }
        
        return stats;
    }
    
    /**
     * 合并所有装备技能
     */
    private List<String> mergeSkills(UUID uuid) {
        List<String> skills = playerSkillsMap.computeIfAbsent(uuid, k -> new ArrayList<>());
        skills.clear();
        
        List<String> armorSkills = armorSkillsCache.get(uuid);
        List<String> weaponSkills = weaponSkillsCache.get(uuid);
        List<String> accessorySkills = accessorySkillsCache.get(uuid);
        List<String> offHandSkills = offHandSkillsCache.get(uuid);
        
        if (armorSkills != null) {
            skills.addAll(armorSkills);
        }
        if (weaponSkills != null) {
            skills.addAll(weaponSkills);
        }
        if (accessorySkills != null) {
            skills.addAll(accessorySkills);
        }
        if (offHandSkills != null) {
            skills.addAll(offHandSkills);
        }
        
        return skills;
    }

    /**
     * 添加物品属性到统计对象
     * 改为 public 以便其他管理器使用
     * 
     * 优化特性:
     * - 使用装备缓存避免重复解析Lore
     * - 缓存未启用时降级到直接解析
     * 
     * 修复: 正确合并宝石属性，避免覆盖装备属性
     */
    public void addItemAttributes(PlayerStats stats, List<String> skills, ItemStack item) {
        String itemDesc = item != null ? item.getType().name() : "NULL";
        
        // 如果装备缓存已启用，使用缓存
        if (equipmentCacheManager != null) {
            PlayerStats cachedStats = equipmentCacheManager.getEquipmentStats(item);
            if (cachedStats != null) {
                // 调试日志：缓存命中
                String hash = equipmentCacheManager.calculateItemHash(item);
                DebugLogManager.logCacheHit(null, hash);
                
                stats.addPlayerStats(cachedStats);
                
                // 解析技能（技能不缓存，因为技能列表较小）
                if (skills != null) {
                    List<String> itemSkills = SkillParser.parseSkillNames(item);
                    skills.addAll(itemSkills);
                }
                return;
            } else {
                // 调试日志：缓存未命中
                String hash = equipmentCacheManager.calculateItemHash(item);
                DebugLogManager.logCacheMiss(null, hash);
            }
        }
        
        // 降级到直接解析
        Map<String, AttributeValue> attrs = LoreParser.parse(item);
        
        // 调试日志：Lore解析结果
        Map<String, Object> parseDetails = new LinkedHashMap<>();
        parseDetails.put("item", itemDesc);
        parseDetails.put("attrCount", attrs.size());
        DebugLogManager.log(null, DebugLogManager.Level.TRACE, 
            DebugLogManager.Operation.PARSE_LORE, "Lore解析: " + attrs.size() + "个属性", parseDetails);
        
        // 修复: 正确合并宝石属性到attrs，避免覆盖装备属性
        Map<String, AttributeValue> socketAttrs = GemParser.parseSocketGemsFromLore(item);
        if (!socketAttrs.isEmpty()) {
            // 调试日志：宝石解析
            Map<String, Object> gemDetails = new LinkedHashMap<>();
            gemDetails.put("gemAttrCount", socketAttrs.size());
            DebugLogManager.log(null, DebugLogManager.Level.TRACE, 
                DebugLogManager.Operation.PARSE_GEM, "宝石解析: " + socketAttrs.size() + "个属性", gemDetails);
            
            for (Map.Entry<String, AttributeValue> entry : socketAttrs.entrySet()) {
                attrs.merge(entry.getKey(), entry.getValue(), AttributeValue::merge);
            }
        }
        
        // 添加合并后的所有属性
        stats.addStats(attrs);

        if (skills != null) {
            List<String> itemSkills = SkillParser.parseSkillNames(item);
            skills.addAll(itemSkills);
        }
    }
    
    /**
     * 使装备缓存失效（装备修改时调用）
     * 
     * @param item 被修改的装备
     */
    public void invalidateEquipmentCache(ItemStack item) {
        if (equipmentCacheManager != null && item != null) {
            String hash = equipmentCacheManager.calculateItemHash(item);
            equipmentCacheManager.invalidate(hash);
        }
    }

    /**
     * 检查物品是否是武器
     * 识别方式（满足任一即可）：
     * 1. 材质在配置的武器材质列表中
     * 2. Lore第一行包含配置的首行关键词
     * 3. Lore任意行包含配置的关键词
     */
    public boolean isWeaponItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        // 检查材质
        if (!weaponMaterials.isEmpty() && weaponMaterials.contains(item.getType())) {
            return true;
        }

        // 检查Lore
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }

        // 检查第一行关键词
        if (!weaponFirstLineKeywords.isEmpty() && !lore.isEmpty()) {
            String firstLine = stripColor(lore.get(0));
            for (String keyword : weaponFirstLineKeywords) {
                if (firstLine.contains(keyword)) {
                    return true;
                }
            }
        }

        // 检查所有行关键词
        for (String line : lore) {
            String stripped = stripColor(line);
            for (String keyword : weaponLoreKeywords) {
                if (stripped.contains(keyword)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查物品是否是防具
     * 识别方式（满足任一即可）：
     * 1. 材质在配置的防具材质列表中
     * 2. Lore第一行包含配置的首行关键词
     * 3. Lore任意行包含配置的关键词
     */
    public boolean isArmorItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        // 检查材质
        if (!armorMaterials.isEmpty() && armorMaterials.contains(item.getType())) {
            return true;
        }

        // 检查Lore
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }

        // 检查第一行关键词
        if (!armorFirstLineKeywords.isEmpty() && !lore.isEmpty()) {
            String firstLine = stripColor(lore.get(0));
            for (String keyword : armorFirstLineKeywords) {
                if (firstLine.contains(keyword)) {
                    return true;
                }
            }
        }

        // 检查所有行关键词
        for (String line : lore) {
            String stripped = stripColor(line);
            for (String keyword : armorLoreKeywords) {
                if (stripped.contains(keyword)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查物品是否具有可解析的属性（用于判断是否需要解析）
     */
    public boolean hasParsableAttributes(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasLore();
    }

    /**
     * 移除颜色代码
     */
    private String stripColor(String input) {
        if (input == null) return "";
        return input.replaceAll("[&§][0-9a-fk-or]", "");
    }

    public void applyMaxHealth(Player player, PlayerStats stats, double savedHealth, double savedMaxHealth) {
        // stats.getMaxHealth() 是装备/武器提供的额外血量，玩家基础血量是 20
        double bonusHealth = stats.getMaxHealth();
        double effectiveMaxHealth = Math.min(20.0 + bonusHealth, MAX_HEALTH_LIMIT);

        AttributeInstance attributeInstance = player.getAttribute(Attribute.MAX_HEALTH);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(healthKey);

            if (effectiveMaxHealth > 20.0) {
                AttributeModifier modifier = new AttributeModifier(
                    healthKey,
                    effectiveMaxHealth - 20.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY
                );
                attributeInstance.addModifier(modifier);
            }

            double newMaxHealth = attributeInstance.getValue();

            double finalHealth;
            if (savedHealth <= 0) {
                // 保存的血量为0或负数（玩家死亡/新生），恢复到满血
                finalHealth = newMaxHealth;
            } else if (newMaxHealth > savedMaxHealth) {
                // 穿装备：血量上限增加，保持当前血量值不变
                // 例如：20/20 穿7万血装备 -> 20/70020
                finalHealth = Math.min(savedHealth, newMaxHealth);
            } else if (newMaxHealth < savedMaxHealth) {
                // 脱装备：血量上限减少，按比例缩放血量
                // 例如：50000/70020 脱7万血装备 -> 约14/20
                double healthPercent = savedHealth / savedMaxHealth;
                finalHealth = newMaxHealth * healthPercent;
                finalHealth = Math.max(1.0, Math.min(finalHealth, newMaxHealth));
            } else {
                // 血量上限不变，保持当前血量
                finalHealth = Math.min(savedHealth, newMaxHealth);
            }

            // 确保玩家血量被正确设置
            if (finalHealth > 0) {
                player.setHealth(finalHealth);
            }

            applyHealthDisplayScale(player, newMaxHealth);
        }
    }

    // 兼容旧方法调用
    public void applyMaxHealth(Player player, PlayerStats stats) {
        applyMaxHealth(player, stats, player.getHealth(), player.getAttribute(Attribute.MAX_HEALTH).getValue());
    }
    
    // 兼容旧方法名
    public void refreshPlayerStats(Player player) {
        refreshFullStats(player);
    }

    public void applyMoveSpeed(Player player, PlayerStats stats) {
        double moveSpeedPercent = stats.getMoveSpeedPercent();

        AttributeInstance attributeInstance = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(speedKey);

            if (moveSpeedPercent > 0) {
                double baseSpeed = 0.1;
                double additionalSpeed = baseSpeed * (moveSpeedPercent / 100.0);

                AttributeModifier modifier = new AttributeModifier(
                    speedKey,
                    additionalSpeed,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY
                );
                attributeInstance.addModifier(modifier);
            }
        }
    }

    public void clearPlayerAttributes(Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(healthKey);
            if (healthAttr.getBaseValue() != 20.0) {
                healthAttr.setBaseValue(20.0);
            }
        }

        resetHealthDisplayScale(player);

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(speedKey);
            if (speedAttr.getBaseValue() != 0.1) {
                speedAttr.setBaseValue(0.1);
            }
        }
    }
    
    /**
     * 清除玩家属性（内部方法，需在锁内调用）
     */
    private void clearPlayerAttributesInternal(Player player) {
        clearPlayerAttributes(player);
    }

    public void resetPlayer(Player player) {
        clearPlayerAttributes(player);
        removePlayer(player.getUniqueId());
    }

    public PlayerStats getPlayerStats(Player player) {
        return playerStatsMap.getOrDefault(player.getUniqueId(), new PlayerStats());
    }

    public List<String> getPlayerSkills(Player player) {
        return playerSkillsMap.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    public void removePlayer(UUID uuid) {
        playerStatsMap.remove(uuid);
        playerSkillsMap.remove(uuid);
        armorStatsCache.remove(uuid);
        weaponStatsCache.remove(uuid);
        accessoryStatsCache.remove(uuid);
        offHandStatsCache.remove(uuid);
        armorSkillsCache.remove(uuid);
        weaponSkillsCache.remove(uuid);
        accessorySkillsCache.remove(uuid);
        offHandSkillsCache.remove(uuid);
        weaponItemHashCache.remove(uuid);
        // 移除动态缓存记录
        cacheManager.removePlayer(uuid);
        // 移除登录加载标记
        loadedPlayers.remove(uuid);
    }

    public double calculateDamageReduction(double defense) {
        if (defenseDivisor <= 0) return 0;
        double reduction = defense / (defense + defenseDivisor);
        return Math.min(reduction, maxDamageReduction);
    }

    public boolean shouldDodge(Player defender) {
        if (!dodgeEnabled) return false;
        PlayerStats stats = getPlayerStats(defender);
        double dodgeChance = Math.min(stats.getDodgePercent() / 100.0, maxDodge);
        return ThreadLocalRandom.current().nextDouble() < dodgeChance;
    }

    public double applyCritResist(double critChance) {
        if (!critResistEnabled) return critChance;
        return Math.max(0, critChance - (critResistDamageReduction * 100));
    }

    private void applyHealthDisplayScale(Player player, double currentMaxHealth) {
        if (!healthDisplayScaleEnabled) {
            resetHealthDisplayScale(player);
            return;
        }

        double displayLimit = 20.0 * healthDisplayMaxRows;
        double scale = Math.min(Math.max(currentMaxHealth, 20.0), displayLimit);
        player.setHealthScaled(true);
        player.setHealthScale(scale);
    }

    private void resetHealthDisplayScale(Player player) {
        player.setHealthScaled(false);
    }

    public double getDefenseDivisor() { return defenseDivisor; }
    public double getMaxDamageReduction() { return maxDamageReduction; }
    public double getMinDamage() { return minDamage; }
    public boolean isDodgeEnabled() { return dodgeEnabled; }
    public double getMaxDodge() { return maxDodge; }
    public boolean isCritResistEnabled() { return critResistEnabled; }
    public double getCritResistDamageReduction() { return critResistDamageReduction; }
}