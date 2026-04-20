package cn.guangdian.classsystem.manager;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.model.AttributeEffect;
import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ClassManager {
    
    private final GuangDianClass plugin;
    private final Map<String, GameClass> classes;
    private final Map<Integer, Long> tierExpRequirements;
    private File classesFile;
    private FileConfiguration classesConfig;
    
    public ClassManager(GuangDianClass plugin) {
        this.plugin = plugin;
        this.classes = new HashMap<>();
        this.tierExpRequirements = new HashMap<>();
        loadClasses();
        loadTierExpRequirements();
    }
    
    private void loadClasses() {
        classesFile = new File(plugin.getDataFolder(), "classes.yml");
        if (!classesFile.exists()) {
            plugin.saveResource("classes.yml", false);
        }
        classesConfig = YamlConfiguration.loadConfiguration(classesFile);
        
        ConfigurationSection classesSection = classesConfig.getConfigurationSection("classes");
        if (classesSection == null) {
            plugin.getLogger().warning("classes.yml 中未找到职业定义!");
            return;
        }
        
        for (String classId : classesSection.getKeys(false)) {
            ConfigurationSection classSection = classesSection.getConfigurationSection(classId);
            if (classSection == null) continue;
            
            GameClass gameClass = new GameClass();
            gameClass.setId(classId);
            gameClass.setName(classSection.getString("name", classId));
            gameClass.setTier(classSection.getInt("tier", 1));
            gameClass.setAdvancement(classSection.getInt("advancement", 0));
            gameClass.setDescription(classSection.getString("description", ""));
            gameClass.setRequiresClass(classSection.getString("requires-class"));
            gameClass.setClassType(classSection.getString("class-type", "WARRIOR"));
            
            List<String> nextClasses = classSection.getStringList("next-classes");
            gameClass.setNextClasses(nextClasses);
            
            ConfigurationSection statsSection = classSection.getConfigurationSection("stats");
            if (statsSection != null) {
                Map<String, Double> stats = new HashMap<>();
                for (String statKey : statsSection.getKeys(false)) {
                    stats.put(statKey, statsSection.getDouble(statKey, 0));
                }
                gameClass.setStats(stats);
            }
            
            List<String> skills = classSection.getStringList("skills");
            gameClass.setSkills(skills);
            
            loadAvailableAttributes(gameClass, classSection);
            loadAttributeEffects(gameClass, classSection);
            loadLevelUpConfig(gameClass, classSection);
            
            classes.put(classId, gameClass);
        }
        
        plugin.getLogger().info("已加载 " + classes.size() + " 个职业");
    }
    
    private void loadAvailableAttributes(GameClass gameClass, ConfigurationSection classSection) {
        List<String> attrIds = classSection.getStringList("available-attributes");
        List<AttributeType> availableAttrs = new ArrayList<>();
        
        for (String attrId : attrIds) {
            AttributeType type = AttributeType.fromId(attrId);
            if (type != null) {
                availableAttrs.add(type);
            } else {
                plugin.getLogger().warning("职业 " + gameClass.getId() + " 中未知的属性类型: " + attrId);
            }
        }
        
        gameClass.setAvailableAttributes(availableAttrs);
    }
    
    private void loadAttributeEffects(GameClass gameClass, ConfigurationSection classSection) {
        ConfigurationSection effectsSection = classSection.getConfigurationSection("attribute-effects");
        if (effectsSection == null) return;
        
        Map<AttributeType, AttributeEffect> effects = new HashMap<>();
        
        for (String attrId : effectsSection.getKeys(false)) {
            AttributeType type = AttributeType.fromId(attrId);
            if (type == null) {
                plugin.getLogger().warning("职业 " + gameClass.getId() + " 中未知的属性效果类型: " + attrId);
                continue;
            }
            
            ConfigurationSection effectSection = effectsSection.getConfigurationSection(attrId);
            if (effectSection == null) continue;
            
            Map<String, Object> effectConfig = new HashMap<>();
            for (String key : effectSection.getKeys(false)) {
                effectConfig.put(key, effectSection.get(key));
            }
            
            AttributeEffect effect = AttributeEffect.fromConfig(type, effectConfig);
            effects.put(type, effect);
        }
        
        gameClass.setAttributeEffects(effects);
    }
    
    private void loadLevelUpConfig(GameClass gameClass, ConfigurationSection classSection) {
        ConfigurationSection levelUpSection = classSection.getConfigurationSection("level-up");
        if (levelUpSection == null) return;
        
        gameClass.setPointsPerLevel(levelUpSection.getInt("points-per-level", 3));
        
        ConfigurationSection bonusSection = levelUpSection.getConfigurationSection("bonus-at-tiers");
        if (bonusSection != null) {
            Map<Integer, Integer> bonusAtTiers = new HashMap<>();
            for (String tierKey : bonusSection.getKeys(false)) {
                try {
                    int tier = Integer.parseInt(tierKey);
                    int bonus = bonusSection.getInt(tierKey, 0);
                    bonusAtTiers.put(tier, bonus);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("无效的阶位编号: " + tierKey);
                }
            }
            gameClass.setBonusAtTiers(bonusAtTiers);
        }
    }
    
    private void loadTierExpRequirements() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection tierSection = config.getConfigurationSection("tier-exp-requirements");
        
        if (tierSection == null) {
            plugin.getLogger().warning("config.yml 中未找到阶位经验需求定义!");
            return;
        }
        
        for (String tierKey : tierSection.getKeys(false)) {
            try {
                int tier = Integer.parseInt(tierKey);
                long exp = tierSection.getLong(tierKey, 0);
                tierExpRequirements.put(tier, exp);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("无效的阶位编号: " + tierKey);
            }
        }
    }
    
    public GameClass getClass(String classId) {
        return classes.get(classId);
    }
    
    public Collection<GameClass> getAllClasses() {
        return classes.values();
    }
    
    public List<GameClass> getBaseClasses() {
        return classes.values().stream()
            .filter(GameClass::isBaseClass)
            .toList();
    }
    
    public List<GameClass> getAvailableClasses(PlayerClassData playerData) {
        GameClass currentClass = getClass(playerData.getClassId());
        if (currentClass == null) {
            return getBaseClasses();
        }
        
        return currentClass.getNextClasses().stream()
            .map(this::getClass)
            .filter(Objects::nonNull)
            .filter(gc -> canAdvanceTo(playerData, gc))
            .toList();
    }
    
    public boolean canAdvanceTo(PlayerClassData playerData, GameClass targetClass) {
        if (targetClass == null) return false;
        
        GameClass currentClass = getClass(playerData.getClassId());
        if (currentClass == null) return false;
        
        if (!currentClass.canAdvanceTo(targetClass.getId())) return false;
        
        int requiredTier = getAdvancementTier(targetClass.getAdvancement());
        if (playerData.getTier() < requiredTier) return false;
        
        if (targetClass.getRequiresClass() != null && 
            !targetClass.getRequiresClass().equals(playerData.getClassId())) {
            return false;
        }
        
        return true;
    }
    
    public int getAdvancementTier(int advancementLevel) {
        return switch (advancementLevel) {
            case 1 -> plugin.getConfig().getInt("advancement-tiers.first", 3);
            case 2 -> plugin.getConfig().getInt("advancement-tiers.second", 6);
            case 3 -> plugin.getConfig().getInt("advancement-tiers.third", 8);
            case 4 -> plugin.getConfig().getInt("advancement-tiers.divine", 9);
            default -> 1;
        };
    }
    
    public long getExpRequiredForTier(int tier) {
        return tierExpRequirements.getOrDefault(tier, 0L);
    }
    
    public long getExpRequiredForNextTier(int currentTier) {
        return getExpRequiredForTier(currentTier + 1);
    }
    
    public boolean canTierUp(PlayerClassData playerData) {
        int maxTier = plugin.getConfig().getInt("settings.max-tier", 9);
        if (playerData.getTier() >= maxTier) return false;
        
        long required = getExpRequiredForNextTier(playerData.getTier());
        return playerData.getExp() >= required;
    }
    
    public void reload() {
        classes.clear();
        tierExpRequirements.clear();
        loadClasses();
        loadTierExpRequirements();
    }
}
