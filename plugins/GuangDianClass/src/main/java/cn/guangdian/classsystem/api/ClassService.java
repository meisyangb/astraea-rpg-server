package cn.guangdian.classsystem.api;

import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ClassService {
    
    PlayerClassData getPlayerData(UUID playerId);
    
    PlayerClassData getPlayerData(Player player);
    
    GameClass getClass(String classId);
    
    Collection<GameClass> getAllClasses();
    
    List<GameClass> getAvailableClasses(UUID playerId);
    
    List<GameClass> getAvailableClasses(Player player);
    
    boolean chooseClass(UUID playerId, String classId);
    
    boolean chooseClass(Player player, String classId);
    
    boolean advanceClass(UUID playerId, String targetClassId);
    
    boolean advanceClass(Player player, String targetClassId);
    
    boolean addExp(UUID playerId, long amount);
    
    boolean addExp(Player player, long amount);
    
    boolean setExp(UUID playerId, long amount);
    
    boolean setExp(Player player, long amount);
    
    boolean setClass(UUID playerId, String classId);
    
    boolean setClass(Player player, String classId);
    
    boolean resetClass(UUID playerId);
    
    boolean resetClass(Player player);
    
    String getPlayerClassName(UUID playerId);
    
    String getPlayerClassName(Player player);
    
    int getPlayerTier(UUID playerId);
    
    int getPlayerTier(Player player);
    
    long getPlayerExp(UUID playerId);
    
    long getPlayerExp(Player player);
    
    int getPlayerAdvancementLevel(UUID playerId);
    
    int getPlayerAdvancementLevel(Player player);
    
    int getAvailableAttributePoints(UUID playerId);
    
    int getAvailableAttributePoints(Player player);
    
    int getAllocatedAttributePoints(UUID playerId, AttributeType type);
    
    int getAllocatedAttributePoints(Player player, AttributeType type);
    
    boolean allocateAttribute(UUID playerId, AttributeType type, int points);
    
    boolean allocateAttribute(Player player, AttributeType type, int points);
    
    boolean deallocateAttribute(UUID playerId, AttributeType type, int points);
    
    boolean deallocateAttribute(Player player, AttributeType type, int points);
    
    void resetAttributes(UUID playerId);
    
    void resetAttributes(Player player);
    
    Map<String, Double> getPlayerAttributeBonuses(UUID playerId);
    
    Map<String, Double> getPlayerAttributeBonuses(Player player);
    
    List<AttributeType> getAvailableAttributesForClass(String classId);
    
    List<AttributeType> getAvailableAttributesForPlayer(UUID playerId);
}
