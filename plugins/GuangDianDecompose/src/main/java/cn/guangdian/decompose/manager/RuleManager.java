package cn.guangdian.decompose.manager;

import cn.guangdian.decompose.GuangDianDecompose;
import cn.guangdian.decompose.model.DecomposeRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleManager {

    private final GuangDianDecompose plugin;
    private final Map<String, DecomposeRule> rules;
    private DecomposeRule defaultRule;
    private File rulesFile;
    private FileConfiguration rulesConfig;

    public RuleManager(GuangDianDecompose plugin) {
        this.plugin = plugin;
        this.rules = new HashMap<>();
        loadRules();
    }

    public void loadRules() {
        rules.clear();

        rulesFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("rules-file", "rules.yml"));
        if (!rulesFile.exists()) {
            plugin.saveResource("rules.yml", false);
        }

        rulesConfig = YamlConfiguration.loadConfiguration(rulesFile);
        ConfigurationSection rulesSection = rulesConfig.getConfigurationSection("");

        if (rulesSection == null) {
            plugin.getLogger().warning("rules.yml 中没有找到任何分解规则!");
            return;
        }

        for (String itemId : rulesSection.getKeys(false)) {
            if ("default".equals(itemId)) {
                loadDefaultRule(rulesSection.getConfigurationSection(itemId));
                continue;
            }

            ConfigurationSection itemSection = rulesSection.getConfigurationSection(itemId);
            if (itemSection == null) continue;

            DecomposeRule rule = new DecomposeRule(itemId);

            List<String> materialList = itemSection.getStringList("materials");
            for (String materialStr : materialList) {
                DecomposeRule.MaterialReward reward = parseMaterialReward(materialStr);
                if (reward != null) {
                    rule.addMaterial(reward);
                }
            }

            rule.setChance(itemSection.getDouble("chance", 1.0));
            rule.setMessage(itemSection.getString("message", ""));

            rules.put(itemId, rule);
            plugin.debug("加载分解规则: " + itemId + " -> " + rule.getMaterials().size() + " 种材料");
        }

        plugin.getLogger().info("已加载 " + rules.size() + " 条分解规则!");
    }

    private void loadDefaultRule(ConfigurationSection section) {
        if (section == null) return;

        boolean enabled = section.getBoolean("enabled", false);
        if (!enabled) {
            this.defaultRule = null;
            return;
        }

        this.defaultRule = new DecomposeRule("default");

        List<String> materialList = section.getStringList("materials");
        for (String materialStr : materialList) {
            DecomposeRule.MaterialReward reward = parseMaterialReward(materialStr);
            if (reward != null) {
                defaultRule.addMaterial(reward);
            }
        }

        defaultRule.setChance(section.getDouble("chance", 0.5));
        defaultRule.setMessage(section.getString("message", ""));
    }

    private DecomposeRule.MaterialReward parseMaterialReward(String str) {
        if (str == null || str.isEmpty()) return null;

        String[] parts = str.split(":");
        if (parts.length < 3) {
            plugin.getLogger().warning("无效的材料格式: " + str + " (应为 type:id:amount)");
            return null;
        }

        String type = parts[0].trim();
        String itemId = parts[1].trim();
        int amount;

        try {
            amount = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("无效的数量: " + parts[2]);
            return null;
        }

        return new DecomposeRule.MaterialReward(type, itemId, amount);
    }

    public DecomposeRule getRule(String itemId) {
        DecomposeRule rule = rules.get(itemId);
        if (rule == null) {
            return defaultRule;
        }
        return rule;
    }

    public boolean hasRule(String itemId) {
        return rules.containsKey(itemId) || defaultRule != null;
    }

    public void addRule(String itemId, DecomposeRule rule) {
        rules.put(itemId, rule);
    }

    public void saveRule(String itemId, DecomposeRule rule) {
        rulesConfig.set(itemId + ".materials", rule.getMaterials().stream()
                .map(DecomposeRule.MaterialReward::toString)
                .toList());
        rulesConfig.set(itemId + ".chance", rule.getChance());
        if (rule.hasCustomMessage()) {
            rulesConfig.set(itemId + ".message", rule.getMessage());
        }

        try {
            rulesConfig.save(rulesFile);
            rules.put(itemId, rule);
        } catch (Exception e) {
            plugin.getLogger().severe("保存分解规则失败: " + e.getMessage());
        }
    }

    public Map<String, DecomposeRule> getAllRules() {
        return new HashMap<>(rules);
    }
}
