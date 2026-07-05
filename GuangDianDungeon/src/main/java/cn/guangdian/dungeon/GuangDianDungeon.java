package cn.guangdian.dungeon;

import cn.guangdian.dungeon.adapter.DungeonServiceAdapter;
import cn.guangdian.dungeon.command.DungeonAdminCommand;
import cn.guangdian.dungeon.command.DungeonCommand;
import cn.guangdian.dungeon.command.PartyCommand;
import cn.guangdian.dungeon.integration.MobBridge;
import cn.guangdian.dungeon.integration.SlimeWorldBridge;
import cn.guangdian.dungeon.listener.DungeonEventListener;
import cn.guangdian.dungeon.manager.*;
import cn.guangdian.dungeon.model.DungeonTemplate;
import cn.guangdian.dungeon.placeholder.DungeonPlaceholder;
import cn.guangdian.dungeon.repository.DungeonRepository;
import cn.guangdian.dungeon.repository.PlayerDungeonRepository;
import cn.guangdian.dungeon.storage.DungeonStorage;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;

public class GuangDianDungeon extends AbstractRPGPlugin {

    private static GuangDianDungeon instance;

    private DungeonRepository dungeonRepository;
    private PlayerDungeonRepository playerRepository;
    private DungeonStorage dungeonStorage;
    private PartyManager partyManager;
    private SessionRewardManager rewardManager;
    private TemplateLoader templateLoader;
    private DungeonServiceAdapter serviceAdapter;
    private SlimeWorldBridge slimeWorldBridge;
    private MobBridge mobBridge;
    private SessionManager sessionManager;
    private StageLoader stageLoader;
    private WorldInstanceManager worldInstanceManager;

    private MiniMessageService miniMessage;
    private final MiniMessage miniMessageParser = MiniMessage.miniMessage();

    private int maxInstances;
    private int instanceTimeout;
    private int cleanupInterval;
    private int autoSaveInterval;
    private long autoSaveTaskId = -1;
    private long cleanupTaskId = -1;

    @Override
    protected void onPluginEnable() {
        instance = this;

        if (!Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            getLogger().severe("缺少RPGCore依赖，无法启动！");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        loadConfiguration();

        initRPGCoreServices();

        initComponents();

        registerCommands();

        registerListeners();

        serviceAdapter = new DungeonServiceAdapter(this);

        startAutoSave();

        startCleanup();

        // 清理孤儿世界（服务端崩溃/异常关闭时残留的实例世界）
        cleanupOrphanInstances();

        getLogger().info("GuangDianDungeon 已启动！");
        getLogger().info("已加载 " + templateLoader.getTemplateCount() + " 个副本模板");
    }

    @Override
    protected void onPluginDisable() {
        stopAutoSave();
        stopCleanup();

        if (slimeWorldBridge != null) {
            slimeWorldBridge.cleanupAll();
        }

        if (worldInstanceManager != null) {
            worldInstanceManager.cleanupAllInstances();
        }

        if (partyManager != null) {
            partyManager.cleanupAll();
        }

        if (playerRepository != null) {
            playerRepository.saveAll();
        }
        
        if (dungeonStorage != null) {
            dungeonStorage.save();
            dungeonStorage.close();
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        cancelAllTasks();

        getLogger().info("GuangDianDungeon 已停止！");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianDungeon";
    }

    private void initRPGCoreServices() {
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                miniMessage = rpgCore.getMiniMessageService();
                getLogger().info("使用 RPGCore MiniMessageService");
            } catch (Exception e) {
                getLogger().warning("无法获取 RPGCore MiniMessageService: " + e.getMessage());
            }
        }

        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
        }
    }

    private void loadConfiguration() {
        maxInstances = getConfig().getInt("global.max-instances", 50);
        instanceTimeout = getConfig().getInt("global.instance-timeout", 3600);
        cleanupInterval = getConfig().getInt("global.cleanup-interval", 300);
        autoSaveInterval = getConfig().getInt("auto-save-interval", 300);
    }

    private void initComponents() {
        File dungeonsDir = new File(getDataFolder(), "dungeons");
        if (!dungeonsDir.exists()) {
            dungeonsDir.mkdirs();
            saveDefaultDungeons();
        }

        templateLoader = new TemplateLoader(this, dungeonsDir);
        templateLoader.loadAll();

        dungeonRepository = new DungeonRepository(this, dungeonsDir);

        File playerDataDir = new File(getDataFolder(), "playerdata");
        playerRepository = new PlayerDungeonRepository(this, playerDataDir);
        
        dungeonStorage = new DungeonStorage(this);
        if (dungeonStorage.init()) dungeonStorage.load();

        mobBridge = new MobBridge(this);

        sessionManager = new SessionManager(this);
        sessionManager.setMobBridge(mobBridge);

        slimeWorldBridge = new SlimeWorldBridge(this);

        partyManager = new PartyManager(this);

        rewardManager = new SessionRewardManager(this);

        stageLoader = new StageLoader(this);

        worldInstanceManager = new WorldInstanceManager(this);
    }

    private void saveDefaultDungeons() {
        saveResource("dungeons/example_dungeon.yml", false);
    }

    private void registerCommands() {
        DungeonCommand dungeonCommand = new DungeonCommand(this);
        if (getCommand("dungeon") != null) {
            getCommand("dungeon").setExecutor(dungeonCommand);
            getCommand("dungeon").setTabCompleter(dungeonCommand);
        }

        PartyCommand partyCommand = new PartyCommand(this);
        if (getCommand("party") != null) {
            getCommand("party").setExecutor(partyCommand);
            getCommand("party").setTabCompleter(partyCommand);
        }

        DungeonAdminCommand adminCommand = new DungeonAdminCommand(this);
        if (getCommand("dungeonadmin") != null) {
            getCommand("dungeonadmin").setExecutor(adminCommand);
            getCommand("dungeonadmin").setTabCompleter(adminCommand);
        }
    }

    private void registerListeners() {
        DungeonEventListener listener = new DungeonEventListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DungeonPlaceholder(this).register();
            getLogger().info("已注册 PlaceholderAPI 扩展");
        }
    }

    /**
     * 清理服务端崩溃/异常关闭时残留的副本实例世界
     */
    private void cleanupOrphanInstances() {
        String prefix = getConfig().getString("world.instance-prefix", "dungeon_inst_");
        File worldContainer = Bukkit.getWorldContainer();
        File[] worldDirs = worldContainer.listFiles(File::isDirectory);
        if (worldDirs == null) return;

        int cleaned = 0;
        for (File dir : worldDirs) {
            String name = dir.getName();
            if (name.startsWith(prefix) && Bukkit.getWorld(name) == null) {
                try {
                    deleteRecursive(dir);
                    cleaned++;
                    getLogger().info("清理残留世界: " + name);
                } catch (Exception e) {
                    getLogger().warning("清理残留世界失败: " + name + " - " + e.getMessage());
                }
            }
        }
        if (cleaned > 0) {
            getLogger().info("共清理 " + cleaned + " 个残留副本世界");
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    private void startAutoSave() {
        if (autoSaveInterval <= 0 || scheduler == null) return;
        long ticks = autoSaveInterval * 20L;
        autoSaveTaskId = scheduler.runAsyncRepeating(() -> {
            if (playerRepository != null) {
                playerRepository.saveAll();
                getLogger().info("自动保存玩家数据完成");
            }
        }, ticks, ticks);
    }

    private void stopAutoSave() {
        if (scheduler != null && autoSaveTaskId != -1) {
            scheduler.cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }
    }

    private void startCleanup() {
        if (cleanupInterval <= 0 || scheduler == null) return;
        long ticks = cleanupInterval * 20L;
        cleanupTaskId = scheduler.runSyncRepeating(() -> {
            int cleaned = sessionManager.cleanupExpired(instanceTimeout * 1000L);
            if (cleaned > 0) {
                getLogger().info("清理了 " + cleaned + " 个过期副本会话");
            }
        }, ticks, ticks);
    }

    private void stopCleanup() {
        if (scheduler != null && cleanupTaskId != -1) {
            scheduler.cancelTask(cleanupTaskId);
            cleanupTaskId = -1;
        }
    }

    public void reloadConfigs() {
        reloadConfig();
        loadConfiguration();
        templateLoader.loadAll();
        stopAutoSave();
        startAutoSave();
        stopCleanup();
        startCleanup();
        getLogger().info("配置已重载！");
    }

    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "");
    }

    public Component color(String text) {
        if (text == null) return Component.empty();
        return miniMessageParser.deserialize(text);
    }

    public void sendMessage(Player player, String key) {
        String msg = getMessage(key);
        if (!msg.isEmpty()) {
            player.sendMessage(color(msg));
        }
    }

    public void sendMessage(Player player, String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        if (!msg.isEmpty()) {
            player.sendMessage(color(msg));
        }
    }

    public void sendMessage(Player player, net.kyori.adventure.text.Component component) {
        player.sendMessage(component);
    }

    public void sendMessageDirect(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(color(message));
    }

    public static GuangDianDungeon getInstance() { return instance; }

    public DungeonRepository getDungeonRepository() { return dungeonRepository; }
    public PlayerDungeonRepository getPlayerRepository() { return playerRepository; }
    public PartyManager getPartyManager() { return partyManager; }
    public SessionRewardManager getRewardManager() { return rewardManager; }
    public TemplateLoader getTemplateLoader() { return templateLoader; }
    public DungeonServiceAdapter getServiceAdapter() { return serviceAdapter; }
    public SlimeWorldBridge getSlimeWorldBridge() { return slimeWorldBridge; }
    public SessionManager getSessionManager() { return sessionManager; }
    public StageLoader getStageLoader() { return stageLoader; }
    public WorldInstanceManager getWorldInstanceManager() { return worldInstanceManager; }
    public MobBridge getMobBridge() { return mobBridge; }
    public MiniMessageService getMiniMessageService() { return miniMessage; }
    public ExternalServiceIntegration getExternalServices() { return externalServices; }
    public int getMaxInstances() { return maxInstances; }
    public int getInstanceTimeout() { return instanceTimeout; }
}
