package cn.guangdian.armorstats.parser;

import cn.guangdian.rpgcore.util.TextStripper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillParser {

    private static Pattern SKILL_NAME_PATTERN = Pattern.compile("技能名称:\\s*([^\\s]+)");
    private static Pattern SKILL_TYPE_PATTERN = Pattern.compile("触发类型:\\s*([^\\s]+)");

    // 新增技能模式 - 支持主动技能和被动技能
    private static Pattern ACTIVE_SKILL_PATTERN = Pattern.compile("主动技能[:\\s]+([^\\s&]+)");
    private static Pattern PASSIVE_SKILL_PATTERN = Pattern.compile("被动技能[:\\s]+([^\\s&]+)");
    private static Pattern SKILL_PATTERN = Pattern.compile("技能[:\\s]+([^\\s&]+)");

    public static List<String> parseSkillNames(ItemStack item) {
        List<String> skillNames = new ArrayList<>();

        if (item == null || !item.hasItemMeta()) {
            return skillNames;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return skillNames;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            return skillNames;
        }

        for (String line : lore) {
            // 使用 TextStripper 支持 MiniMessage 标签和传统颜色代码
            String plainLine = TextStripper.stripAll(line);

            // 尝试匹配旧格式
            Matcher nameMatcher = SKILL_NAME_PATTERN.matcher(plainLine);
            if (nameMatcher.find()) {
                String skillName = nameMatcher.group(1);
                if (skillName != null && !skillName.isEmpty()) {
                    skillNames.add(skillName);
                }
                continue;
            }

            // 尝试匹配主动技能
            Matcher activeMatcher = ACTIVE_SKILL_PATTERN.matcher(plainLine);
            if (activeMatcher.find()) {
                String skillName = activeMatcher.group(1).trim();
                if (skillName != null && !skillName.isEmpty()) {
                    skillNames.add(skillName);
                }
                continue;
            }

            // 尝试匹配被动技能
            Matcher passiveMatcher = PASSIVE_SKILL_PATTERN.matcher(plainLine);
            if (passiveMatcher.find()) {
                String skillName = passiveMatcher.group(1).trim();
                if (skillName != null && !skillName.isEmpty()) {
                    skillNames.add(skillName);
                }
                continue;
            }

            // 尝试匹配通用技能
            Matcher skillMatcher = SKILL_PATTERN.matcher(plainLine);
            if (skillMatcher.find()) {
                String skillName = skillMatcher.group(1).trim();
                if (skillName != null && !skillName.isEmpty()) {
                    skillNames.add(skillName);
                }
            }
        }

        return skillNames;
    }

    public static String parseTriggerType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            return null;
        }

        for (String line : lore) {
            // 使用 TextStripper 支持 MiniMessage 标签和传统颜色代码
            String plainLine = TextStripper.stripAll(line);

            Matcher typeMatcher = SKILL_TYPE_PATTERN.matcher(plainLine);
            if (typeMatcher.find()) {
                return typeMatcher.group(1);
            }
        }

        return null;
    }

    public static boolean hasSkill(ItemStack item) {
        return !parseSkillNames(item).isEmpty();
    }
}
