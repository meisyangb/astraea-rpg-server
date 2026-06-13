package cn.guangdian.gearscore.manager;

import cn.guangdian.gearscore.GuangDianGearScore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreCalculator {

    private final GuangDianGearScore plugin;
    private Pattern scorePattern;

    public ScoreCalculator(GuangDianGearScore plugin) {
        this.plugin = plugin;
        updatePatterns(plugin.getScorePatterns());
    }

    public void updatePatterns(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            patterns = List.of("评分", "战力", "装备评分");
        }
        
        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append("(?:");
        for (int i = 0; i < patterns.size(); i++) {
            if (i > 0) patternBuilder.append("|");
            patternBuilder.append(Pattern.quote(patterns.get(i)));
        }
        patternBuilder.append(")[：:][\\s]*([\\d,]+)");
        
        this.scorePattern = Pattern.compile(patternBuilder.toString());
        plugin.getLogger().info("评分匹配模式: " + patternBuilder);
    }

    public long calculateTotalScore(Player player) {
        long totalScore = 0;
        
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack item : armor) {
            totalScore += parseItemScore(item);
        }
        
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        totalScore += parseItemScore(mainHand);
        
        ItemStack offHand = player.getInventory().getItemInOffHand();
        totalScore += parseItemScore(offHand);
        
        return totalScore;
    }

    public long parseItemScore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 0;
        }

        if (!item.hasItemMeta()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return 0;
        }

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return 0;
        }

        for (String line : lore) {
            String stripped = stripColor(line);
            
            Matcher matcher = scorePattern.matcher(stripped);
            if (matcher.find()) {
                String numberStr = matcher.group(1).replace(",", "");
                try {
                    return Long.parseLong(numberStr);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        return 0;
    }

    private String stripColor(String input) {
        if (input == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '§' || c == '&') {
                i++;
            } else {
                sb.append(c);
            }
        }
        
        // 移除 MiniMessage 标签 (如 <dark_gray>, <red>, <bold> 等)
        String result = sb.toString();
        result = result.replaceAll("<[^>]+>", "");
        
        return result;
    }
}
