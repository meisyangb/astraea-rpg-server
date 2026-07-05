package cn.guangdian.fakeonline;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Random;

/**
 * 伪装在线数配置类
 */
public class FakeOnlineConfig {

    private final GuangDianFakeOnline plugin;
    private String mode;
    private int fixedOnline;
    private int minOnline;
    private int maxOnline;
    private double multiplier;
    private int fakeMaxPlayers;
    private boolean customMotd;
    private String motdLine1;
    private String motdLine2;
    private boolean customSamplePlayers;
    private List<String> samplePlayers;
    private boolean showInTab;
    private boolean showInScoreboard;
    private Random random = new Random();

    // ==================== 调试设置 ====================
    private boolean debug;

    // 虚拟玩家相关配置
    private boolean virtualPlayersEnabled;
    private int virtualPlayerCount;
    private boolean useRealSkins;
    private boolean dynamicEnabled;
    private int dynamicInterval;
    private int minVirtualPlayers;
    private int maxVirtualPlayers;
    private int[] changeRange;
    private String joinMessageFormat;
    private String quitMessageFormat;

    public FakeOnlineConfig(GuangDianFakeOnline plugin) {
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

        customMotd = config.getBoolean("custom-motd.enabled", false);
        motdLine1 = config.getString("custom-motd.line1", "&a译梦传说 RPG服务器");
        motdLine2 = config.getString("custom-motd.line2", "&e当前在线: %fake_online% 人");

        customSamplePlayers = config.getBoolean("custom-sample-players.enabled", false);
        samplePlayers = config.getStringList("custom-sample-players.players");

        showInTab = config.getBoolean("tab-display.enabled", false);
        showInScoreboard = config.getBoolean("scoreboard-display.enabled", false);

        // 虚拟玩家配置
        virtualPlayersEnabled = config.getBoolean("virtual-players.enabled", true);
        virtualPlayerCount = config.getInt("virtual-players.count", 50);
        useRealSkins = config.getBoolean("virtual-players.use-real-skins", true);

        // 动态配置
        dynamicEnabled = config.getBoolean("dynamic.enabled", true);
        dynamicInterval = config.getInt("dynamic.interval", 300);
        minVirtualPlayers = config.getInt("dynamic.min-virtual", 40);
        maxVirtualPlayers = config.getInt("dynamic.max-virtual", 60);
        
        int minChange = config.getInt("dynamic.change-range.min", 1);
        int maxChange = config.getInt("dynamic.change-range.max", 3);
        changeRange = new int[]{minChange, maxChange};

        // 消息配置
        joinMessageFormat = config.getString("messages.join-format", "&e%player% 加入了游戏");
        quitMessageFormat = config.getString("messages.quit-format", "&e%player% 离开了游戏");

        // 调试配置
        debug = config.getBoolean("debug", false);
    }

    /**
     * 获取伪装的在线玩家数
     */
    public int getFakeOnlineCount(int realOnline) {
        switch (mode.toUpperCase()) {
            case "FIXED":
                return fixedOnline;

            case "RANGE":
                return minOnline + random.nextInt(maxOnline - minOnline + 1);

            case "MULTIPLIER":
                return (int) Math.ceil(realOnline * multiplier);

            case "ADD":
                return realOnline + fixedOnline;

            default:
                return fixedOnline;
        }
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
        return customMotd;
    }

    /**
     * 获取 MOTD
     */
    public String getMotd(int realOnline, int fakeOnline) {
        String line1 = motdLine1.replace("%real_online%", String.valueOf(realOnline))
                                 .replace("%fake_online%", String.valueOf(fakeOnline));
        String line2 = motdLine2.replace("%real_online%", String.valueOf(realOnline))
                                 .replace("%fake_online%", String.valueOf(fakeOnline));

        // 转换颜色代码
        line1 = translateColors(line1);
        line2 = translateColors(line2);

        return line1 + "\n" + line2;
    }

    /**
     * 是否启用自定义玩家列表示例
     */
    public boolean hasCustomSamplePlayers() {
        return customSamplePlayers;
    }

    /**
     * 获取玩家列表示例
     */
    public List<String> getSamplePlayers() {
        return samplePlayers;
    }

    /**
     * 获取模式
     */
    public String getMode() {
        return mode;
    }

    /**
     * 是否在TAB列表中显示
     */
    public boolean isShowInTab() {
        return showInTab;
    }

    /**
     * 是否在计分板中显示
     */
    public boolean isShowInScoreboard() {
        return showInScoreboard;
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

    // ==================== 虚拟玩家相关方法 ====================

    /**
     * 是否启用虚拟玩家
     */
    public boolean isVirtualPlayersEnabled() {
        return virtualPlayersEnabled;
    }

    /**
     * 获取虚拟玩家数量
     */
    public int getVirtualPlayerCount() {
        return virtualPlayerCount;
    }

    /**
     * 是否使用真实皮肤
     */
    public boolean isUseRealSkins() {
        return useRealSkins;
    }

    /**
     * 是否启用动态功能
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
     * 获取最少虚拟玩家数
     */
    public int getMinVirtualPlayers() {
        return minVirtualPlayers;
    }

    /**
     * 获取最多虚拟玩家数
     */
    public int getMaxVirtualPlayers() {
        return maxVirtualPlayers;
    }

    /**
     * 获取变化范围
     */
    public int[] getChangeRange() {
        return changeRange;
    }

    /**
     * 获取加入消息
     */
    public String getJoinMessage(String playerName) {
        return translateColors(joinMessageFormat.replace("%player%", playerName));
    }

    /**
     * 获取退出消息
     */
    public String getQuitMessage(String playerName) {
        return translateColors(quitMessageFormat.replace("%player%", playerName));
    }

    /**
     * 是否启用调试模式
     */
    public boolean isDebug() {
        return debug;
    }
}