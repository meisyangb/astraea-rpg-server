package cn.guangdian.classsystem.api;

import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 职业服务扩展接口
 *
 * <p>继承 RPGCore 的 ClassService 接口，添加职业系统特有的方法。</p>
 *
 * @see cn.guangdian.rpgcore.service.api.ClassService
 */
public interface ClassService extends cn.guangdian.rpgcore.service.api.ClassService {
    
    // ==================== 扩展方法：Player 参数版本 ====================
    
    /**
     * 获取玩家职业名称
     */
    default String getPlayerClassName(Player player) {
        return getPlayerClassName(player.getUniqueId());
    }
    
    /**
     * 获取玩家职业ID
     */
    default String getPlayerClassId(Player player) {
        return getPlayerClassId(player.getUniqueId());
    }
    
    /**
     * 获取玩家阶位
     */
    default int getPlayerTier(Player player) {
        return getPlayerTier(player.getUniqueId());
    }
    
    /**
     * 获取玩家转职阶数
     */
    default int getPlayerAdvancementLevel(Player player) {
        return getPlayerAdvancementLevel(player.getUniqueId());
    }
    
    /**
     * 获取玩家职业属性加成
     */
    default Map<String, Double> getPlayerClassStats(Player player) {
        return getPlayerClassStats(player.getUniqueId());
    }
    
    /**
     * 获取玩家指定属性值
     */
    default double getPlayerClassStat(Player player, String statName) {
        return getPlayerClassStat(player.getUniqueId(), statName);
    }
    
    /**
     * 获取玩家经验值
     */
    default long getPlayerExp(Player player) {
        return getPlayerExp(player.getUniqueId());
    }
    
    // ==================== 职业系统特有方法 ====================
    
    PlayerClassData getPlayerData(UUID playerId);
    
    default PlayerClassData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    GameClass getClass(String classId);
    
    Collection<GameClass> getAllClasses();
    
    List<GameClass> getAvailableClasses(UUID playerId);
    
    default List<GameClass> getAvailableClasses(Player player) {
        return getAvailableClasses(player.getUniqueId());
    }
    
    boolean chooseClass(UUID playerId, String classId);
    
    default boolean chooseClass(Player player, String classId) {
        return chooseClass(player.getUniqueId(), classId);
    }
    
    boolean advanceClass(UUID playerId, String targetClassId);
    
    default boolean advanceClass(Player player, String targetClassId) {
        return advanceClass(player.getUniqueId(), targetClassId);
    }
    
    boolean addExp(UUID playerId, long amount);
    
    default boolean addExp(Player player, long amount) {
        return addExp(player.getUniqueId(), amount);
    }
    
    boolean setExp(UUID playerId, long amount);
    
    default boolean setExp(Player player, long amount) {
        return setExp(player.getUniqueId(), amount);
    }
    
    boolean setClass(UUID playerId, String classId);
    
    default boolean setClass(Player player, String classId) {
        return setClass(player.getUniqueId(), classId);
    }
    
    boolean resetClass(UUID playerId);
    
    default boolean resetClass(Player player) {
        return resetClass(player.getUniqueId());
    }
}
