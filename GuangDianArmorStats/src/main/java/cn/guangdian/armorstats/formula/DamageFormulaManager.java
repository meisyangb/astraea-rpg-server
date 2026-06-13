package cn.guangdian.armorstats.formula;

import cn.guangdian.armorstats.GuangDianArmorStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DamageFormulaManager {

    private final GuangDianArmorStats plugin;
    private FileConfiguration formulaConfig;
    private File formulaConfigFile;
    
    private final Map<String, String> formulas;
    private final Map<String, Double> globalSettings;
    private final Map<String, Double> pvpSettings;
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([a-z_]+)\\}");
    private static final Pattern RANDOM_PATTERN = Pattern.compile("random\\(([0-9.]+),\\s*([0-9.]+)\\)");
    
    private boolean enabled;
    private int precision;
    private boolean debug;
    private double roundFactor;

    public DamageFormulaManager(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        this.formulas = new ConcurrentHashMap<>();
        this.globalSettings = new ConcurrentHashMap<>();
        this.pvpSettings = new ConcurrentHashMap<>();
        
        loadConfig();
    }

    private void loadConfig() {
        formulaConfigFile = new File(plugin.getDataFolder(), "damage_formula.yml");
        if (!formulaConfigFile.exists()) {
            plugin.saveResource("damage_formula.yml", false);
        }
        formulaConfig = YamlConfiguration.loadConfiguration(formulaConfigFile);
        loadFormulas();
        loadSettings();
    }

    private void loadFormulas() {
        formulas.clear();
        
        loadSectionFormulas("formulas.physical", "physical.");
        loadSectionFormulas("formulas.magical", "magical.");
        loadSectionFormulas("formulas.true_damage", "true_damage.");
        loadSectionFormulas("formulas.percent_damage", "percent_damage.");
        loadSectionFormulas("formulas.elemental", "elemental.");
        loadSectionFormulas("boss.attack", "boss.attack.");
        loadSectionFormulas("boss.defense", "boss.defense.");
        loadSectionFormulas("boss.penetration", "boss.penetration.");
        
        plugin.getLogger().info("Loaded " + formulas.size() + " damage formulas");
    }

    private void loadSectionFormulas(String path, String prefix) {
        ConfigurationSection section = formulaConfig.getConfigurationSection(path);
        if (section == null) return;
        
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value != null) {
                formulas.put(prefix + key, value);
            }
        }
    }

    private void loadSettings() {
        globalSettings.clear();
        pvpSettings.clear();
        
        ConfigurationSection globalSection = formulaConfig.getConfigurationSection("global");
        if (globalSection != null) {
            enabled = globalSection.getBoolean("enabled", true);
            precision = globalSection.getInt("precision", 2);
            debug = globalSection.getBoolean("debug", false);
            roundFactor = Math.pow(10, precision);
            
            globalSettings.put("min_damage", globalSection.getDouble("min_damage", 1.0));
        }
        
        ConfigurationSection pvpSection = formulaConfig.getConfigurationSection("pvp");
        if (pvpSection != null) {
            pvpSettings.put("damage_multiplier", pvpSection.getDouble("damage_multiplier", 0.8));
            pvpSettings.put("max_crit_damage", pvpSection.getDouble("max_crit_damage", 200.0));
            pvpSettings.put("max_penetration", pvpSection.getDouble("max_penetration", 50.0));
        }
    }

    public double evaluate(String formulaKey, Map<String, Double> variables) {
        String formula = formulas.get(formulaKey);
        if (formula == null) {
            plugin.getLogger().warning("Formula not found: " + formulaKey);
            return 0;
        }
        
        return evaluateFormula(formula, variables);
    }

    public double evaluateFormula(String formula, Map<String, Double> variables) {
        String processed = formula;
        
        Matcher randomMatcher = RANDOM_PATTERN.matcher(processed);
        StringBuffer sb = new StringBuffer();
        while (randomMatcher.find()) {
            double min = Double.parseDouble(randomMatcher.group(1));
            double max = Double.parseDouble(randomMatcher.group(2));
            double randomValue = min + Math.random() * (max - min);
            randomMatcher.appendReplacement(sb, String.valueOf(randomValue));
        }
        randomMatcher.appendTail(sb);
        processed = sb.toString();
        
        Matcher matcher = VARIABLE_PATTERN.matcher(processed);
        sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Double value = variables.get(varName);
            if (value == null) {
                value = 0.0;
            }
            matcher.appendReplacement(sb, String.valueOf(value));
        }
        matcher.appendTail(sb);
        processed = sb.toString();
        
        try {
            return evaluateExpression(processed);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to evaluate formula: " + formula + " -> " + processed);
            return 0;
        }
    }

    private double evaluateExpression(String expression) {
        expression = expression.replaceAll("\\s+", "");
        
        if (expression.contains("+") || expression.contains("-")) {
            int lastPlus = expression.lastIndexOf("+");
            int lastMinus = expression.lastIndexOf("-");
            
            if (lastPlus > lastMinus && lastPlus != -1) {
                double left = evaluateExpression(expression.substring(0, lastPlus));
                double right = evaluateExpression(expression.substring(lastPlus + 1));
                return round(left + right);
            } else if (lastMinus != -1 && lastMinus != 0) {
                if (lastMinus > 0 && expression.charAt(lastMinus - 1) == '*') {
                    // skip
                } else if (lastMinus > 0 && expression.charAt(lastMinus - 1) == '/') {
                    // skip
                } else {
                    double left = evaluateExpression(expression.substring(0, lastMinus));
                    double right = evaluateExpression(expression.substring(lastMinus + 1));
                    return round(left - right);
                }
            }
        }
        
        if (expression.contains("*") || expression.contains("/")) {
            int lastMult = expression.lastIndexOf("*");
            int lastDiv = expression.lastIndexOf("/");
            
            if (lastMult > lastDiv && lastMult != -1) {
                double left = evaluateExpression(expression.substring(0, lastMult));
                double right = evaluateExpression(expression.substring(lastMult + 1));
                return round(left * right);
            } else if (lastDiv != -1) {
                double left = evaluateExpression(expression.substring(0, lastDiv));
                double right = evaluateExpression(expression.substring(lastDiv + 1));
                if (right == 0) return 0;
                return round(left / right);
            }
        }
        
        if (expression.startsWith("(") && expression.endsWith(")")) {
            return evaluateExpression(expression.substring(1, expression.length() - 1));
        }
        
        try {
            return Double.parseDouble(expression);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double round(double value) {
        if (precision <= 0) return value;
        return Math.round(value * roundFactor) / roundFactor;
    }

    public double calculatePhysicalDamage(Map<String, Double> variables) {
        double baseDamage = evaluate("physical.base", variables);
        variables.put("base_damage", baseDamage);
        
        double defenseReduction = evaluate("physical.defense_reduction", variables);
        double armorReduction = evaluate("physical.armor_reduction", variables);
        
        variables.put("defense_reduction", defenseReduction);
        variables.put("armor_reduction", armorReduction);
        
        double finalDamage = evaluate("physical.final", variables);
        
        double minDamage = globalSettings.getOrDefault("min_damage", 1.0);
        return Math.max(minDamage, finalDamage);
    }

    public double calculateBossAttackDamage(Map<String, Double> variables) {
        double baseDamage = evaluate("boss.attack.base", variables);
        variables.put("base_damage", baseDamage);
        
        double multiplied = evaluate("boss.attack.multiplier", variables);
        
        double critChance = variables.getOrDefault("crit_chance", 0.0);
        if (Math.random() * 100 < critChance) {
            multiplied = evaluate("boss.attack.critical", variables);
        }
        
        return multiplied;
    }

    public double calculateBossDefenseDamage(double incomingDamage, Map<String, Double> variables) {
        variables.put("incoming_damage", incomingDamage);
        
        double armorReduction = evaluate("boss.defense.armor_reduction", variables);
        double defenseReduction = evaluate("boss.defense.defense_reduction", variables);
        double damageReduction = evaluate("boss.defense.damage_reduction", variables);
        
        variables.put("armor_reduction", armorReduction);
        variables.put("defense_reduction", defenseReduction);
        variables.put("damage_reduction", damageReduction);
        
        double finalDamage = evaluate("boss.defense.final", variables);
        
        return Math.max(1.0, finalDamage);
    }

    public double applyPvpBalance(double damage, boolean isCritical) {
        double multiplier = pvpSettings.getOrDefault("damage_multiplier", 0.8);
        damage *= multiplier;
        
        if (isCritical) {
            double maxCrit = pvpSettings.getOrDefault("max_crit_damage", 200.0);
            // 限制暴击伤害
        }
        
        return damage;
    }

    public void reloadConfig() {
        loadConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getPrecision() {
        return precision;
    }

    public String getFormula(String key) {
        return formulas.get(key);
    }

    public Map<String, String> getAllFormulas() {
        return new HashMap<>(formulas);
    }
}
