package cn.guangdian.classsystem.restriction;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ClassRestrictionManager {
    
    private final GuangDianClass plugin;
    private final Map<String, ClassRestrictions> classRestrictions;
    private ExternalServiceIntegration externalServices;
    
    // PDC Keys - 与 RPGItems ItemFactory 保持一致
    private static final NamespacedKey KEY_ID = new NamespacedKey("rpgitems", "id");
    private static final NamespacedKey KEY_LEVEL = new NamespacedKey("rpgitems", "level");
    private static final NamespacedKey KEY_REQUIRED_CLASS = new NamespacedKey("rpgitems", "required_class");
    
    // 装备限制结果
    public static class EquipCheckResult {
        private final boolean allowed;
        private final String reason;
        private final String requiredClass;
        private final int requiredLevel;
        
        private EquipCheckResult(boolean allowed, String reason, String requiredClass, int requiredLevel) {
            this.allowed = allowed;
            this.reason = reason;
            this.requiredClass = requiredClass;
            this.requiredLevel = requiredLevel;
        }
        
        public static EquipCheckResult allowed() {
            return new EquipCheckResult(true, "", "", 0);
        }
        
        public static EquipCheckResult deniedClass(String requiredClass) {
            return new EquipCheckResult(false, "class", requiredClass, 0);
        }
        
        public static EquipCheckResult deniedLevel(int requiredLevel) {
            return new EquipCheckResult(false, "level", "", requiredLevel);
        }
        
        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
        public String getRequiredClass() { return requiredClass; }
        public int getRequiredLevel() { return requiredLevel; }
    }
    
    public ClassRestrictionManager(GuangDianClass plugin) {
        this.plugin = plugin;
        this.classRestrictions = new HashMap<>();
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.externalServices = rpgCore.getExternalServices();
        }
        
        loadRestrictions();
    }
    
    private void loadRestrictions() {
        ConfigurationSection restrictionsSection = plugin.getConfig().getConfigurationSection("restrictions");
        if (restrictionsSection == null) {
            plugin.getLogger().info("未配置职业限制");
            return;
        }
        
        for (String classId : restrictionsSection.getKeys(false)) {
            ConfigurationSection classSection = restrictionsSection.getConfigurationSection(classId);
            if (classSection == null) continue;
            
            ClassRestrictions restrictions = new ClassRestrictions();
            
            List<String> allowedItems = classSection.getStringList("allowed-items");
            restrictions.setAllowedItems(allowedItems);
            
            List<String> blockedItems = classSection.getStringList("blocked-items");
            restrictions.setBlockedItems(blockedItems);
            
            List<String> permissions = classSection.getStringList("required-permissions");
            restrictions.setRequiredPermissions(permissions);
            
            List<String> worlds = classSection.getStringList("allowed-worlds");
            restrictions.setAllowedWorlds(worlds);
            
            restrictions.setSkillMultiplier(classSection.getDouble("skill-multiplier", 1.0));
            restrictions.setExpMultiplier(classSection.getDouble("exp-multiplier", 1.0));
            
            classRestrictions.put(classId.toLowerCase(), restrictions);
        }
        
        plugin.getLogger().info("已加载 " + classRestrictions.size() + " 个职业限制配置");
    }
    
    public boolean canUseItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;
        
        PlayerClassData data = plugin.getPlayerData(player);
        if (data == null) return true;
        
        ClassRestrictions restrictions = classRestrictions.get(data.getClassId().toLowerCase());
        if (restrictions == null) return true;
        
        String materialName = item.getType().name().toLowerCase();
        
        if (!restrictions.getAllowedItems().isEmpty()) {
            boolean allowed = false;
            for (String allowedItem : restrictions.getAllowedItems()) {
                if (materialName.contains(allowedItem.toLowerCase())) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) return false;
        }
        
        for (String blockedItem : restrictions.getBlockedItems()) {
            if (materialName.contains(blockedItem.toLowerCase())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查玩家是否可以装备指定物品
     * 从物品 PDC 读取 rpgitems:required_class 和 rpgitems:level
     * 与玩家当前职业名称和阶位进行比对
     */
    public EquipCheckResult canEquipItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return EquipCheckResult.allowed();
        }
        
        if (!item.hasItemMeta()) {
            return EquipCheckResult.allowed();
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return EquipCheckResult.allowed();
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // 检查是否是 RPGItems 物品
        if (!pdc.has(KEY_ID, PersistentDataType.STRING)) {
            return EquipCheckResult.allowed();
        }
        
        // 读取装备限制属性
        String requiredClass = pdc.get(KEY_REQUIRED_CLASS, PersistentDataType.STRING);
        int requiredLevel = pdc.getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0);
        
        // 如果没有限制条件，允许装备
        if ((requiredClass == null || requiredClass.isEmpty()) && requiredLevel <= 0) {
            return EquipCheckResult.allowed();
        }
        
        // 获取玩家职业数据
        PlayerClassData data = plugin.getPlayerData(player);
        if (data == null) {
            // 没有职业数据的玩家不能穿戴有限制的装备
            if (requiredClass != null && !requiredClass.isEmpty()) {
                return EquipCheckResult.deniedClass(requiredClass);
            }
            if (requiredLevel > 0) {
                return EquipCheckResult.deniedLevel(requiredLevel);
            }
            return EquipCheckResult.allowed();
        }
        
        // 检查职业限制
        if (requiredClass != null && !requiredClass.isEmpty()) {
            if (!isClassMatch(player, data, requiredClass)) {
                return EquipCheckResult.deniedClass(requiredClass);
            }
        }
        
        // 检查等级限制（阶位 >= 装备等级）
        if (requiredLevel > 0) {
            if (data.getTier() < requiredLevel) {
                return EquipCheckResult.deniedLevel(requiredLevel);
            }
        }
        
        return EquipCheckResult.allowed();
    }
    
    /**
     * 检查玩家职业是否匹配装备要求
     * 支持中文名称匹配和 classId 匹配
     * 同时检查职业进化链（高阶职业可以穿戴低阶职业的装备）
     */
    private boolean isClassMatch(Player player, PlayerClassData data, String requiredClass) {
        String playerClassId = data.getClassId();
        if (playerClassId == null || playerClassId.isEmpty()) {
            return false;
        }
        
        // 获取玩家当前职业
        GameClass playerGameClass = plugin.getClassManager().getClass(playerClassId);
        if (playerGameClass == null) {
            return false;
        }
        
        String playerName = playerGameClass.getName();
        
        // 1. 精确匹配中文名称
        if (requiredClass.equals(playerName)) {
            return true;
        }
        
        // 2. 精确匹配 classId
        if (requiredClass.equalsIgnoreCase(playerClassId)) {
            return true;
        }
        
        // 3. 检查职业进化链 - 高阶职业可以穿戴低阶职业的装备
        // 例如：剑卫可以穿戴剑侍的装备，剑师可以穿戴剑卫和剑侍的装备
        if (isInClassEvolutionChain(playerClassId, requiredClass)) {
            return true;
        }
        
        // 4. 模糊匹配（中文名称包含关系）
        if (playerName.contains(requiredClass) || requiredClass.contains(playerName)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查玩家职业是否在目标职业的进化链中
     * 即玩家职业是否是目标职业的高阶进化
     */
    private boolean isInClassEvolutionChain(String playerClassId, String requiredClassIdOrName) {
        GameClass requiredGameClass = plugin.getClassManager().getClass(requiredClassIdOrName);
        
        // 如果按 classId 找不到，尝试按中文名称查找
        if (requiredGameClass == null) {
            requiredGameClass = plugin.getClassManager().getClassByName(requiredClassIdOrName);
        }
        
        if (requiredGameClass == null) {
            return false;
        }
        
        // 从玩家当前职业向上追溯进化链
        // 如果目标职业是玩家职业的祖先，则允许
        return isAncestorOf(requiredGameClass.getId(), playerClassId, new HashSet<>());
    }
    
    /**
     * 递归检查 targetId 是否是 playerClassId 的祖先职业
     */
    private boolean isAncestorOf(String targetId, String playerClassId, Set<String> visited) {
        if (visited.contains(playerClassId)) {
            return false;
        }
        visited.add(playerClassId);
        
        GameClass playerClass = plugin.getClassManager().getClass(playerClassId);
        if (playerClass == null) {
            return false;
        }
        
        // 如果目标就是当前职业
        if (targetId.equals(playerClassId)) {
            return true;
        }
        
        // 检查当前职业的前置职业
        String requiresClass = playerClass.getRequiresClass();
        if (requiresClass != null && !requiresClass.isEmpty()) {
            if (targetId.equals(requiresClass)) {
                return true;
            }
            // 继续向上追溯
            return isAncestorOf(targetId, requiresClass, visited);
        }
        
        return false;
    }
    
    public boolean hasRequiredPermissions(Player player, String classId) {
        ClassRestrictions restrictions = classRestrictions.get(classId.toLowerCase());
        if (restrictions == null) return true;
        
        for (String permission : restrictions.getRequiredPermissions()) {
            if (!player.hasPermission(permission)) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean isInAllowedWorld(Player player, String classId) {
        ClassRestrictions restrictions = classRestrictions.get(classId.toLowerCase());
        if (restrictions == null) return true;
        
        if (restrictions.getAllowedWorlds().isEmpty()) return true;
        
        String worldName = player.getWorld().getName();
        return restrictions.getAllowedWorlds().contains(worldName);
    }
    
    public boolean canChooseClass(Player player, GameClass gameClass) {
        if (!hasRequiredPermissions(player, gameClass.getId())) {
            return false;
        }
        
        if (!isInAllowedWorld(player, gameClass.getId())) {
            return false;
        }
        
        return true;
    }
    
    public double getSkillMultiplier(Player player) {
        PlayerClassData data = plugin.getPlayerData(player);
        if (data == null) return 1.0;
        
        ClassRestrictions restrictions = classRestrictions.get(data.getClassId().toLowerCase());
        if (restrictions == null) return 1.0;
        
        return restrictions.getSkillMultiplier();
    }
    
    public double getExpMultiplier(Player player) {
        PlayerClassData data = plugin.getPlayerData(player);
        if (data == null) return 1.0;
        
        ClassRestrictions restrictions = classRestrictions.get(data.getClassId().toLowerCase());
        if (restrictions == null) return 1.0;
        
        return restrictions.getExpMultiplier();
    }
    
    public ClassRestrictions getRestrictions(String classId) {
        return classRestrictions.get(classId.toLowerCase());
    }
    
    public void reload() {
        classRestrictions.clear();
        loadRestrictions();
    }
    
    public static class ClassRestrictions {
        private List<String> allowedItems = new ArrayList<>();
        private List<String> blockedItems = new ArrayList<>();
        private List<String> requiredPermissions = new ArrayList<>();
        private List<String> allowedWorlds = new ArrayList<>();
        private double skillMultiplier = 1.0;
        private double expMultiplier = 1.0;
        
        public List<String> getAllowedItems() { return allowedItems; }
        public void setAllowedItems(List<String> allowedItems) { this.allowedItems = allowedItems; }
        
        public List<String> getBlockedItems() { return blockedItems; }
        public void setBlockedItems(List<String> blockedItems) { this.blockedItems = blockedItems; }
        
        public List<String> getRequiredPermissions() { return requiredPermissions; }
        public void setRequiredPermissions(List<String> requiredPermissions) { this.requiredPermissions = requiredPermissions; }
        
        public List<String> getAllowedWorlds() { return allowedWorlds; }
        public void setAllowedWorlds(List<String> allowedWorlds) { this.allowedWorlds = allowedWorlds; }
        
        public double getSkillMultiplier() { return skillMultiplier; }
        public void setSkillMultiplier(double skillMultiplier) { this.skillMultiplier = skillMultiplier; }
        
        public double getExpMultiplier() { return expMultiplier; }
        public void setExpMultiplier(double expMultiplier) { this.expMultiplier = expMultiplier; }
    }
}
