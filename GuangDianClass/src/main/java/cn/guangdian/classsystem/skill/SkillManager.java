package cn.guangdian.classsystem.skill;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;

public class SkillManager {
    
    private final GuangDianClass plugin;
    private final Map<String, SkillDefinition> skillDefinitions;
    
    public SkillManager(GuangDianClass plugin) {
        this.plugin = plugin;
        this.skillDefinitions = new HashMap<>();
        loadSkills();
    }
    
    private void loadSkills() {
        registerDefaultSkills();
        plugin.getLogger().info("已加载 " + skillDefinitions.size() + " 个技能定义");
    }
    
    private void registerDefaultSkills() {
        registerSkill(new SkillDefinition("slash", "斩击", "战士基础技能，造成物理伤害", 1, 5));
        registerSkill(new SkillDefinition("shield_bash", "盾击", "用盾牌猛击敌人", 1, 10));
        registerSkill(new SkillDefinition("charge", "冲锋", "骑乘冲锋，造成大量伤害", 3, 15));
        registerSkill(new SkillDefinition("mounted_combat", "骑乘战斗", "提升骑乘时的战斗力", 3, 0));
        
        registerSkill(new SkillDefinition("fireball", "火球术", "发射一个火球", 1, 5));
        registerSkill(new SkillDefinition("ice_shield", "冰盾", "召唤冰盾保护自己", 1, 10));
        registerSkill(new SkillDefinition("elemental_fury", "元素狂怒", "释放元素之力", 3, 20));
        registerSkill(new SkillDefinition("meteor", "陨石术", "召唤陨石从天而降", 6, 50));
        registerSkill(new SkillDefinition("time_stop", "时间停止", "短暂停止时间", 6, 100));
        
        registerSkill(new SkillDefinition("aimed_shot", "瞄准射击", "精准瞄准射击", 1, 5));
        registerSkill(new SkillDefinition("arrow_rain", "箭雨", "射出大量箭矢", 1, 15));
        registerSkill(new SkillDefinition("multi_shot", "多重射击", "同时射出多支箭", 3, 10));
        registerSkill(new SkillDefinition("trap", "陷阱", "设置陷阱", 3, 5));
        
        registerSkill(new SkillDefinition("backstab", "背刺", "从背后攻击造成额外伤害", 1, 15));
        registerSkill(new SkillDefinition("stealth", "潜行", "进入隐身状态", 1, 20));
        registerSkill(new SkillDefinition("shadow_step", "暗影步", "瞬移到目标身后", 3, 15));
        registerSkill(new SkillDefinition("assassinate", "暗杀", "一击致命", 6, 50));
        
        registerSkill(new SkillDefinition("heal", "治疗", "恢复生命值", 1, 10));
        registerSkill(new SkillDefinition("blessing", "祝福", "获得神明祝福", 1, 15));
        registerSkill(new SkillDefinition("group_heal", "群体治疗", "治疗周围队友", 3, 30));
        registerSkill(new SkillDefinition("resurrection", "复活", "复活死亡的队友", 6, 100));
        
        registerSkill(new SkillDefinition("divine_dragon_form", "神圣龙形态", "化身为神圣巨龙", 9, 200));
        registerSkill(new SkillDefinition("cosmic_annihilation", "宇宙湮灭", "释放宇宙之力", 9, 300));
        registerSkill(new SkillDefinition("godslayer_arrow", "弑神之箭", "能弑杀神明的箭矢", 9, 250));
        registerSkill(new SkillDefinition("divine_death", "神圣死亡", "裁决生死", 9, 200));
        registerSkill(new SkillDefinition("divine_miracle", "神圣奇迹", "创造奇迹", 9, 150));
    }
    
    private void registerSkill(SkillDefinition skill) {
        skillDefinitions.put(skill.getId(), skill);
    }
    
    public List<String> getUnlockedSkills(Player player) {
        PlayerClassData data = plugin.getPlayerData(player);
        if (data == null) return new ArrayList<>();
        
        GameClass gameClass = plugin.getClassManager().getClass(data.getClassId());
        if (gameClass == null) return new ArrayList<>();
        
        return new ArrayList<>(gameClass.getSkills());
    }
    
    public boolean hasSkill(Player player, String skillId) {
        List<String> skills = getUnlockedSkills(player);
        return skills.contains(skillId);
    }
    
    public SkillDefinition getSkillDefinition(String skillId) {
        return skillDefinitions.get(skillId);
    }
    
    public void sendSkillList(Player player) {
        List<String> skills = getUnlockedSkills(player);
        
        if (skills.isEmpty()) {
            player.sendMessage(Component.text("你还没有解锁任何技能！").color(NamedTextColor.RED));
            return;
        }
        
        player.sendMessage(Component.text("========== 已解锁技能 ==========").color(NamedTextColor.GOLD));
        
        for (String skillId : skills) {
            SkillDefinition skill = skillDefinitions.get(skillId);
            if (skill != null) {
                player.sendMessage(Component.text(skill.getName())
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(" - " + skill.getDescription())
                        .color(NamedTextColor.GRAY)));
            } else {
                player.sendMessage(Component.text(skillId)
                    .color(NamedTextColor.YELLOW));
            }
        }
        
        player.sendMessage(Component.text("================================").color(NamedTextColor.GOLD));
    }
    
    public Collection<SkillDefinition> getAllSkills() {
        return skillDefinitions.values();
    }
    
    public static class SkillDefinition {
        private final String id;
        private final String name;
        private final String description;
        private final int requiredTier;
        private final int cooldown;
        
        public SkillDefinition(String id, String name, String description, int requiredTier, int cooldown) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.requiredTier = requiredTier;
            this.cooldown = cooldown;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getRequiredTier() { return requiredTier; }
        public int getCooldown() { return cooldown; }
    }
}
