package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.config.AttributeApplyLogConfig;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.applier.SimpleAttributeApplier;
import cn.guangdian.armorstats.source.PlayerAttributeApplier;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增量属性管理器
 * 
 * 设计原则：
 * 1. 每个装备槽位独立缓存
 * 2. 只更新变化的槽位（增量更新）
 * 3. 合并所有槽位属性后一次性应用
 * 4. 同时应用 Minecraft 属性和存储战斗属性到 PDC
 * 
 * 槽位分类：
 * - HELMET (头盔)
 * - CHESTPLATE (胸甲)
 * - LEGGINGS (护腿)
 * - BOOTS (靴子)
 * - MAIN_HAND (主手武器)
 * - OFF_HAND (副手)
 */
public class IncrementalStatsManager {

    private static final double DEFAULT_MAX_HEALTH = 20.0;

    private final GuangDianArmorStats plugin;
    private final SimpleAttributeApplier applier;
    private final PlayerAttributeApplier combatApplier; // 战斗属性应用器
    private final AttributeApplyLogConfig logConfig;
    
    // 槽位枚举
    public enum Slot {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS,
        MAIN_HAND,
        OFF_HAND
    }
    
    // 每个槽位的属性缓存：UUID -> Slot -> PlayerStats
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Slot, PlayerStats>> slotStatsCache = new ConcurrentHashMap<>();
    
    // 玩家是否已加载
    private final ConcurrentHashMap<UUID, Boolean> loadedPlayers = new ConcurrentHashMap<>();

    // 宝石属性缓存：物品哈希 -> 宝石属性（避免每次装备变化都重新加载宝石数据）
    private final ConcurrentHashMap<String, java.util.Map<String, cn.guangdian.armorstats.data.AttributeValue>> gemAttrCache = new ConcurrentHashMap<>();
    
    public IncrementalStatsManager(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        this.applier = new SimpleAttributeApplier(plugin);
        this.combatApplier = new PlayerAttributeApplier();
        this.logConfig = AttributeApplyLogConfig.getInstance();
    }
    
    // ==================== 槽位变化处理 ====================
    
    /**
     * 处理装备槽位变化
     * 
     * @param player 玩家
     * @param slot 槽位
     * @param newItem 新物品
     */
    public void onSlotChange(Player player, Slot slot, ItemStack newItem) {
        UUID uuid = player.getUniqueId();
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[槽位变化] " + player.getName() + " - " + slot.name() +
                " -> " + (newItem == null ? "AIR" : newItem.getType().name()));
        }
        
        // 1. 更新该槽位的缓存
        updateSlotCache(uuid, slot, newItem);
        
        // 2. 合并所有槽位属性
        PlayerStats totalStats = mergeAllSlots(uuid);
        
        // 3. 应用到玩家
        applyToPlayer(player, totalStats);
    }
    
    /**
     * 处理快捷栏切换（主手武器变化）
     * 
     * @param player 玩家
     * @param newWeapon 新武器
     */
    public void onWeaponChange(Player player, ItemStack newWeapon) {
        onSlotChange(player, Slot.MAIN_HAND, newWeapon);
    }
    
    /**
     * 处理副手变化
     * 
     * @param player 玩家
     * @param newOffHand 新副手物品
     */
    public void onOffHandChange(Player player, ItemStack newOffHand) {
        onSlotChange(player, Slot.OFF_HAND, newOffHand);
    }
    
    // ==================== 缓存管理 ====================
    
    /**
     * 更新指定槽位的缓存
     */
    private void updateSlotCache(UUID uuid, Slot slot, ItemStack item) {
        ConcurrentHashMap<Slot, PlayerStats> playerSlots = slotStatsCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        
        if (item == null || item.getType().isAir()) {
            // 清除该槽位的属性
            playerSlots.remove(slot);
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[缓存更新] 清除槽位: " + slot.name());
            }
        } else {
            // 解析物品属性并缓存
            PlayerStats slotStats = parseItemStats(item);
            playerSlots.put(slot, slotStats);
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[缓存更新] 更新槽位: " + slot.name() +
                    ", 攻击: " + slotStats.getMinAttack() + "-" + slotStats.getMaxAttack() +
                    ", 生命: " + slotStats.getMaxHealth());
            }
        }
    }
    
    /**
     * 解析物品属性（只使用 PDC + 宝石属性）
     */
    private PlayerStats parseItemStats(ItemStack item) {
        PlayerStats stats = new PlayerStats();
        
        // 只从 PDC 读取属性（不使用 Lore 解析）
        java.util.Map<String, cn.guangdian.armorstats.data.AttributeValue> attrs = 
            cn.guangdian.armorstats.parser.PDCAttributeReader.readFromPDC(item);
        
        // 先添加装备属性（会累加）
        stats.addStats(attrs);
        
        // 读取宝石属性，再添加（会累加到装备属性上）
        java.util.Map<String, cn.guangdian.armorstats.data.AttributeValue> gemAttrs = getGemAttributes(item);
        stats.addStats(gemAttrs);
        
        // 输出添加后的属性
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[PDC解析] 物品: " + item.getType() +
                ", 属性数: " + attrs.size() +
                ", 攻击: " + stats.getMinAttack() + "-" + stats.getMaxAttack() +
                ", 防御: " + stats.getDefenseMin() + "-" + stats.getDefenseMax() +
                ", 生命: " + stats.getMaxHealth() +
                ", 暴击伤害: " + stats.getCritDamagePercent() + "%" +
                ", 移动速度: " + stats.getMoveSpeedPercent() + "%");
        }
        
        return stats;
    }
    
    /**
     * 从 GuangDianSocket 获取宝石属性（带缓存）
     * 缓存 key：物品哈希，避免每次装备变化都重新加载宝石数据
     */
    private java.util.Map<String, cn.guangdian.armorstats.data.AttributeValue> getGemAttributes(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return java.util.Collections.emptyMap();
        }

        // 计算物品哈希作为缓存 key
        String itemHash = cn.guangdian.armorstats.cache.EquipmentHash.calculate(item);

        // 先查缓存
        java.util.Map<String, cn.guangdian.armorstats.data.AttributeValue> cached = gemAttrCache.get(itemHash);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，执行原始加载逻辑
        java.util.Map<String, cn.guangdian.armorstats.data.AttributeValue> attrs = loadGemAttributes(item);

        // 写入缓存（上限 500，避免内存膨胀）
        if (gemAttrCache.size() < 500) {
            gemAttrCache.put(itemHash, attrs);
        }

        return attrs;
    }

    /**
     * 实际加载宝石属性（原 getGemAttributes 逻辑）
     */
    private java.util.Map<String, cn.guangdian.armorstats.data.AttributeValue> loadGemAttributes(ItemStack item) {
        java.util.Map<String, cn.guangdian.armorstats.data.AttributeValue> attrs = new java.util.HashMap<>();
        
        if (item == null || !item.hasItemMeta()) {
            return attrs;
        }
        
        try {
            // 获取 GuangDianSocket 插件
            org.bukkit.plugin.Plugin socketPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("GuangDianSocket");
            if (socketPlugin == null || !socketPlugin.isEnabled()) {
                return attrs;
            }
            
            // 获取 GemStorage
            cn.guangdian.socket.GuangDianSocket socket = (cn.guangdian.socket.GuangDianSocket) socketPlugin;
            cn.guangdian.socket.storage.GemStorage gemStorage = socket.getGemStorage();
            
            if (gemStorage == null) {
                return attrs;
            }
            
            // 加载宝石数据
            java.util.List<cn.guangdian.socket.model.GemData> gems = gemStorage.loadGems(item);
            if (gems == null || gems.isEmpty()) {
                return attrs;
            }
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[宝石属性] 找到 " + gems.size() + " 个宝石");
            }
            
            // 聚合所有宝石属性
            for (cn.guangdian.socket.model.GemData gem : gems) {
                String gemId = gem.getItemId();
                
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[宝石属性] 处理宝石: " + gemId);
                }
                
                // 解析宝石属性
                java.util.Map<String, cn.guangdian.socket.model.AttributeValue> socketAttrs = 
                    cn.guangdian.socket.parser.SocketParser.parseGemAttributesById(gemId);
                
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[宝石属性] 解析宝石 " + gemId + " 属性: " + socketAttrs.size() + " 个");
                }
                
                for (java.util.Map.Entry<String, cn.guangdian.socket.model.AttributeValue> entry : socketAttrs.entrySet()) {
                    String attrName = entry.getKey();
                    cn.guangdian.socket.model.AttributeValue gemValue = entry.getValue();
                    
                    // 正确处理范围值和单值
                    if (gemValue instanceof cn.guangdian.socket.model.AttributeValue.RangeValue) {
                        // 范围值 - 累加而不是覆盖
                        cn.guangdian.socket.model.AttributeValue.RangeValue rangeValue = 
                            (cn.guangdian.socket.model.AttributeValue.RangeValue) gemValue;
                        double min = rangeValue.getMin();
                        double max = rangeValue.getMax();
                        if (min > 0 || max > 0) {
                            // 检查是否已有该属性，如果有则累加
                            cn.guangdian.armorstats.data.AttributeValue existing = attrs.get(attrName);
                            if (existing != null) {
                                if (existing instanceof cn.guangdian.armorstats.data.AttributeValue.RangeValue) {
                                    cn.guangdian.armorstats.data.AttributeValue.RangeValue r =
                                        (cn.guangdian.armorstats.data.AttributeValue.RangeValue) existing;
                                    min += r.getMin();
                                    max += r.getMax();
                                }
                            }
                            attrs.put(attrName, cn.guangdian.armorstats.data.AttributeValue.ofRange(min, max));
                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("[宝石属性] " + attrName + ": " + min + "-" + max);
                            }
                        }
                    } else {
                        // 单值 - 累加而不是覆盖
                        double value = gemValue.getValue();
                        if (value > 0) {
                            // 检查是否已有该属性，如果有则累加
                            cn.guangdian.armorstats.data.AttributeValue existing = attrs.get(attrName);
                            if (existing != null) {
                                if (existing instanceof cn.guangdian.armorstats.data.AttributeValue.SingleValue) {
                                    value += ((cn.guangdian.armorstats.data.AttributeValue.SingleValue) existing).getValue();
                                }
                            }
                            attrs.put(attrName, cn.guangdian.armorstats.data.AttributeValue.of(value));
                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("[宝石属性] " + attrName + ": " + value);
                            }
                        }
                    }
                }
            }
            
            if (!attrs.isEmpty() && plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[宝石属性] 总属性: " + attrs.size() + " 个");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("获取宝石属性失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return attrs;
    }
    
    /**
     * 合并所有槽位的属性
     */
    private PlayerStats mergeAllSlots(UUID uuid) {
        PlayerStats total = new PlayerStats();
        
        ConcurrentHashMap<Slot, PlayerStats> playerSlots = slotStatsCache.get(uuid);
        if (playerSlots != null) {
            // 合并所有槽位的属性
            for (PlayerStats slotStats : playerSlots.values()) {
                total.addPlayerStats(slotStats);
            }
        }
        
        // 添加职业属性
        addClassStats(uuid, total);

        // 仅在调试模式下输出属性合并日志
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[属性合并] 总属性 - " +
                "生命: " + total.getMaxHealth() +
                ", 攻击: " + total.getMinAttack() + "-" + total.getMaxAttack() +
                ", 防御: " + total.getDefenseMin() + "-" + total.getDefenseMax() +
                ", 暴击伤害: " + total.getCritDamagePercent() + "%" +
                ", 移动速度: " + total.getMoveSpeedPercent() + "%");
        }

        return total;
    }
    
    /**
     * 添加职业属性
     * 通过 RPGCore ServiceRegistry 获取 ClassService
     */
    private void addClassStats(UUID uuid, PlayerStats stats) {
        // 通过 RPGCore 获取职业服务
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore == null) {
            return;
        }

        cn.guangdian.rpgcore.api.ServiceRegistry registry = rpgCore.getServiceRegistry();
        if (registry == null) {
            return;
        }

        // 获取 ClassService（不需要反射，直接使用 RPGCore 接口）
        cn.guangdian.rpgcore.service.api.ClassService classService =
            registry.getOptionalService(cn.guangdian.rpgcore.service.api.ClassService.class).orElse(null);

        if (classService == null) {
            return;
        }

        java.util.Map<String, Double> classStats = classService.getPlayerClassStats(uuid);

        if (classStats == null || classStats.isEmpty()) {
            return;
        }

        // 添加职业属性（使用 set 方法设置累加后的值）
        double health = classStats.getOrDefault("health", 0.0);
        if (health > 0) {
            stats.setMaxHealth(stats.getMaxHealth() + health);
        }

        double attack = classStats.getOrDefault("attack", 0.0);
        if (attack > 0) {
            stats.setMinAttack(stats.getMinAttack() + attack);
            stats.setMaxAttack(stats.getMaxAttack() + attack);
        }

        double defense = classStats.getOrDefault("defense", 0.0);
        if (defense > 0) {
            stats.setDefenseMin(stats.getDefenseMin() + defense);
            stats.setDefenseMax(stats.getDefenseMax() + defense);
        }

        double critChance = classStats.getOrDefault("critChance", 0.0);
        if (critChance > 0) {
            stats.setCritChancePercent(stats.getCritChancePercent() + critChance);
        }

        double critDamage = classStats.getOrDefault("critDamage", 0.0);
        if (critDamage > 0) {
            stats.setCritDamagePercent(stats.getCritDamagePercent() + critDamage);
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[职业属性] 攻击: " + attack +
                ", 防御: " + defense +
                ", 生命: " + health);
        }
    }
    
    // ==================== 属性应用 ====================
    
    /**
     * 应用属性到玩家
     * 
     * 同时执行：
     * 1. 应用 Minecraft 属性（生命、移动速度）
     * 2. 存储战斗属性到玩家 PDC（用于战斗计算）
     * 3. 应用生命值缩放（控制显示行数）
     */
    private void applyToPlayer(Player player, PlayerStats stats) {
        // 1. 清除旧 modifier
        applier.clearAll(player);
        
        // 2. 应用新 Minecraft 属性
        applier.applyAll(player, stats);
        
        // 3. 存储战斗属性到玩家 PDC
        combatApplier.apply(player, stats);
        
        // 4. 应用生命值缩放（控制显示行数）
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : DEFAULT_MAX_HEALTH;
        plugin.getHealthManager().applyHealthScale(player, maxHealth);
        
        // 5. 更新回血任务
        plugin.getRegenTask().updatePlayerRegen(
            player.getUniqueId(), 
            stats.getHealthRegen(), 
            stats.getHealthRegenPercent()
        );
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
            plugin.getLogger().info("[属性应用] " + player.getName() +
                " - 生命上限: " + maxHealth +
                ", 移动速度: " + (speedAttr != null ? speedAttr.getValue() : 0) +
                ", 攻击: " + stats.getMinAttack() + "-" + stats.getMaxAttack() +
                ", 防御: " + stats.getDefenseMin() + "-" + stats.getDefenseMax() +
                ", 每秒回血: " + stats.getHealthRegen() +
                ", 生命恢复: " + stats.getHealthRegenPercent() + "%");
        }
    }
    
    // ==================== 玩家生命周期 ====================
    
    /**
     * 玩家登录时初始化
     */
    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 清除旧缓存
        slotStatsCache.remove(uuid);
        
        // 初始化所有槽位
        ConcurrentHashMap<Slot, PlayerStats> playerSlots = new ConcurrentHashMap<>();
        slotStatsCache.put(uuid, playerSlots);
        
        // 解析当前装备
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        updateSlotCache(uuid, Slot.HELMET, inv.getHelmet());
        updateSlotCache(uuid, Slot.CHESTPLATE, inv.getChestplate());
        updateSlotCache(uuid, Slot.LEGGINGS, inv.getLeggings());
        updateSlotCache(uuid, Slot.BOOTS, inv.getBoots());
        // 主手/副手持防具时不解析属性
        updateSlotCache(uuid, Slot.MAIN_HAND, isArmorType(inv.getItemInMainHand()) ? null : inv.getItemInMainHand());
        updateSlotCache(uuid, Slot.OFF_HAND, isArmorType(inv.getItemInOffHand()) ? null : inv.getItemInOffHand());
        
        // 合并并应用
        PlayerStats totalStats = mergeAllSlots(uuid);
        applyToPlayer(player, totalStats);
        
        // 标记为已加载
        loadedPlayers.put(uuid, true);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[玩家登录] " + player.getName() + " 属性初始化完成");
        }
    }
    
    /**
     * 玩家退出时清理
     */
    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 清除缓存
        slotStatsCache.remove(uuid);
        loadedPlayers.remove(uuid);
        
        // 完全重置属性（包括移动速度）
        applier.resetAll(player);

        // 清理 PDC 战斗属性
        combatApplier.clearCombatAttributes(player);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[玩家退出] " + player.getName() + " 数据已清理");
        }
    }
    
    /**
     * 检查玩家是否已加载
     */
    public boolean isPlayerLoaded(UUID uuid) {
        return loadedPlayers.getOrDefault(uuid, false);
    }
    
    /**
     * 获取属性应用器（用于启动/停止速度监测等）
     */
    public SimpleAttributeApplier getApplier() {
        return applier;
    }
    
    /**
     * 获取玩家当前总属性
     */
    public PlayerStats getPlayerStats(UUID uuid) {
        return mergeAllSlots(uuid);
    }
    
    // ==================== 调试方法 ====================
    
    /**
     * 调试：输出玩家所有装备槽位的 PDC 数据
     * 
     * @param player 玩家
     */
    public void debugAllSlots(Player player) {
        plugin.getLogger().info("========== PDC 调试开始 ==========");
        plugin.getLogger().info("玩家: " + player.getName());
        
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        
        // 头盔
        plugin.getLogger().info("--- 槽位: HELMET ---");
        debugItemPDC(inv.getHelmet(), "  ");
        
        // 胸甲
        plugin.getLogger().info("--- 槽位: CHESTPLATE ---");
        debugItemPDC(inv.getChestplate(), "  ");
        
        // 护腿
        plugin.getLogger().info("--- 槽位: LEGGINGS ---");
        debugItemPDC(inv.getLeggings(), "  ");
        
        // 靴子
        plugin.getLogger().info("--- 槽位: BOOTS ---");
        debugItemPDC(inv.getBoots(), "  ");
        
        // 主手
        plugin.getLogger().info("--- 槽位: MAIN_HAND ---");
        debugItemPDC(inv.getItemInMainHand(), "  ");
        
        // 副手
        plugin.getLogger().info("--- 槽位: OFF_HAND ---");
        debugItemPDC(inv.getItemInOffHand(), "  ");
        
        plugin.getLogger().info("========== PDC 调试结束 ==========");
    }
    
    /**
     * 调试：输出单个物品的 PDC 数据
     */
    private void debugItemPDC(ItemStack item, String prefix) {
        if (item == null || item.getType().isAir()) {
            plugin.getLogger().info(prefix + "物品: AIR");
            return;
        }
        
        plugin.getLogger().info(prefix + "物品类型: " + item.getType());
        
        if (!item.hasItemMeta()) {
            plugin.getLogger().info(prefix + "没有 ItemMeta");
            return;
        }
        
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // 输出所有 PDC Keys
        plugin.getLogger().info(prefix + "PDC Keys 数量: " + pdc.getKeys().size());
        for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
            plugin.getLogger().info(prefix + "  Key: " + key.toString());
        }
        
        // 使用调试读取器
        java.util.Map<String, cn.guangdian.armorstats.data.AttributeValue> attrs = 
            cn.guangdian.armorstats.parser.PDCAttributeReader.readFromPDCWithDebug(item, prefix);
        
        plugin.getLogger().info(prefix + "解析属性数: " + attrs.size());
    }
    
    /**
     * 判断物品是否是防具类型（头盔/胸甲/护腿/靴子）
     * 防具只在装备栏生效，手持时不解析属性
     */
    private boolean isArmorType(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        Material type = item.getType();
        return type.name().endsWith("_HELMET") ||
               type.name().endsWith("_CHESTPLATE") ||
               type.name().endsWith("_LEGGINGS") ||
               type.name().endsWith("_BOOTS");
    }
}
