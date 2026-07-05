package cn.guangdian.fakeplayer;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 伪装玩家配置类
 */
public class FakePlayerConfig {

    private final GuangDianFakePlayer plugin;
    private String mode;
    private int fixedOnline;
    private int minOnline;
    private int maxOnline;
    private double multiplier;
    private int fakeMaxPlayers;
    private boolean customMotdEnabled;
    private String motdLine1;
    private String motdLine2;
    private boolean placeholderapiEnabled;
    private String placeholderPrefix;
    private boolean debug;
    private Random random = new Random();

    // TAB列表虚拟玩家配置
    private boolean virtualPlayersEnabled;
    private int virtualPlayersCount;
    private boolean useProtocolLib;
    private String nameMode;
    private List<String> presetNames;

    // 动态行为配置
    private boolean dynamicEnabled;
    private int dynamicInterval;
    private int changeMin;
    private int changeMax;
    private int minCount;
    private int maxCount;

    // 显示格式配置
    private VirtualPlayerFormat defaultFormat;
    private final List<VirtualPlayerFormat> groupFormats = new ArrayList<>();

    // 特殊标签配置
    private SpecialTagsConfig specialTags;

    public FakePlayerConfig(GuangDianFakePlayer plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        mode = config.getString("mode", "FIXED");
        fixedOnline = config.getInt("fixed-online", 50);
        minOnline = config.getInt("min-online", 30);
        maxOnline = config.getInt("max-online", 80);
        multiplier = config.getDouble("multiplier", 1.5);
        fakeMaxPlayers = config.getInt("fake-max-players", 100);

        customMotdEnabled = config.getBoolean("custom-motd.enabled", false);
        motdLine1 = config.getString("custom-motd.line1", "&a译梦传说 RPG服务器");
        motdLine2 = config.getString("custom-motd.line2", "&e当前在线: %fake_online% 人");

        placeholderapiEnabled = config.getBoolean("placeholderapi.enabled", true);
        placeholderPrefix = config.getString("placeholderapi.prefix", "gdfakeplayer");

        debug = config.getBoolean("debug", false);

        // TAB列表虚拟玩家配置
        virtualPlayersEnabled = config.getBoolean("virtual-players.enabled", true);
        virtualPlayersCount = config.getInt("virtual-players.count", 50);
        useProtocolLib = config.getBoolean("virtual-players.use-protocollib", true);
        nameMode = config.getString("virtual-players.name-mode", "preset");
        presetNames = config.getStringList("virtual-players.preset-names");

        // 动态行为配置
        dynamicEnabled = config.getBoolean("dynamic.enabled", true);
        dynamicInterval = config.getInt("dynamic.interval", 300);
        String changeRange = config.getString("dynamic.change-range", "1-3");
        if (changeRange.contains("-")) {
            String[] parts = changeRange.split("-");
            changeMin = Integer.parseInt(parts[0]);
            changeMax = Integer.parseInt(parts[1]);
        } else {
            changeMin = 1;
            changeMax = 3;
        }
        minCount = config.getInt("dynamic.min-count", 40);
        maxCount = config.getInt("dynamic.max-count", 60);

        // 加载显示格式配置
        loadFormats(config);
        
        // 加载特殊标签配置
        loadSpecialTags(config);
    }

    /**
     * 加载显示格式配置
     */
    private void loadFormats(FileConfiguration config) {
        // 加载默认格式
        ConfigurationSection defaultSection = config.getConfigurationSection("virtual-players.default-format");
        if (defaultSection != null) {
            defaultFormat = new VirtualPlayerFormat(
                "default",
                defaultSection.getString("prefix", "<gray>◆ <white>"),
                defaultSection.getString("name", "<gray>%player_name%"),
                defaultSection.getString("suffix", " <dark_gray>[<gray>Lv.%player_level%<dark_gray>]"),
                new ArrayList<>()
            );
        } else {
            defaultFormat = new VirtualPlayerFormat(
                "default",
                "<gray>◆ <white>",
                "<gray>%player_name%",
                " <dark_gray>[<gray>Lv.%player_level%<dark_gray>]",
                new ArrayList<>()
            );
        }

        // 加载权限组格式
        groupFormats.clear();
        ConfigurationSection groupSection = config.getConfigurationSection("virtual-players.group-formats");
        if (groupSection != null) {
            for (String key : groupSection.getKeys(false)) {
                ConfigurationSection section = groupSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                List<String> matchNames = section.getStringList("match-names");
                groupFormats.add(new VirtualPlayerFormat(
                    key,
                    section.getString("prefix", ""),
                    section.getString("name", "%player_name%"),
                    section.getString("suffix", ""),
                    matchNames
                ));
            }
        }
    }

    /**
     * 根据虚拟玩家名字获取对应的显示格式
     */
    public VirtualPlayerFormat getFormatForName(String name) {
        for (VirtualPlayerFormat format : groupFormats) {
            if (format.matchNames().contains(name)) {
                return format;
            }
        }
        return defaultFormat;
    }

    /**
     * 构建虚拟玩家的显示名称（包含特殊标签）
     */
    public String buildDisplayName(String name, VirtualPlayerFormat format) {
        // 先构建特殊标签
        String specialTagsStr = buildSpecialTags();
        
        // 构建基本显示名称
        String displayName = specialTagsStr + format.prefix() + format.name() + format.suffix();
        
        // 替换占位符
        displayName = displayName.replace("%player_name%", name);
        // 随机等级 (1-50)
        int level = 1 + random.nextInt(50);
        displayName = displayName.replace("%player_level%", String.valueOf(level));
        // 随机 Ping (20-80)
        int ping = 20 + random.nextInt(60);
        displayName = displayName.replace("%player_ping%", String.valueOf(ping));
        
        return displayName;
    }

    /**
     * 构建特殊标签（工会、婚姻、战斗、AFK等）
     */
    private String buildSpecialTags() {
        if (specialTags == null) {
            return "";
        }
        
        StringBuilder tags = new StringBuilder();
        
        // 工会标签
        if (specialTags.guildEnabled && random.nextDouble() < specialTags.guildProbability) {
            String guildName = specialTags.guildNames.get(random.nextInt(specialTags.guildNames.size()));
            String guildTag = specialTags.hasGuild.replace("%guild_name%", guildName);
            tags.append(guildTag);
        }
        
        // 婚姻标签
        if (specialTags.marriageEnabled && random.nextDouble() < specialTags.marriageProbability) {
            String spouseName = specialTags.spouseNames.get(random.nextInt(specialTags.spouseNames.size()));
            String marriageTag = specialTags.married.replace("%spouse%", spouseName);
            tags.append(marriageTag);
        }
        
        // 战斗标签
        if (specialTags.combatEnabled && random.nextDouble() < specialTags.combatProbability) {
            tags.append(specialTags.inCombat);
        }
        
        // AFK 标签
        if (specialTags.afkEnabled && random.nextDouble() < specialTags.afkProbability) {
            tags.append(specialTags.afkFormat);
        }
        
        return tags.toString();
    }

    /**
     * 加载特殊标签配置
     */
    private void loadSpecialTags(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("virtual-players.special-tags");
        if (section == null) {
            // 使用默认配置
            specialTags = new SpecialTagsConfig(
                true, true, true, true,
                "<aqua><bold>⚔ %guild_name% <reset>", "",
                "<light_purple><bold>❤ %spouse% <reset>", "",
                "<red><bold>⚔ 战斗 <reset>", "",
                "<gray>[AFK] <white>",
                0.3, 0.2, 0.1, 0.05,
                getDefaultGuildNames(),
                getDefaultSpouseNames()
            );
            return;
        }
        
        // 工会配置
        ConfigurationSection guildSection = section.getConfigurationSection("guild");
        boolean guildEnabled = guildSection != null && guildSection.getBoolean("enabled", true);
        String hasGuild = guildSection != null ? guildSection.getString("has-guild", "<aqua><bold>⚔ %guild_name% <reset>") : "<aqua><bold>⚔ %guild_name% <reset>";
        String noGuild = guildSection != null ? guildSection.getString("no-guild", "") : "";
        List<String> guildNames = guildSection != null ? guildSection.getStringList("guild-names") : getDefaultGuildNames();
        
        // 婚姻配置
        ConfigurationSection marriageSection = section.getConfigurationSection("marriage");
        boolean marriageEnabled = marriageSection != null && marriageSection.getBoolean("enabled", true);
        String married = marriageSection != null ? marriageSection.getString("married", "<light_purple><bold>❤ %spouse% <reset>") : "<light_purple><bold>❤ %spouse% <reset>";
        String single = marriageSection != null ? marriageSection.getString("single", "") : "";
        List<String> spouseNames = marriageSection != null ? marriageSection.getStringList("spouse-names") : getDefaultSpouseNames();
        
        // 战斗配置
        ConfigurationSection combatSection = section.getConfigurationSection("combat");
        boolean combatEnabled = combatSection != null && combatSection.getBoolean("enabled", true);
        String inCombat = combatSection != null ? combatSection.getString("in-combat", "<red><bold>⚔ 战斗 <reset>") : "<red><bold>⚔ 战斗 <reset>";
        String outCombat = combatSection != null ? combatSection.getString("out-combat", "") : "";
        
        // AFK 配置
        ConfigurationSection afkSection = section.getConfigurationSection("afk");
        boolean afkEnabled = afkSection != null && afkSection.getBoolean("enabled", true);
        String afkFormat = afkSection != null ? afkSection.getString("format", "<gray>[AFK] <white>") : "<gray>[AFK] <white>";
        
        // 显示概率配置
        ConfigurationSection probabilitySection = section.getConfigurationSection("display-probability");
        double guildProbability = probabilitySection != null ? probabilitySection.getDouble("guild", 0.3) : 0.3;
        double marriageProbability = probabilitySection != null ? probabilitySection.getDouble("marriage", 0.2) : 0.2;
        double combatProbability = probabilitySection != null ? probabilitySection.getDouble("combat", 0.1) : 0.1;
        double afkProbability = probabilitySection != null ? probabilitySection.getDouble("afk", 0.05) : 0.05;
        
        specialTags = new SpecialTagsConfig(
            guildEnabled, marriageEnabled, combatEnabled, afkEnabled,
            hasGuild, noGuild,
            married, single,
            inCombat, outCombat,
            afkFormat,
            guildProbability, marriageProbability, combatProbability, afkProbability,
            guildNames, spouseNames
        );
    }

    /**
     * 获取默认工会名称列表
     */
    private List<String> getDefaultGuildNames() {
        List<String> names = new ArrayList<>();
        names.add("星辰公会");
        names.add("龙魂公会");
        names.add("天启公会");
        names.add("暗影公会");
        names.add("光明公会");
        names.add("风暴公会");
        names.add("烈焰公会");
        names.add("冰霜公会");
        names.add("雷霆公会");
        names.add("荣耀公会");
        return names;
    }

    /**
     * 获取默认伴侣名称列表
     */
    private List<String> getDefaultSpouseNames() {
        List<String> names = new ArrayList<>();
        names.add("小可爱");
        names.add("大宝贝");
        names.add("心上人");
        names.add("挚爱");
        names.add("甜蜜");
        return names;
    }

    /**
     * 获取特殊标签配置
     */
    public SpecialTagsConfig getSpecialTags() {
        return specialTags;
    }

    /**
     * 获取伪装的在线玩家数
     * 伪装在线数 = 真实在线数 + 虚拟人数
     */
    public int getFakeOnlineCount(int realOnline) {
        int virtualCount;

        switch (mode.toUpperCase()) {
            case "FIXED":
                virtualCount = fixedOnline;
                break;

            case "RANGE":
                virtualCount = minOnline + random.nextInt(maxOnline - minOnline + 1);
                break;

            case "MULTIPLIER":
                // 倍数模式：真实在线数 * 倍数（虚拟人数 = 真实在线数 * (倍数 - 1))
                virtualCount = (int) Math.ceil(realOnline * (multiplier - 1));
                break;

            case "ADD":
                // 加法模式：虚拟人数 = 固定值
                virtualCount = fixedOnline;
                break;

            default:
                virtualCount = fixedOnline;
                break;
        }

        // 伪装在线数 = 真实在线数 + 虚拟人数
        return realOnline + virtualCount;
    }

    /**
     * 获取伪装的最大玩家数
     */
    public int getFakeMaxPlayers() {
        return fakeMaxPlayers;
    }

    /**
     * 是否启用自定义 MOTD
     */
    public boolean hasCustomMotd() {
        return customMotdEnabled;
    }

    /**
     * 获取 MOTD
     */
    public String getMotd(int realOnline, int fakeOnline) {
        String line1 = motdLine1.replace("%real_online%", String.valueOf(realOnline))
                                 .replace("%fake_online%", String.valueOf(fakeOnline));
        String line2 = motdLine2.replace("%real_online%", String.valueOf(realOnline))
                                 .replace("%fake_online%", String.valueOf(fakeOnline));

        line1 = translateColors(line1);
        line2 = translateColors(line2);

        return line1 + "\n" + line2;
    }

    /**
     * 是否启用PlaceholderAPI
     */
    public boolean isPlaceholderapiEnabled() {
        return placeholderapiEnabled;
    }

    /**
     * 获取占位符前缀
     */
    public String getPlaceholderPrefix() {
        return placeholderPrefix;
    }

    /**
     * 获取模式
     */
    public String getMode() {
        return mode;
    }

    /**
     * 是否启用调试模式
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * 是否启用虚拟玩家显示
     */
    public boolean isVirtualPlayersEnabled() {
        return virtualPlayersEnabled;
    }

    /**
     * 获取虚拟玩家数量
     */
    public int getVirtualPlayersCount() {
        return virtualPlayersCount;
    }

    /**
     * 是否使用ProtocolLib
     */
    public boolean isUseProtocolLib() {
        return useProtocolLib;
    }

    /**
     * 获取名字生成模式
     */
    public String getNameMode() {
        return nameMode;
    }

    /**
     * 获取预设名字列表
     */
    public List<String> getPresetNames() {
        return presetNames;
    }

    /**
     * 是否启用动态行为
     */
    public boolean isDynamicEnabled() {
        return dynamicEnabled;
    }

    /**
     * 获取动态更新间隔（秒）
     */
    public int getDynamicInterval() {
        return dynamicInterval;
    }

    /**
     * 获取变化最小值
     */
    public int getChangeMin() {
        return changeMin;
    }

    /**
     * 获取变化最大值
     */
    public int getChangeMax() {
        return changeMax;
    }

    /**
     * 获取最小虚拟玩家数
     */
    public int getMinCount() {
        return minCount;
    }

    /**
     * 获取最大虚拟玩家数
     */
    public int getMaxCount() {
        return maxCount;
    }

    /**
     * 转换颜色代码
     */
    private String translateColors(String text) {
        return text.replace("&0", "§0")
                   .replace("&1", "§1")
                   .replace("&2", "§2")
                   .replace("&3", "§3")
                   .replace("&4", "§4")
                   .replace("&5", "§5")
                   .replace("&6", "§6")
                   .replace("&7", "§7")
                   .replace("&8", "§8")
                   .replace("&9", "§9")
                   .replace("&a", "§a")
                   .replace("&b", "§b")
                   .replace("&c", "§c")
                   .replace("&d", "§d")
                   .replace("&e", "§e")
                   .replace("&f", "§f")
                   .replace("&l", "§l")
                   .replace("&m", "§m")
                   .replace("&n", "§n")
                   .replace("&o", "§o")
                   .replace("&r", "§r");
    }

    /**
     * 将 MiniMessage 格式转换为 legacy § 格式
     * 用于 PlayerInfoData 的显示名称
     */
    public String miniMessageToLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input
            .replace("<black>", "§0").replace("</black>", "")
            .replace("<dark_blue>", "§1").replace("</dark_blue>", "")
            .replace("<dark_green>", "§2").replace("</dark_green>", "")
            .replace("<dark_aqua>", "§3").replace("</dark_aqua>", "")
            .replace("<dark_red>", "§4").replace("</dark_red>", "")
            .replace("<dark_purple>", "§5").replace("</dark_purple>", "")
            .replace("<gold>", "§6").replace("</gold>", "")
            .replace("<gray>", "§7").replace("</gray>", "")
            .replace("<dark_gray>", "§8").replace("</dark_gray>", "")
            .replace("<blue>", "§9").replace("</blue>", "")
            .replace("<green>", "§a").replace("</green>", "")
            .replace("<aqua>", "§b").replace("</aqua>", "")
            .replace("<red>", "§c").replace("</red>", "")
            .replace("<light_purple>", "§d").replace("</light_purple>", "")
            .replace("<yellow>", "§e").replace("</yellow>", "")
            .replace("<white>", "§f").replace("</white>", "")
            .replace("<bold>", "§l").replace("</bold>", "")
            .replace("<underlined>", "§n").replace("</underlined>", "")
            .replace("<strikethrough>", "§m").replace("</strikethrough>", "")
            .replace("<italic>", "§o").replace("</italic>", "")
            .replace("<obfuscated>", "§k").replace("</obfuscated>", "")
            .replace("<reset>", "§r").replace("</reset>", "");
    }

    /**
     * 获取默认格式
     */
    public VirtualPlayerFormat getDefaultFormat() {
        return defaultFormat;
    }

    /**
     * 获取所有权限组格式
     */
    public List<VirtualPlayerFormat> getGroupFormats() {
        return groupFormats;
    }

    /**
     * 虚拟玩家显示格式记录类
     */
    public record VirtualPlayerFormat(
        String key,
        String prefix,
        String name,
        String suffix,
        List<String> matchNames
    ) {}

    /**
     * 特殊标签配置类
     */
    public static class SpecialTagsConfig {
        public final boolean guildEnabled;
        public final boolean marriageEnabled;
        public final boolean combatEnabled;
        public final boolean afkEnabled;
        
        public final String hasGuild;
        public final String noGuild;
        public final String married;
        public final String single;
        public final String inCombat;
        public final String outCombat;
        public final String afkFormat;
        
        public final double guildProbability;
        public final double marriageProbability;
        public final double combatProbability;
        public final double afkProbability;
        
        public final List<String> guildNames;
        public final List<String> spouseNames;

        public SpecialTagsConfig(
            boolean guildEnabled, boolean marriageEnabled, boolean combatEnabled, boolean afkEnabled,
            String hasGuild, String noGuild,
            String married, String single,
            String inCombat, String outCombat,
            String afkFormat,
            double guildProbability, double marriageProbability, double combatProbability, double afkProbability,
            List<String> guildNames, List<String> spouseNames
        ) {
            this.guildEnabled = guildEnabled;
            this.marriageEnabled = marriageEnabled;
            this.combatEnabled = combatEnabled;
            this.afkEnabled = afkEnabled;
            this.hasGuild = hasGuild;
            this.noGuild = noGuild;
            this.married = married;
            this.single = single;
            this.inCombat = inCombat;
            this.outCombat = outCombat;
            this.afkFormat = afkFormat;
            this.guildProbability = guildProbability;
            this.marriageProbability = marriageProbability;
            this.combatProbability = combatProbability;
            this.afkProbability = afkProbability;
            this.guildNames = guildNames;
            this.spouseNames = spouseNames;
        }
    }
}