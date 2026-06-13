package cn.guangdian.classsystem.manager;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.model.PlayerSkillData;
import cn.guangdian.classsystem.model.SkillOrb;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 技能空间管理器
 * 
 * 管理玩家的技能空间、快捷栏绑定、技能球创建等
 */
public class SkillSpaceManager {

    private final GuangDianClass plugin;
    private final Map<UUID, PlayerSkillData> playerDataMap;
    private final Map<String, SkillOrb> skillTemplates; // 技能模板(从配置加载)
    
    // PDC Keys
    private final NamespacedKey skillIdKey;
    private final NamespacedKey skillTypeKey;
    
    public SkillSpaceManager(GuangDianClass plugin) {
        this.plugin = plugin;
        this.playerDataMap = new ConcurrentHashMap<>();
        this.skillTemplates = new HashMap<>();
        
        // 初始化PDC Keys
        this.skillIdKey = new NamespacedKey(plugin, "skill_id");
        this.skillTypeKey = new NamespacedKey(plugin, "skill_type");
        
        // 加载技能模板
        loadSkillTemplates();
        
        plugin.getLogger().info("技能空间管理器已初始化,加载了 " + skillTemplates.size() + " 个技能模板");
    }
    
    /**
     * 从配置文件加载技能模板
     */
    private void loadSkillTemplates() {
        File skillsFile = new File(plugin.getDataFolder(), "skills.yml");
        if (!skillsFile.exists()) {
            plugin.saveResource("skills.yml", false);
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(skillsFile);
        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        
        if (skillsSection == null) {
            plugin.getLogger().warning("skills.yml 中未找到 skills 配置节");
            return;
        }
        
        for (String skillId : skillsSection.getKeys(false)) {
            ConfigurationSection skillConfig = skillsSection.getConfigurationSection(skillId);
            if (skillConfig == null) continue;
            
            try {
                SkillOrb skill = parseSkillFromConfig(skillId, skillConfig);
                skillTemplates.put(skillId, skill);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "加载技能 " + skillId + " 失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 从配置解析技能
     */
    private SkillOrb parseSkillFromConfig(String skillId, ConfigurationSection config) {
        String name = config.getString("name", skillId);
        String typeStr = config.getString("type", "active");
        SkillOrb.SkillType type = "passive".equalsIgnoreCase(typeStr) ? 
            SkillOrb.SkillType.PASSIVE : SkillOrb.SkillType.ACTIVE;
        
        String materialStr = config.getString("material", "BLAZE_POWDER");
        Material material = Material.matchMaterial(materialStr);
        if (material == null) material = Material.BLAZE_POWDER;
        
        int customModelData = config.getInt("custom_model_data", 0);
        String description = config.getString("description", "");
        double damageMult = config.getDouble("damage_mult", 1.0);
        double range = config.getDouble("range", 4.0);
        int cooldown = config.getInt("cooldown", 5);
        int manaCost = config.getInt("mana_cost", 0);
        int requiredTier = config.getInt("required_tier", 1);
        String effect = config.getString("effect", "none");
        double triggerChance = config.getDouble("trigger_chance", 100.0);
        
        List<String> statusEffects = config.getStringList("status_effects");
        
        return new SkillOrb(skillId, name, type, material, customModelData, description,
                          damageMult, range, cooldown, manaCost, requiredTier, effect,
                          triggerChance, statusEffects);
    }
    
    /**
     * 获取或创建玩家技能数据
     */
    public PlayerSkillData getPlayerSkillData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerSkillData data = new PlayerSkillData(uuid);
            // 初始化技能空间(复制模板)
            for (SkillOrb template : skillTemplates.values()) {
                data.addSkill(new SkillOrb(
                    template.getSkillId(),
                    template.getName(),
                    template.getType(),
                    template.getMaterial(),
                    template.getCustomModelData(),
                    template.getDescription(),
                    template.getDamageMult(),
                    template.getRange(),
                    template.getCooldown(),
                    template.getManaCost(),
                    template.getRequiredTier(),
                    template.getEffect(),
                    template.getTriggerChance(),
                    template.getStatusEffects()
                ));
            }
            return data;
        });
    }
    
    /**
     * 创建技能球物品(主动技能)
     */
    public ItemStack createSkillOrb(SkillOrb skill) {
        ItemStack orb = new ItemStack(skill.getMaterial());
        ItemMeta meta = orb.getItemMeta();
        
        // 设置显示名称
        meta.displayName(Component.text("§6⚡ " + skill.getName())
            .decorate(TextDecoration.BOLD));
        
        // 设置Lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7类型: §e主动技能"));
        lore.add(Component.text("§7伤害倍率: §c" + skill.getDamageMult() + "x"));
        lore.add(Component.text("§7范围: §a" + skill.getRange() + " 格"));
        lore.add(Component.text("§7冷却: §b" + skill.getCooldown() + " 秒"));
        lore.add(Component.text("§7法力消耗: §d" + skill.getManaCost()));
        lore.add(Component.empty());
        lore.add(Component.text("§f" + skill.getDescription()));
        lore.add(Component.empty());
        lore.add(Component.text("§a§l右键 §7使用技能"));
        lore.add(Component.text("§e§lShift+右键 §7打开技能空间"));
        
        meta.lore(lore);
        
        // 设置自定义模型数据
        if (skill.getCustomModelData() > 0) {
            meta.setCustomModelData(skill.getCustomModelData());
        }
        
        // 存储技能ID到PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(skillIdKey, PersistentDataType.STRING, skill.getSkillId());
        pdc.set(skillTypeKey, PersistentDataType.STRING, "active");
        
        orb.setItemMeta(meta);
        return orb;
    }
    
    /**
     * 创建技能球物品(被动技能)
     */
    public ItemStack createPassiveSkillOrb(SkillOrb skill) {
        ItemStack orb = new ItemStack(skill.getMaterial());
        ItemMeta meta = orb.getItemMeta();
        
        meta.displayName(Component.text("§b✨ " + skill.getName())
            .decorate(TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7类型: §b被动技能"));
        lore.add(Component.text("§7触发概率: §a" + skill.getTriggerChance() + "%"));
        lore.add(Component.empty());
        lore.add(Component.text("§f" + skill.getDescription()));
        lore.add(Component.empty());
        lore.add(Component.text("§7被动技能自动生效"));
        
        meta.lore(lore);
        
        if (skill.getCustomModelData() > 0) {
            meta.setCustomModelData(skill.getCustomModelData());
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(skillIdKey, PersistentDataType.STRING, skill.getSkillId());
        pdc.set(skillTypeKey, PersistentDataType.STRING, "passive");
        
        orb.setItemMeta(meta);
        return orb;
    }
    
    /**
     * 从物品获取技能ID
     */
    public String getSkillIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(skillIdKey, PersistentDataType.STRING);
    }
    
    /**
     * 从物品获取技能类型
     */
    public String getSkillTypeFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(skillTypeKey, PersistentDataType.STRING);
    }
    
    /**
     * 绑定技能到快捷栏
     */
    public void bindSkillToHotbar(Player player, String skillId, int slot) {
        if (slot < 0 || slot > 8) {
            player.sendMessage(Component.text("§c无效的快捷栏槽位!"));
            return;
        }
        
        PlayerSkillData data = getPlayerSkillData(player);
        SkillOrb skill = data.getSkill(skillId);
        
        if (skill == null) {
            player.sendMessage(Component.text("§c技能不存在!"));
            return;
        }
        
        // 检查是否为主动技能
        if (skill.isPassive()) {
            player.sendMessage(Component.text("§c被动技能无法绑定到快捷栏!"));
            return;
        }
        
        // 检查是否解锁
        if (!skill.isUnlocked()) {
            player.sendMessage(Component.text("§c技能未解锁!"));
            return;
        }
        
        // 绑定技能
        data.bindSkillToHotbar(slot, skillId);
        
        // 更新玩家快捷栏
        player.getInventory().setItem(slot, createSkillOrb(skill));
        
        player.sendMessage(Component.text("§a技能 §e" + skill.getName() + 
            " §a已绑定到快捷栏 §6" + (slot + 1)));
        
        // 保存数据
        savePlayerData(player);
    }
    
    /**
     * 解绑快捷栏技能
     */
    public void unbindSkillFromHotbar(Player player, int slot) {
        if (slot < 0 || slot > 8) return;
        
        PlayerSkillData data = getPlayerSkillData(player);
        String skillId = data.getHotbarBinding(slot);
        
        if (skillId != null) {
            // 清空快捷栏槽位
            player.getInventory().setItem(slot, new ItemStack(Material.AIR));
            
            SkillOrb skill = data.getSkill(skillId);
            data.unbindSkillFromHotbar(slot);
            
            if (skill != null) {
                player.sendMessage(Component.text("§c技能 §e" + skill.getName() + 
                    " §c已从快捷栏移除"));
            }
            
            savePlayerData(player);
        }
    }
    
    /**
     * 解锁技能
     */
    public void unlockSkill(Player player, String skillId) {
        PlayerSkillData data = getPlayerSkillData(player);
        SkillOrb skill = data.getSkill(skillId);
        
        if (skill == null) {
            plugin.getLogger().warning("尝试解锁不存在的技能: " + skillId);
            return;
        }
        
        if (!skill.isUnlocked()) {
            data.unlockSkill(skillId);
            
            player.sendMessage(Component.text("§6解锁新技能: §e" + skill.getName()));
            
            // 如果是被动技能,通知玩家
            if (skill.isPassive()) {
                player.sendMessage(Component.text("§b被动技能已自动激活!"));
            }
            
            savePlayerData(player);
        }
    }
    
    /**
     * 根据职业等级解锁技能
     */
    public void unlockSkillsByTier(Player player, int tier) {
        PlayerSkillData data = getPlayerSkillData(player);
        
        // 解锁所有 required_tier <= 当前阶位 的技能
        for (SkillOrb skill : data.getSkillSpace().values()) {
            if (skill.getRequiredTier() <= tier && !skill.isUnlocked()) {
                unlockSkill(player, skill.getSkillId());
            }
        }
    }
    
    /**
     * 保存玩家数据
     */
    public void savePlayerData(Player player) {
        PlayerSkillData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;
        
        File dataFile = new File(plugin.getDataFolder(), "data/skills/" + player.getUniqueId() + ".yml");
        dataFile.getParentFile().mkdirs();
        
        YamlConfiguration config = new YamlConfiguration();
        
        // 保存快捷栏绑定
        for (Map.Entry<Integer, String> entry : data.getHotbarBindings().entrySet()) {
            config.set("hotbar." + entry.getKey(), entry.getValue());
        }
        
        // 保存解锁的技能
        List<String> unlockedSkills = new ArrayList<>();
        for (SkillOrb skill : data.getSkillSpace().values()) {
            if (skill.isUnlocked()) {
                unlockedSkills.add(skill.getSkillId());
            }
        }
        config.set("unlocked", unlockedSkills);
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "保存玩家技能数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 加载玩家数据
     */
    public void loadPlayerData(Player player) {
        PlayerSkillData data = getPlayerSkillData(player);
        
        File dataFile = new File(plugin.getDataFolder(), "data/skills/" + player.getUniqueId() + ".yml");
        if (!dataFile.exists()) return;
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        
        // 加载快捷栏绑定
        ConfigurationSection hotbarSection = config.getConfigurationSection("hotbar");
        if (hotbarSection != null) {
            for (String key : hotbarSection.getKeys(false)) {
                int slot = Integer.parseInt(key);
                String skillId = hotbarSection.getString(key);
                if (skillId != null) {
                    data.bindSkillToHotbar(slot, skillId);
                    
                    // 更新快捷栏
                    SkillOrb skill = data.getSkill(skillId);
                    if (skill != null && skill.isUnlocked() && skill.isActive()) {
                        player.getInventory().setItem(slot, createSkillOrb(skill));
                    }
                }
            }
        }
        
        // 加载解锁的技能
        List<String> unlockedSkills = config.getStringList("unlocked");
        for (String skillId : unlockedSkills) {
            data.unlockSkill(skillId);
        }
    }
    
    /**
     * 清理玩家数据
     */
    public void clearPlayerData(Player player) {
        savePlayerData(player);
        playerDataMap.remove(player.getUniqueId());
    }
    
    /**
     * 获取技能模板
     */
    public SkillOrb getSkillTemplate(String skillId) {
        return skillTemplates.get(skillId);
    }
    
    /**
     * 获取所有技能模板
     */
    public Map<String, SkillOrb> getSkillTemplates() {
        return Collections.unmodifiableMap(skillTemplates);
    }
}
