package cn.guangdian.classsystem.adapter;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.data.ClassDataHandler;
import cn.guangdian.classsystem.manager.AttributeManager;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.manager.ExpManager;
import cn.guangdian.classsystem.model.AdvancementLevel;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.annotation.RPGService;
import cn.guangdian.rpgcore.api.ServicePriority;
import cn.guangdian.rpgcore.api.ServiceRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 职业服务适配器
 *
 * <p>实现 ClassService 接口，提供职业系统服务。</p>
 * <p>使用 @RPGService 注解自动注册到 RPGCore 服务系统。</p>
 */
@RPGService(serviceInterface = cn.guangdian.rpgcore.service.api.ClassService.class, priority = ServicePriority.NORMAL)
public class ClassServiceAdapter implements ClassService {
    
    private final GuangDianClass plugin;
    private final ClassManager classManager;
    private final ExpManager expManager;
    private final ClassDataHandler dataHandler;
    private final AttributeManager attributeManager;
    private boolean usingRPGCore;
    
    public ClassServiceAdapter(GuangDianClass plugin, ClassManager classManager, 
                                ExpManager expManager, ClassDataHandler dataHandler,
                                AttributeManager attributeManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.expManager = expManager;
        this.dataHandler = dataHandler;
        this.attributeManager = attributeManager;
        
        registerWithRPGCore();
    }
    
    private void registerWithRPGCore() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            if (registry != null) {
                registry.registerService(cn.guangdian.rpgcore.service.api.ClassService.class, this);
                usingRPGCore = true;
                plugin.getLogger().info("已注册到 RPGCore 服务系统");
            }
        }
    }
    
    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            if (registry != null) {
                registry.unregisterService(cn.guangdian.rpgcore.service.api.ClassService.class);
            }
        }
    }
    
    public boolean isUsingRPGCore() {
        return usingRPGCore;
    }
    
    // ==================== RPGCore ClassService 接口实现 ====================
    
    @Override
    public String getPlayerClassName(UUID playerId) {
        PlayerClassData data = dataHandler.getPlayerData(playerId);
        if (data == null) return null;

        GameClass gameClass = classManager.getClass(data.getClassId());
        return gameClass != null ? gameClass.getName() : null;
    }

    @Override
    public String getPlayerClassId(UUID playerId) {
        PlayerClassData data = dataHandler.getPlayerData(playerId);
        return data != null ? data.getClassId() : null;
    }

    @Override
    public int getPlayerTier(UUID playerId) {
        PlayerClassData data = dataHandler.getPlayerData(playerId);
        return data != null ? data.getTier() : 1;
    }
    
    @Override
    public long getPlayerExp(UUID playerId) {
        PlayerClassData data = dataHandler.getPlayerData(playerId);
        return data != null ? data.getExp() : 0;
    }
    
    @Override
    public int getPlayerAdvancementLevel(UUID playerId) {
        PlayerClassData data = dataHandler.getPlayerData(playerId);
        return data != null ? data.getAdvancementLevel() : 0;
    }
    
    @Override
    public Map<String, Double> getPlayerClassStats(UUID playerId) {
        PlayerClassData data = dataHandler.getPlayerData(playerId);
        if (data == null) return Collections.emptyMap();
        
        GameClass gameClass = classManager.getClass(data.getClassId());
        if (gameClass == null) return Collections.emptyMap();
        
        Map<String, Double> stats = new HashMap<>(gameClass.getStats());
        
        AttributeManager.AttributeBonus attrBonus = attributeManager.calculateTotalBonus(playerId);
        if (attrBonus != null) {
            stats.merge("health", attrBonus.health, Double::sum);
            stats.merge("attack", attrBonus.attack, Double::sum);
            stats.merge("defense", attrBonus.defense, Double::sum);
            stats.merge("critChance", attrBonus.critChance, Double::sum);
            stats.merge("critDamage", attrBonus.critDamage, Double::sum);
            stats.merge("dodge", attrBonus.dodge, Double::sum);
            stats.merge("mana", attrBonus.mana, Double::sum);
        }
        
        int tier = data.getTier();
        if (tier > 1) {
            double tierMultiplier = 1.0 + (tier - 1) * 0.1;
            double tierHealth = (tier - 1) * 10;
            stats.merge("health", tierHealth, Double::sum);
            stats.put("tierMultiplier", tierMultiplier);
        }
        
        int advancement = data.getAdvancementLevel();
        double advancementMultiplier = AdvancementLevel.getMultiplier(advancement);
        if (advancementMultiplier > 1.0) {
            stats.put("advancementMultiplier", advancementMultiplier);
        }
        
        return stats;
    }
    
    @Override
    public double getPlayerClassStat(UUID playerId, String statName) {
        Map<String, Double> stats = getPlayerClassStats(playerId);
        return stats.getOrDefault(statName, 0.0);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
    
    // ==================== GuangDianClass 扩展方法实现 ====================
    
    @Override
    public PlayerClassData getPlayerData(UUID playerId) {
        return dataHandler.getPlayerData(playerId);
    }
    
    @Override
    public GameClass getClass(String classId) {
        return classManager.getClass(classId);
    }
    
    @Override
    public Collection<GameClass> getAllClasses() {
        return classManager.getAllClasses();
    }
    
    @Override
    public List<GameClass> getAvailableClasses(UUID playerId) {
        PlayerClassData data = getPlayerData(playerId);
        if (data == null) return classManager.getBaseClasses();
        return classManager.getAvailableClasses(data);
    }
    
    @Override
    public boolean chooseClass(UUID playerId, String classId) {
        GameClass targetClass = classManager.getClass(classId);
        if (targetClass == null) return false;
        
        if (!targetClass.isBaseClass()) return false;
        
        PlayerClassData data = dataHandler.getOrCreatePlayerData(playerId);
        if (data.getClassId() != null && !data.getClassId().equals(plugin.getDefaultClassId())) {
            return false;
        }
        
        data.setClassId(classId);
        data.setTier(1);
        data.setExp(0);
        data.setAdvancementLevel(0);
        data.setLastUpdateTime(System.currentTimeMillis());
        
        // 初始化该途径的技能空间
        String pathway = targetClass.getPathway();
        if (pathway != null && !pathway.isEmpty()) {
            org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                plugin.getSkillSpaceManager().initSkillsForPathway(player, pathway);
                // 解锁序列9的第一个技能
                plugin.getSkillSpaceManager().unlockSkill(player, targetClass.getSkills().isEmpty() ? "" : targetClass.getSkills().get(0));
            }
        }
        
        return true;
    }
    
    @Override
    public boolean advanceClass(UUID playerId, String targetClassId) {
        PlayerClassData data = getPlayerData(playerId);
        if (data == null) return false;
        
        GameClass targetClass = classManager.getClass(targetClassId);
        if (targetClass == null) return false;
        
        if (!classManager.canAdvanceTo(data, targetClass)) return false;
        
        data.setClassId(targetClassId);
        data.setAdvancementLevel(targetClass.getAdvancement());
        data.setLastUpdateTime(System.currentTimeMillis());
        
        int attributePoints = targetClass.getAttributePoints();
        if (attributePoints > 0) {
            data.addAttributePoints(attributePoints);
        }
        
        // 解锁新职业的技能
        List<String> skills = targetClass.getSkills();
        if (!skills.isEmpty()) {
            org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                for (String skillId : skills) {
                    plugin.getSkillSpaceManager().unlockSkill(player, skillId);
                }
            }
        }
        
        return true;
    }
    
    @Override
    public boolean addExp(UUID playerId, long amount) {
        return expManager.addExp(playerId, amount);
    }
    
    @Override
    public boolean setExp(UUID playerId, long amount) {
        return expManager.setExp(playerId, amount);
    }
    
    @Override
    public boolean setClass(UUID playerId, String classId) {
        GameClass targetClass = classManager.getClass(classId);
        if (targetClass == null) return false;
        
        PlayerClassData data = dataHandler.getOrCreatePlayerData(playerId);
        data.setClassId(classId);
        data.setTier(targetClass.getTier());
        data.setAdvancementLevel(targetClass.getAdvancement());
        data.setLastUpdateTime(System.currentTimeMillis());
        
        // 初始化该途径的技能空间并解锁所有该序列及之前的技能
        String pathway = targetClass.getPathway();
        if (pathway != null && !pathway.isEmpty()) {
            org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                // 初始化技能空间
                plugin.getSkillSpaceManager().initSkillsForPathway(player, pathway);
                
                // 解锁该职业的技能
                List<String> skills = targetClass.getSkills();
                if (!skills.isEmpty()) {
                    for (String skillId : skills) {
                        plugin.getSkillSpaceManager().unlockSkill(player, skillId);
                    }
                }
                
                // 解锁该途径中序列大于当前职业序列的所有技能（序列数字越大，等级越低）
                int currentSequence = targetClass.getSequence();
                for (cn.guangdian.classsystem.model.GameClass gc : classManager.getClassesByPathway(pathway)) {
                    // 序列数字越大 = 等级越低，所以 sequence > currentSequence 表示更低等级的职业
                    if (gc.getSequence() > currentSequence) {
                        List<String> lowerSkills = gc.getSkills();
                        if (!lowerSkills.isEmpty()) {
                            for (String skillId : lowerSkills) {
                                plugin.getSkillSpaceManager().unlockSkill(player, skillId);
                            }
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    @Override
    public boolean resetClass(UUID playerId) {
        PlayerClassData data = getPlayerData(playerId);
        if (data == null) return false;
        
        data.setClassId(plugin.getDefaultClassId());
        data.setTier(1);
        data.setExp(0);
        data.setAdvancementLevel(0);
        data.setTotalExp(0);
        data.setLastUpdateTime(System.currentTimeMillis());
        
        return true;
    }
}
