package cn.guangdian.mobs.migration;

import cn.guangdian.mobs.GuangDianMobs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MythicMobs 迁移工具
 * 将 MythicMobs 的配置转换为 GuangDianMobs 格式
 */
public class MythicMobsMigrator {

    private final GuangDianMobs plugin;
    private final MigrationReport report;

    // MythicMobs 路径
    private final File mythicMobsDir;
    private final File mythicMobsMobsDir;
    private final File mythicMobsSkillsDir;
    private final File mythicMobsSpawnersDir;

    public MythicMobsMigrator(GuangDianMobs plugin) {
        this.plugin = plugin;
        this.report = new MigrationReport();

        // 初始化 MythicMobs 目录路径
        this.mythicMobsDir = new File(plugin.getServer().getWorldContainer(), "plugins/MythicMobs");
        this.mythicMobsMobsDir = new File(mythicMobsDir, "mobs");
        this.mythicMobsSkillsDir = new File(mythicMobsDir, "skills");
        this.mythicMobsSpawnersDir = new File(mythicMobsDir, "spawners");
    }

    /**
     * 执行完整迁移
     */
    public MigrationReport migrateAll() {
        report.clear();
        plugin.getLogger().info("开始 MythicMobs 迁移...");

        // 1. 迁移怪物配置
        migrateMobs();

        // 2. 迁移技能配置
        migrateSkills();

        // 3. 迁移刷新点配置
        migrateSpawners();

        plugin.getLogger().info("MythicMobs 迁移完成!");
        report.printSummary(plugin.getLogger());

        return report;
    }

    /**
     * 迁移怪物配置
     */
    public void migrateMobs() {
        if (!mythicMobsMobsDir.exists() || !mythicMobsMobsDir.isDirectory()) {
            report.addWarning("怪物", "MythicMobs mobs 目录不存在: " + mythicMobsMobsDir.getPath());
            return;
        }

        plugin.getLogger().info("正在迁移怪物配置...");

        // 加载所有 mobs YAML 文件
        List<File> mobFiles = listYamlFiles(mythicMobsMobsDir);
        YamlConfiguration outputConfig = new YamlConfiguration();
        ConfigurationSection mobsSection = outputConfig.createSection("mobs");

        for (File file : mobFiles) {
            try {
                YamlConfiguration inputConfig = YamlConfiguration.loadConfiguration(file);
                for (String mobName : inputConfig.getKeys(false)) {
                    if (isItemConfig(inputConfig, mobName)) {
                        continue; // 跳过物品配置
                    }
                    try {
                        ConfigurationSection mobSection = inputConfig.getConfigurationSection(mobName);
                        if (mobSection != null) {
                            migrateMob(mobName, mobSection, mobsSection);
                            report.incrementMobsMigrated();
                        }
                    } catch (Exception e) {
                        report.addError("怪物", mobName, e.getMessage());
                    }
                }
            } catch (Exception e) {
                report.addError("怪物文件", file.getName(), e.getMessage());
            }
        }

        // 保存迁移后的配置
        saveConfig(outputConfig, "mobs_migrated.yml");
        plugin.getLogger().info("怪物配置已迁移到 mobs_migrated.yml");
    }

    /**
     * 迁移单个怪物
     */
    private void migrateMob(String mobName, ConfigurationSection input, ConfigurationSection output) {
        String mobId = sanitizeId(mobName);
        ConfigurationSection mobSection = output.createSection(mobId);

        // 基础属性
        mobSection.set("display-name", convertColorCodes(input.getString("Display", mobName)));
        mobSection.set("type", input.getString("MobType", input.getString("Type", "ZOMBIE")).toUpperCase());
        mobSection.set("health", input.getDouble("Health", 20.0));
        mobSection.set("damage", input.getDouble("Damage", 3.0));
        mobSection.set("defense", input.getDouble("Armor", 0.0));
        mobSection.set("level", input.getInt("Levels.Primary", 1));

        // Options 映射
        ConfigurationSection options = input.getConfigurationSection("Options");
        if (options != null) {
            mobSection.set("speed", options.getDouble("MovementSpeed", 0.23));
            mobSection.set("attack-speed", options.getDouble("AttackSpeed", 1.0));
            mobSection.set("follow-range", options.getDouble("FollowRange", 32.0));

            // MobOptions
            ConfigurationSection mobOptions = mobSection.createSection("options");
            mobOptions.set("always-show-name", options.getBoolean("AlwaysShowName", false));
            mobOptions.set("prevent-other-drops", options.getBoolean("PreventOtherDrops", false));
            mobOptions.set("knockback-resistance", options.getDouble("KnockbackResistance", 0.0));

            // BossBar
            ConfigurationSection bossBar = options.getConfigurationSection("BossBar");
            if (bossBar != null && bossBar.getBoolean("enabled", false)) {
                mobOptions.set("show-boss-bar", true);
                mobOptions.set("boss-bar-color", bossBar.getString("color", "RED").toUpperCase());
                mobOptions.set("boss-bar-style", bossBar.getString("style", "SOLID").toUpperCase());
            }

            // 史莱姆大小
            if (options.contains("Size")) {
                mobOptions.set("size", options.getInt("Size"));
            }
        }

        // DamageModifiers
        List<String> damageModifiers = input.getStringList("DamageModifiers");
        if (!damageModifiers.isEmpty()) {
            ConfigurationSection dmgMods = mobSection.createSection("damage-modifiers");
            for (String mod : damageModifiers) {
                String[] parts = mod.split("\\s+");
                if (parts.length >= 2) {
                    String type = parts[0];
                    double value = parseDoubleSafe(parts[1], 1.0);
                    dmgMods.set(type, value);
                }
            }
        }

        // Equipment
        List<String> equipment = input.getStringList("Equipment");
        if (!equipment.isEmpty()) {
            ConfigurationSection equipSection = mobSection.createSection("equipment");
            for (String equip : equipment) {
                String[] parts = equip.split("\\s+");
                if (parts.length >= 2) {
                    String item = parts[0];
                    String slot = parts[1].toUpperCase();

                    // 转换 slot 名称
                    String gdSlot = switch (slot) {
                        case "HEAD", "HELMET" -> "helmet";
                        case "CHEST", "CHESTPLATE" -> "chestplate";
                        case "LEGS", "LEGGINGS" -> "leggings";
                        case "FEET", "BOOTS" -> "boots";
                        case "HAND", "MAINHAND" -> "main-hand";
                        case "OFFHAND" -> "off-hand";
                        default -> slot.toLowerCase();
                    };
                    equipSection.set(gdSlot, item.toUpperCase());
                }
            }
        }

        // Skills - 转换为 GuangDianMobs 格式
        List<String> skills = input.getStringList("Skills");
        if (!skills.isEmpty()) {
            List<String> skillIds = new ArrayList<>();
            for (String skillLine : skills) {
                String skillId = parseMythicSkill(skillLine);
                if (skillId != null) {
                    skillIds.add(skillId);
                }
            }
            if (!skillIds.isEmpty()) {
                mobSection.set("skills", skillIds);
            }
        }

        // AI 配置
        ConfigurationSection modules = input.getConfigurationSection("Modules");
        if (modules != null && modules.getBoolean("ThreatTable", false)) {
            ConfigurationSection aiSection = mobSection.createSection("ai");
            aiSection.set("target-selectors", List.of("players", "highestthreat"));

            ConfigurationSection threatSection = aiSection.createSection("threat");
            threatSection.set("use-threat-table", true);
            threatSection.set("radius", 32.0);
        }

        // AITargetSelectors
        List<String> aiTargetSelectors = input.getStringList("AITargetSelectors");
        if (!aiTargetSelectors.isEmpty()) {
            ConfigurationSection aiSection = mobSection.getConfigurationSection("ai");
            if (aiSection == null) {
                aiSection = mobSection.createSection("ai");
            }
            List<String> selectors = new ArrayList<>();
            for (String selector : aiTargetSelectors) {
                if (!selector.equals("0 Clear")) {
                    selectors.add(selector.replaceAll("^\\d+\\s+", "").toLowerCase());
                }
            }
            aiSection.set("target-selectors", selectors);
        }

        // Drops - 简单处理，指向掉落表
        List<String> drops = input.getStringList("Drops");
        if (!drops.isEmpty()) {
            mobSection.set("drop-table", mobId + "_drops");
        }
    }

    /**
     * 迁移技能配置
     */
    public void migrateSkills() {
        if (!mythicMobsSkillsDir.exists() || !mythicMobsSkillsDir.isDirectory()) {
            report.addWarning("技能", "MythicMobs skills 目录不存在: " + mythicMobsSkillsDir.getPath());
            return;
        }

        plugin.getLogger().info("正在迁移技能配置...");

        List<File> skillFiles = listYamlFiles(mythicMobsSkillsDir);
        YamlConfiguration outputConfig = new YamlConfiguration();
        ConfigurationSection skillsSection = outputConfig.createSection("skills");

        for (File file : skillFiles) {
            try {
                YamlConfiguration inputConfig = YamlConfiguration.loadConfiguration(file);
                for (String skillName : inputConfig.getKeys(false)) {
                    try {
                        ConfigurationSection skillSection = inputConfig.getConfigurationSection(skillName);
                        if (skillSection != null) {
                            migrateSkill(skillName, skillSection, skillsSection);
                            report.incrementSkillsMigrated();
                        }
                    } catch (Exception e) {
                        report.addError("技能", skillName, e.getMessage());
                    }
                }
            } catch (Exception e) {
                report.addError("技能文件", file.getName(), e.getMessage());
            }
        }

        saveConfig(outputConfig, "skills_migrated.yml");
        plugin.getLogger().info("技能配置已迁移到 skills_migrated.yml");
    }

    /**
     * 迁移单个技能
     */
    private void migrateSkill(String skillName, ConfigurationSection input, ConfigurationSection output) {
        String skillId = sanitizeId(skillName);
        ConfigurationSection skillSection = output.createSection(skillId);

        skillSection.set("display-name", convertColorCodes("<yellow>" + skillName));

        // 冷却时间 (秒转 tick)
        int cooldownSeconds = input.getInt("Cooldown", 5);
        skillSection.set("cooldown", cooldownSeconds * 20);

        // 范围
        skillSection.set("range", input.getDouble("Range", 10.0));

        // 触发几率
        skillSection.set("chance", input.getDouble("Chance", 0.3));

        // 条件
        List<String> conditions = input.getStringList("Conditions");
        if (!conditions.isEmpty()) {
            List<String> gdConditions = new ArrayList<>();
            for (String condition : conditions) {
                gdConditions.add(convertCondition(condition));
            }
            skillSection.set("conditions", gdConditions);
        }

        // 解析技能类型和效果
        List<String> mythicSkills = input.getStringList("Skills");
        if (!mythicSkills.isEmpty()) {
            MobSkillType skillType = analyzeSkillType(mythicSkills);
            skillSection.set("type", skillType.name());

            // 提取伤害
            double damage = extractDamage(mythicSkills);
            if (damage > 0) {
                skillSection.set("damage", damage);
            }

            // 提取治疗
            double heal = extractHeal(mythicSkills);
            if (heal > 0) {
                skillSection.set("heal", heal);
            }

            // 提取效果
            List<String> effects = extractEffects(mythicSkills);
            if (!effects.isEmpty()) {
                skillSection.set("effects", effects);
            }

            // 提取粒子
            String particle = extractParticle(mythicSkills);
            if (particle != null) {
                skillSection.set("particle", particle);
            }

            // 提取音效
            String sound = extractSound(mythicSkills);
            if (sound != null) {
                skillSection.set("sound", sound);
            }

            // 提取消息
            String message = extractMessage(mythicSkills);
            if (message != null) {
                skillSection.set("message", convertColorCodes(message));
            }

            // 目标选择器
            String target = extractTarget(mythicSkills);
            if (target != null) {
                skillSection.set("target", target);
            }
        }
    }

    /**
     * 迁移刷新点配置
     */
    public void migrateSpawners() {
        if (!mythicMobsSpawnersDir.exists() || !mythicMobsSpawnersDir.isDirectory()) {
            report.addWarning("刷新点", "MythicMobs spawners 目录不存在: " + mythicMobsSpawnersDir.getPath());
            return;
        }

        plugin.getLogger().info("正在迁移刷新点配置...");

        List<File> spawnerFiles = listYamlFiles(mythicMobsSpawnersDir);
        YamlConfiguration outputConfig = new YamlConfiguration();
        ConfigurationSection spawnpointsSection = outputConfig.createSection("spawnpoints");

        for (File file : spawnerFiles) {
            try {
                YamlConfiguration inputConfig = YamlConfiguration.loadConfiguration(file);
                for (String spawnerName : inputConfig.getKeys(false)) {
                    try {
                        ConfigurationSection spawnerSection = inputConfig.getConfigurationSection(spawnerName);
                        if (spawnerSection != null) {
                            migrateSpawner(spawnerName, spawnerSection, spawnpointsSection);
                            report.incrementSpawnersMigrated();
                        }
                    } catch (Exception e) {
                        report.addError("刷新点", spawnerName, e.getMessage());
                    }
                }
            } catch (Exception e) {
                report.addError("刷新点文件", file.getName(), e.getMessage());
            }
        }

        saveConfig(outputConfig, "spawnpoints_migrated.yml");
        plugin.getLogger().info("刷新点配置已迁移到 spawnpoints_migrated.yml");
    }

    /**
     * 迁移单个刷新点
     */
    private void migrateSpawner(String spawnerName, ConfigurationSection input, ConfigurationSection output) {
        String spawnerId = sanitizeId(spawnerName);
        ConfigurationSection spawnerSection = output.createSection(spawnerId);

        // 位置
        spawnerSection.set("world", input.getString("World", "world"));
        spawnerSection.set("x", input.getDouble("X", 0));
        spawnerSection.set("y", input.getDouble("Y", 0));
        spawnerSection.set("z", input.getDouble("Z", 0));
        spawnerSection.set("yaw", 0);
        spawnerSection.set("pitch", 0);

        // 怪物
        String mobName = input.getString("MobName", "");
        spawnerSection.set("mob", sanitizeId(mobName));
        spawnerSection.set("level", input.getInt("MobLevel", 1));

        // 刷新设置
        spawnerSection.set("amount", input.getInt("MobsPerSpawn", 1));
        spawnerSection.set("max-mobs", input.getInt("MaxMobs", 1));
        spawnerSection.set("cooldown", input.getInt("Cooldown", 60) * 20); // 秒转 tick
        spawnerSection.set("radius", input.getDouble("Radius", 5.0));

        // 其他选项
        spawnerSection.set("use-timer", input.getBoolean("UseTimer", true));
        spawnerSection.set("require-player", input.getBoolean("CheckForPlayers", true));
        spawnerSection.set("player-range", input.getDouble("ActivationRange", 30.0));
        spawnerSection.set("enabled", true);

        // 显示名称
        spawnerSection.set("display-name", spawnerName);
    }

    // ==================== 工具方法 ====================

    /**
     * 列出目录中所有 YAML 文件
     */
    private List<File> listYamlFiles(File directory) {
        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".yml") || name.endsWith(".yaml");
                    })
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * 保存配置到插件数据目录
     */
    private void saveConfig(YamlConfiguration config, String filename) {
        File file = new File(plugin.getDataFolder(), filename);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("保存迁移配置失败: " + filename + " - " + e.getMessage());
        }
    }

    /**
     * 检查是否是物品配置（不是怪物）
     */
    private boolean isItemConfig(ConfigurationSection section, String key) {
        ConfigurationSection mobSection = section.getConfigurationSection(key);
        if (mobSection == null) return false;
        String type = mobSection.getString("Type", "").toLowerCase();
        return type.equals("item");
    }

    /**
     * 转换颜色代码 & → MiniMessage <>
     */
    private String convertColorCodes(String text) {
        if (text == null) return null;

        // 替换 & 颜色代码为 MiniMessage 格式
        String result = text
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underline>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");

        return result;
    }

    /**
     * 清理 ID（移除特殊字符）
     */
    private String sanitizeId(String id) {
        if (id == null) return "unknown";
        return id.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * 解析 MythicMobs 技能行
     * 格式: skill{s=技能名} @目标 ~onTimer:100 0.3
     */
    private String parseMythicSkill(String skillLine) {
        // 提取 skill{s=xxx} 中的技能名
        if (skillLine.contains("skill{s=")) {
            int start = skillLine.indexOf("skill{s=") + 8;
            int end = skillLine.indexOf("}", start);
            if (end > start) {
                String skillName = skillLine.substring(start, end);
                return sanitizeId(skillName);
            }
        }
        return null;
    }

    /**
     * 转换条件
     */
    private String convertCondition(String condition) {
        // targetwithin X → targetwithin X
        if (condition.startsWith("targetwithin")) {
            return condition;
        }
        return condition;
    }

    /**
     * 分析技能类型
     */
    private MobSkillType analyzeSkillType(List<String> skills) {
        for (String skill : skills) {
            String lower = skill.toLowerCase();
            if (lower.contains("heal")) return MobSkillType.HEAL;
            if (lower.contains("teleport")) return MobSkillType.TELEPORT;
            if (lower.contains("summon")) return MobSkillType.SUMMON;
            if (lower.contains("shoot")) return MobSkillType.PROJECTILE;
            if (lower.contains("potion")) {
                if (lower.contains("resistance") || lower.contains("speed") || lower.contains("strength")) {
                    return MobSkillType.BUFF;
                }
                return MobSkillType.DEBUFF;
            }
        }
        return MobSkillType.DAMAGE;
    }

    /**
     * 提取伤害值
     */
    private double extractDamage(List<String> skills) {
        for (String skill : skills) {
            if (skill.contains("damage{")) {
                int start = skill.indexOf("amount=");
                if (start > 0) {
                    start += 7;
                    int end = skill.indexOf(";", start);
                    if (end < 0) end = skill.indexOf("}", start);
                    if (end > start) {
                        return parseDoubleSafe(skill.substring(start, end), 0);
                    }
                }
            }
        }
        return 0;
    }

    /**
     * 提取治疗值
     */
    private double extractHeal(List<String> skills) {
        for (String skill : skills) {
            if (skill.contains("heal{")) {
                int start = skill.indexOf("amount=");
                if (start > 0) {
                    start += 7;
                    int end = skill.indexOf(";", start);
                    if (end < 0) end = skill.indexOf("}", start);
                    if (end > start) {
                        return parseDoubleSafe(skill.substring(start, end), 0);
                    }
                }
            }
        }
        return 0;
    }

    /**
     * 提取效果
     */
    private List<String> extractEffects(List<String> skills) {
        List<String> effects = new ArrayList<>();
        for (String skill : skills) {
            if (skill.contains("potion{")) {
                int start = skill.indexOf("type=");
                if (start > 0) {
                    start += 5;
                    int end = skill.indexOf(";", start);
                    if (end < 0) end = skill.indexOf("}", start);
                    if (end > start) {
                        effects.add(skill.substring(start, end).toUpperCase());
                    }
                }
            }
        }
        return effects;
    }

    /**
     * 提取粒子效果
     */
    private String extractParticle(List<String> skills) {
        for (String skill : skills) {
            if (skill.contains("effect:particle{")) {
                int start = skill.indexOf("v=");
                if (start > 0) {
                    start += 2;
                    int end = skill.indexOf(";", start);
                    if (end < 0) end = skill.indexOf("}", start);
                    if (end > start) {
                        return skill.substring(start, end).toUpperCase();
                    }
                }
            }
            if (skill.contains("effect:lightning")) {
                return "END_ROD";
            }
            if (skill.contains("effect:explosion")) {
                return "EXPLOSION";
            }
        }
        return null;
    }

    /**
     * 提取音效
     */
    private String extractSound(List<String> skills) {
        for (String skill : skills) {
            if (skill.contains("effect:sound{")) {
                int start = skill.indexOf("s=");
                if (start > 0) {
                    start += 2;
                    int end = skill.indexOf(";", start);
                    if (end < 0) end = skill.indexOf("}", start);
                    if (end > start) {
                        return skill.substring(start, end).toUpperCase().replace(".", "_");
                    }
                }
            }
        }
        return null;
    }

    /**
     * 提取消息
     */
    private String extractMessage(List<String> skills) {
        for (String skill : skills) {
            if (skill.contains("message{")) {
                int start = skill.indexOf("msg=\"");
                if (start > 0) {
                    start += 5;
                    int end = skill.indexOf("\"", start);
                    if (end > start) {
                        return skill.substring(start, end);
                    }
                }
                // 简写格式: message{m="xxx"}
                start = skill.indexOf("m=\"");
                if (start > 0) {
                    start += 3;
                    int end = skill.indexOf("\"", start);
                    if (end > start) {
                        return skill.substring(start, end);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 提取目标选择器
     */
    private String extractTarget(List<String> skills) {
        for (String skill : skills) {
            if (skill.contains("@target")) return "TARGET";
            if (skill.contains("@self")) return "SELF";
            if (skill.contains("@playersinradius")) {
                int start = skill.indexOf("{r=");
                if (start > 0) {
                    start += 3;
                    int end = skill.indexOf("}", start);
                    if (end > start) {
                        String radius = skill.substring(start, end);
                        return "@PlayersInRadius{r=" + radius + "}";
                    }
                }
                return "@PlayersInRadius{r=10}";
            }
        }
        return null;
    }

    /**
     * 安全解析 double
     */
    private double parseDoubleSafe(String value, double defaultValue) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取迁移报告
     */
    public MigrationReport getReport() {
        return report;
    }

    /**
     * 技能类型枚举
     */
    public enum MobSkillType {
        DAMAGE, HEAL, BUFF, DEBUFF, SUMMON, TELEPORT, PROJECTILE
    }
}
