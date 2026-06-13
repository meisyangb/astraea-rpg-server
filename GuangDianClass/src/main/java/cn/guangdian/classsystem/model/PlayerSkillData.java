package cn.guangdian.classsystem.model;

import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家技能数据
 * 
 * 存储玩家的技能空间和快捷栏绑定信息
 */
public class PlayerSkillData {

    private final UUID playerId;
    private final Map<String, SkillOrb> skillSpace;      // 技能空间(所有技能)
    private final Map<Integer, String> hotbarBindings;   // 快捷栏绑定 (槽位 -> 技能ID)
    private final Set<String> passiveSkills;             // 已激活的被动技能

    public PlayerSkillData(UUID playerId) {
        this.playerId = playerId;
        this.skillSpace = new ConcurrentHashMap<>();
        this.hotbarBindings = new ConcurrentHashMap<>();
        this.passiveSkills = ConcurrentHashMap.newKeySet();
    }

    // Getters
    public UUID getPlayerId() { return playerId; }
    public Map<String, SkillOrb> getSkillSpace() { return skillSpace; }
    public Map<Integer, String> getHotbarBindings() { return hotbarBindings; }
    public Set<String> getPassiveSkills() { return passiveSkills; }

    /**
     * 添加技能到技能空间
     */
    public void addSkill(SkillOrb skill) {
        skillSpace.put(skill.getSkillId(), skill);
    }

    /**
     * 获取技能
     */
    public SkillOrb getSkill(String skillId) {
        return skillSpace.get(skillId);
    }

    /**
     * 绑定技能到快捷栏
     */
    public void bindSkillToHotbar(int slot, String skillId) {
        if (slot >= 0 && slot < 9) {
            hotbarBindings.put(slot, skillId);
        }
    }

    /**
     * 解绑快捷栏技能
     */
    public void unbindSkillFromHotbar(int slot) {
        hotbarBindings.remove(slot);
    }

    /**
     * 获取快捷栏绑定的技能
     */
    public String getHotbarBinding(int slot) {
        return hotbarBindings.get(slot);
    }

    /**
     * 激活被动技能
     */
    public void activatePassiveSkill(String skillId) {
        passiveSkills.add(skillId);
    }

    /**
     * 停用被动技能
     */
    public void deactivatePassiveSkill(String skillId) {
        passiveSkills.remove(skillId);
    }

    /**
     * 检查被动技能是否激活
     */
    public boolean isPassiveSkillActive(String skillId) {
        return passiveSkills.contains(skillId);
    }

    /**
     * 解锁技能
     */
    public void unlockSkill(String skillId) {
        SkillOrb skill = skillSpace.get(skillId);
        if (skill != null) {
            skill.setUnlocked(true);
            // 如果是被动技能,自动激活
            if (skill.isPassive()) {
                activatePassiveSkill(skillId);
            }
        }
    }

    /**
     * 获取已解锁的主动技能数量
     */
    public int getUnlockedActiveSkillCount() {
        return (int) skillSpace.values().stream()
            .filter(SkillOrb::isUnlocked)
            .filter(SkillOrb::isActive)
            .count();
    }

    /**
     * 获取已解锁的被动技能数量
     */
    public int getUnlockedPassiveSkillCount() {
        return (int) skillSpace.values().stream()
            .filter(SkillOrb::isUnlocked)
            .filter(SkillOrb::isPassive)
            .count();
    }

    /**
     * 获取总技能数量
     */
    public int getTotalSkillCount() {
        return skillSpace.size();
    }
}
