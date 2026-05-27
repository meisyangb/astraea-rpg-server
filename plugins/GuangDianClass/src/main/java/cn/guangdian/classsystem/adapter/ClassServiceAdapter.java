package cn.guangdian.classsystem.adapter;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.data.ClassDataHandler;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.manager.ExpManager;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ClassServiceAdapter implements ClassService {
    
    private final GuangDianClass plugin;
    private final ClassManager classManager;
    private final ExpManager expManager;
    private final ClassDataHandler dataHandler;
    private boolean usingRPGCore;
    
    public ClassServiceAdapter(GuangDianClass plugin, ClassManager classManager, 
                                ExpManager expManager, ClassDataHandler dataHandler) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.expManager = expManager;
        this.dataHandler = dataHandler;
        
        registerWithRPGCore();
    }
    
    private void registerWithRPGCore() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            if (registry != null) {
                registry.registerService(ClassService.class, this);
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
                registry.unregisterService(ClassService.class);
            }
        }
    }
    
    public boolean isUsingRPGCore() {
        return usingRPGCore;
    }
    
    @Override
    public PlayerClassData getPlayerData(UUID playerId) {
        return dataHandler.getPlayerData(playerId);
    }
    
    @Override
    public PlayerClassData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
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
    public List<GameClass> getAvailableClasses(Player player) {
        return getAvailableClasses(player.getUniqueId());
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
        
        return true;
    }
    
    @Override
    public boolean chooseClass(Player player, String classId) {
        return chooseClass(player.getUniqueId(), classId);
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
        
        return true;
    }
    
    @Override
    public boolean advanceClass(Player player, String targetClassId) {
        return advanceClass(player.getUniqueId(), targetClassId);
    }
    
    @Override
    public boolean addExp(UUID playerId, long amount) {
        return expManager.addExp(playerId, amount);
    }
    
    @Override
    public boolean addExp(Player player, long amount) {
        return expManager.addExp(player, amount);
    }
    
    @Override
    public boolean setExp(UUID playerId, long amount) {
        return expManager.setExp(playerId, amount);
    }
    
    @Override
    public boolean setExp(Player player, long amount) {
        return expManager.setExp(player, amount);
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
        
        return true;
    }
    
    @Override
    public boolean setClass(Player player, String classId) {
        return setClass(player.getUniqueId(), classId);
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
    
    @Override
    public boolean resetClass(Player player) {
        return resetClass(player.getUniqueId());
    }
    
    @Override
    public String getPlayerClassName(UUID playerId) {
        PlayerClassData data = getPlayerData(playerId);
        if (data == null) return "未知";
        
        GameClass gameClass = classManager.getClass(data.getClassId());
        return gameClass != null ? gameClass.getName() : "未知";
    }
    
    @Override
    public String getPlayerClassName(Player player) {
        return getPlayerClassName(player.getUniqueId());
    }
    
    @Override
    public int getPlayerTier(UUID playerId) {
        PlayerClassData data = getPlayerData(playerId);
        return data != null ? data.getTier() : 1;
    }
    
    @Override
    public int getPlayerTier(Player player) {
        return getPlayerTier(player.getUniqueId());
    }
    
    @Override
    public long getPlayerExp(UUID playerId) {
        PlayerClassData data = getPlayerData(playerId);
        return data != null ? data.getExp() : 0;
    }
    
    @Override
    public long getPlayerExp(Player player) {
        return getPlayerExp(player.getUniqueId());
    }
    
    @Override
    public int getPlayerAdvancementLevel(UUID playerId) {
        PlayerClassData data = getPlayerData(playerId);
        return data != null ? data.getAdvancementLevel() : 0;
    }
    
    @Override
    public int getPlayerAdvancementLevel(Player player) {
        return getPlayerAdvancementLevel(player.getUniqueId());
    }
}
