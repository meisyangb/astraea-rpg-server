package cn.guangdian.classsystem.manager;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        // 优先从 classes/ 文件夹加载多个文件
        File classesDir = new File(plugin.getDataFolder(), "classes");
        
        if (classesDir.exists() && classesDir.isDirectory()) {
            loadClassesFromDirectory(classesDir);
        }
        
        // 降级到单个 classes.yml（向后兼容）
        if (classes.isEmpty()) {
            loadClassesFromSingleFile();
        }
    }
    
    /**
     * 从文件夹加载多个职业配置文件
     */
    private void loadClassesFromDirectory(File classesDir) {
        File[] files = classesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("classes/ 文件夹为空，尝试加载 classes.yml");
            return;
        }
        
        int totalClasses = 0;
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            int loaded = loadClassesFromConfig(config, file.getName());
            totalClasses += loaded;
        }
        
        plugin.getLogger().info("从 classes/ 文件夹加载了 " + totalClasses + " 个职业（" + files.length + " 个文件）");
    }
    
    /**
     * 从单个文件加载职业配置（向后兼容）
     */
    private void loadClassesFromSingleFile() {
        classesFile = new File(plugin.getDataFolder(), "classes.yml");
        if (!classesFile.exists()) {
            plugin.saveResource("classes.yml", false);
        }
        classesConfig = YamlConfiguration.loadConfiguration(classesFile);
        
        int loaded = loadClassesFromConfig(classesConfig, "classes.yml");
        plugin.getLogger().info("从 classes.yml 加载了 " + loaded + " 个职业");
    }
    
    /**
     * 从配置加载职业
     * @return 加载的职业数量
     */
    private int loadClassesFromConfig(FileConfiguration config, String fileName) {
        ConfigurationSection classesSection = config.getConfigurationSection("classes");
        if (classesSection == null) {
            plugin.getLogger().warning(fileName + " 中未找到职业定义!");
            return 0;
        }
        
        int count = 0;
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
            
            // 加载克苏鲁风格途径系统字段
            gameClass.setPathway(classSection.getString("pathway", ""));
            gameClass.setSequence(classSection.getInt("sequence", 9));
            gameClass.setBranch(classSection.getString("branch", ""));
            
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
            
            gameClass.setAttributePoints(classSection.getInt("attribute-points", 0));
            
            classes.put(classId, gameClass);
            count++;
        }
        
        return count;
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
    
    /**
     * 通过中文名称查找职业
     */
    public GameClass getClassByName(String name) {
        for (GameClass gameClass : classes.values()) {
            if (name.equals(gameClass.getName())) {
                return gameClass;
            }
        }
        return null;
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
    
    // ========================================
    // 克苏鲁风格途径系统辅助方法
    // ========================================
    
    /**
     * 获取指定途径的所有职业
     */
    public List<GameClass> getClassesByPathway(String pathway) {
        return classes.values().stream()
            .filter(gc -> gc.getPathway() != null && gc.getPathway().equals(pathway))
            .sorted((a, b) -> Integer.compare(a.getSequence(), b.getSequence())) // 序列升序（9→0）
            .toList();
    }
    
    /**
     * 获取指定途径的初始职业（序列9）
     */
    public List<GameClass> getBaseClassesByPathway(String pathway) {
        return classes.values().stream()
            .filter(gc -> gc.getPathway() != null && gc.getPathway().equals(pathway))
            .filter(gc -> gc.getSequence() == 9)
            .toList();
    }
    
    /**
     * 获取指定序列的所有职业
     */
    public List<GameClass> getClassesBySequence(int sequence) {
        return classes.values().stream()
            .filter(gc -> gc.getSequence() == sequence)
            .toList();
    }
    
    /**
     * 获取指定途径和序列的职业
     */
    public GameClass getClassByPathwayAndSequence(String pathway, int sequence) {
        return classes.values().stream()
            .filter(gc -> gc.getPathway() != null && gc.getPathway().equals(pathway))
            .filter(gc -> gc.getSequence() == sequence)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取指定途径、序列和分支的职业
     */
    public GameClass getClassByPathwaySequenceBranch(String pathway, int sequence, String branch) {
        return classes.values().stream()
            .filter(gc -> gc.getPathway() != null && gc.getPathway().equals(pathway))
            .filter(gc -> gc.getSequence() == sequence)
            .filter(gc -> branch == null || branch.isEmpty() || 
                         (gc.getBranch() != null && gc.getBranch().equals(branch)))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取玩家可以晋升的下一序列职业
     */
    public List<GameClass> getNextSequenceClasses(PlayerClassData playerData) {
        GameClass currentClass = getClass(playerData.getClassId());
        if (currentClass == null) return new ArrayList<>();
        
        int currentSequence = currentClass.getSequence();
        if (currentSequence <= 0) return new ArrayList<>(); // 已是真神
        
        int nextSequence = currentSequence - 1; // 序列降低（更强）
        String pathway = currentClass.getPathway();
        String branch = currentClass.getBranch();
        
        // 如果当前有分支，查找同分支的下一职业
        if (branch != null && !branch.isEmpty()) {
            GameClass branchNext = getClassByPathwaySequenceBranch(pathway, nextSequence, branch);
            if (branchNext != null) {
                return List.of(branchNext);
            }
        }
        
        // 否则返回所有可能的下一职业（通过序列筛选）
        return classes.values().stream()
            .filter(gc -> gc.getPathway() != null && gc.getPathway().equals(pathway))
            .filter(gc -> gc.getSequence() == nextSequence)
            .toList();
    }
    
    /**
     * 获取途径列表
     */
    public List<String> getAllPathways() {
        return classes.values().stream()
            .map(GameClass::getPathway)
            .filter(p -> p != null && !p.isEmpty())
            .distinct()
            .toList();
    }
    
    /**
     * 获取途径名称（中文）
     */
    public String getPathwayName(String pathway) {
        return switch (pathway) {
            case "abyss" -> "深渊途径";
            case "void" -> "虚空途径";
            case "shadow" -> "暗影途径";
            case "corruption" -> "腐化途径";
            default -> "未知途径";
        };
    }
}
