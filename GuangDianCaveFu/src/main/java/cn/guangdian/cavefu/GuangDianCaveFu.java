package cn.guangdian.cavefu;

import cn.guangdian.cavefu.adapter.CaveServiceAdapter;
import cn.guangdian.cavefu.cave.CaveManager;
import cn.guangdian.cavefu.command.CaveAdminCommand;
import cn.guangdian.cavefu.command.CaveCommand;
import cn.guangdian.cavefu.config.ConfigManager;
import cn.guangdian.cavefu.hook.RPGItemsHook;
import cn.guangdian.cavefu.listener.PlayerDataListener;
import cn.guangdian.cavefu.placeholder.CavePlaceholder;
import cn.guangdian.cavefu.protection.ProtectionListener;
import cn.guangdian.cavefu.storage.DataManager;
import cn.guangdian.cavefu.upgrade.UpgradeManager;
import cn.guangdian.cavefu.world.CaveWorldManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 光点洞府插件 - GuangDianCaveFu
 * 
 * <p>完全独立插件，不依赖 RPGCore。</p>
 * <p>数据存储：SQLite 数据库（caves.db），参考 GuangDianPoints 的存储架构</p>
 * <p>颜色系统：使用 Adventure MiniMessage（Minecraft 1.21 内置）</p>
 *
 * <p>保存机制：</p>
 * <ul>
 *   <li>操作即时保存：每次洞府操作立即写入 SQLite（事务性）</li>
 *   <li>定时自动保存：每 300 秒全量保存（异步，CompletableFuture）</li>
 *   <li>玩家退出保存：玩家退出时异步全量保存</li>
 *   <li>关闭保存：插件关闭时同步全量保存</li>
 * </ul>
 *
 * @author Gumin
 * @version 2.0.0
 */
public final class GuangDianCaveFu extends JavaPlugin {

    private static GuangDianCaveFu instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private CaveWorldManager worldManager;
    private CaveManager caveManager;
    private UpgradeManager upgradeManager;
    private CavePlaceholder placeholderExpansion;
    private CaveServiceAdapter serviceAdapter;
    private RPGItemsHook rpgItemsHook;

    private MiniMessage miniMessage;

    private int autoSaveInterval;
    private int autoSaveTaskId = -1;

    @Override
    public void onEnable() {
        instance = this;
        this.miniMessage = MiniMessage.miniMessage();

        // 加载配置
        saveDefaultConfig();
        autoSaveInterval = getConfig().getInt("auto-save-interval", 300);

        configManager = new ConfigManager(this);
        configManager.load();

        // 初始化 SQLite 数据库
        dataManager = new DataManager(this);
        if (dataManager.initialize()) {
            dataManager.load();
        } else {
            getLogger().severe("SQLite 初始化失败，插件无法启动！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        worldManager = new CaveWorldManager(this);
        worldManager.init();

        caveManager = new CaveManager(this);
        upgradeManager = new UpgradeManager(this);
        rpgItemsHook = new RPGItemsHook();

        getCommand("cave").setExecutor(new CaveCommand(this));
        getCommand("caveadmin").setExecutor(new CaveAdminCommand(this));

        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new CavePlaceholder(this);
            placeholderExpansion.register();
            getLogger().info("已注册 PlaceholderAPI 扩展: gdcave");
        }

        serviceAdapter = new CaveServiceAdapter(this);

        startAutoSave();

        getLogger().info("GuangDianCaveFu 洞府插件已启用！(SQLite 版本)");
        getLogger().info("存储模式: SQLite | 自动保存间隔: " + autoSaveInterval + "秒");
        getLogger().info("当前洞府数量: " + dataManager.getCaveCount());
    }

    @Override
    public void onDisable() {
        stopAutoSave();

        if (dataManager != null) {
            dataManager.shutdown();
        }

        if (placeholderExpansion != null) {
            PlaceholderAPI.unregisterExpansion(placeholderExpansion);
            placeholderExpansion = null;
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }

        getLogger().info("GuangDianCaveFu 洞府插件已禁用！");
    }

    private void startAutoSave() {
        if (autoSaveInterval <= 0) {
            getLogger().warning("自动保存已禁用（auto-save-interval <= 0）");
            return;
        }
        long ticks = autoSaveInterval * 20L;
        autoSaveTaskId = getServer().getScheduler().runTaskTimerAsynchronously(
            this,
            () -> { if (dataManager != null) dataManager.saveAsync(); },
            ticks, ticks
        ).getTaskId();
        getLogger().info("自动保存已启动：每 " + autoSaveInterval + " 秒");
    }

    private void stopAutoSave() {
        if (autoSaveTaskId != -1) {
            getServer().getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }
    }

    public void saveOnPlayerQuit() {
        if (dataManager != null) dataManager.saveAsync();
    }

    // ==================== 工具 ====================

    public void sendMiniMessage(org.bukkit.command.CommandSender sender, String text) {
        if (text == null || text.isEmpty()) return;
        sender.sendMessage(miniMessage.deserialize(text));
    }

    public MiniMessage getMiniMessage() { return miniMessage; }
    public void logInfo(String m) { getLogger().info(m); }
    public void logWarning(String m) { getLogger().warning(m); }
    public void logSevere(String m) { getLogger().severe(m); }

    public void reloadAll() {
        configManager.reload();
        getLogger().info("配置已重新加载！");
    }

    // ==================== Getter ====================

    public static GuangDianCaveFu getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public CaveWorldManager getWorldManager() { return worldManager; }
    public CaveManager getCaveManager() { return caveManager; }
    public UpgradeManager getUpgradeManager() { return upgradeManager; }
    public RPGItemsHook getRPGItemsHook() { return rpgItemsHook; }
}
