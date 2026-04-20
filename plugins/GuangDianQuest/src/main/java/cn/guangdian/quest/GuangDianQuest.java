package cn.guangdian.quest;

import cn.guangdian.quest.adapter.QuestServiceAdapter;
import cn.guangdian.quest.command.QuestCommand;
import cn.guangdian.quest.integration.PluginIntegration;
import cn.guangdian.quest.lifecycle.QuestDataHandler;
import cn.guangdian.quest.listener.QuestEventListener;
import cn.guangdian.quest.manager.DailyQuestManager;
import cn.guangdian.quest.manager.QuestLineManager;
import cn.guangdian.quest.manager.QuestManager;
import cn.guangdian.quest.manager.QuestProgressManager;
import cn.guangdian.quest.placeholder.QuestPlaceholder;
import cn.guangdian.quest.repository.PlayerQuestRepository;
import cn.guangdian.quest.repository.QuestRepository;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;

public class GuangDianQuest extends AbstractRPGPlugin {

    private static GuangDianQuest instance;

    private QuestRepository questRepository;
    private PlayerQuestRepository playerRepository;
    private QuestManager questManager;
    private QuestProgressManager progressManager;
    private DailyQuestManager dailyManager;
    private QuestLineManager questLineManager;
    private QuestServiceAdapter serviceAdapter;
    private QuestDataHandler dataHandler;
    private PluginIntegration pluginIntegration;

    // RPGCore 服务引用
    private MiniMessageService miniMessage;
    private final MiniMessage miniMessageParser = MiniMessage.miniMessage();

    private int maxActiveQuests;
    private int dailyQuestLimit;
    private int autoSaveInterval;
    private long autoSaveTaskId = -1;

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

        hookRPGCore();

        initComponents();

        registerCommands();

        registerListeners();

        registerPlaceholderAPI();

        serviceAdapter = new QuestServiceAdapter(this);

        // 初始化插件集成 (GuangDianMobs, RPGItems)
        pluginIntegration = new PluginIntegration(this);

        startAutoSave();

        getLogger().info("GuangDianQuest 已启动！");
        getLogger().info("已加载 " + questManager.getQuestCount() + " 个任务");
    }

    @Override
    protected void onPluginDisable() {
        stopAutoSave();

        if (dataHandler != null) {
            dataHandler.unregister();
        }

        if (playerRepository != null) {
            playerRepository.saveAll();
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        getLogger().info("GuangDianQuest 已停止！");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianQuest";
    }

    private void initRPGCoreServices() {
        // 获取 RPGCore 服务
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                miniMessage = rpgCore.getMiniMessageService();
                getLogger().info("使用 RPGCore MiniMessageService");
            } catch (Exception e) {
                getLogger().warning("无法获取 RPGCore MiniMessageService: " + e.getMessage());
            }
        }

        // 如果 RPGCore 服务不可用，使用本地降级
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
        }
    }

    private void loadConfiguration() {
        maxActiveQuests = getConfig().getInt("max-active-quests", 10);
        dailyQuestLimit = getConfig().getInt("daily-quest-limit", 5);
        autoSaveInterval = getConfig().getInt("auto-save-interval", 300);
    }

    private void initComponents() {
        File questsDir = new File(getDataFolder(), "quests");
        if (!questsDir.exists()) {
            questsDir.mkdirs();
            saveDefaultQuests();
        }

        questRepository = new QuestRepository(this, questsDir);
        questRepository.loadAll();

        File playerDataDir = new File(getDataFolder(), "playerdata");
        playerRepository = new PlayerQuestRepository(this, playerDataDir);

        questManager = new QuestManager(this, questRepository, playerRepository);

        progressManager = new QuestProgressManager(this, questManager, playerRepository);

        dailyManager = new DailyQuestManager(this, questManager);

        questLineManager = new QuestLineManager(this);
        File questLinesFile = new File(getDataFolder(), "questlines.yml");
        if (!questLinesFile.exists()) {
            saveResource("questlines.yml", false);
        }
        questLineManager.loadAll(questLinesFile);
    }

    private void saveDefaultQuests() {
        saveResource("quests/main/example_main.yml", false);
        saveResource("quests/side/example_side.yml", false);
        saveResource("quests/daily/example_daily.yml", false);
    }

    private void registerCommands() {
        QuestCommand questCommand = new QuestCommand(this);
        if (getCommand("quest") != null) {
            getCommand("quest").setExecutor(questCommand);
            getCommand("quest").setTabCompleter(questCommand);
        }
    }

    private QuestEventListener questEventListener;

    private void registerListeners() {
        questEventListener = new QuestEventListener(this);
        Bukkit.getPluginManager().registerEvents(questEventListener, this);
        subscribeRPGCoreEvents();
        
        // 注册玩家生命周期处理器
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            dataHandler = new QuestDataHandler(this);
            dataHandler.register();
            getLogger().info("已注册到 RPGCore PlayerLifecycleManager");
        }
    }

    private void subscribeRPGCoreEvents() {
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                cn.guangdian.rpgcore.api.EventBus eventBus = rpgCore.getEventBus();
                if (eventBus != null) {
                    eventBus.subscribe(cn.guangdian.rpgcore.event.events.NPCInteractEvent.class, event -> {
                        if (event instanceof cn.guangdian.rpgcore.event.events.NPCInteractEvent npcEvent) {
                            org.bukkit.entity.Player player = npcEvent.getPlayer();
                            String npcId = npcEvent.getNpcId();
                            if (player != null && questEventListener != null) {
                                questEventListener.onNPCInteract(npcId, player);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            getLogger().warning("订阅RPGCore事件失败: " + e.getMessage());
        }
    }

    private void registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new QuestPlaceholder(this).register();
            getLogger().info("已注册 PlaceholderAPI 扩展");
        }
    }

    private void hookRPGCore() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            externalServices = rpgCore.getExternalServices();
            scheduler = rpgCore.getScheduler();
            getLogger().info("已连接到 RPGCore: " + (externalServices != null ? externalServices.getExternalServiceStatus() : "服务不可用"));
        }
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

    public void savePlayerOnQuit(Player player) {
        if (playerRepository != null) {
            playerRepository.savePlayerData(player.getUniqueId());
        }
    }

    public void reloadConfigs() {
        reloadConfig();
        loadConfiguration();
        questRepository.loadAll();
        questLineManager.loadAll(new File(getDataFolder(), "questlines.yml"));
        stopAutoSave();
        startAutoSave();
        getLogger().info("配置已重载！");
    }

    public String getMessage(String key, String... replacements) {
        String msg = getConfig().getString("messages." + key, "");
        if (msg.isEmpty()) return "";
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return colorToString(msg);
    }

    /**
     * 使用 MiniMessage 解析颜色代码并返回 String（用于兼容旧代码）
     */
    public static String colorToString(String text) {
        if (text == null) return "";
        // 将 & 颜色代码转换为 MiniMessage 格式
        String miniMessageText = text
            .replace("<black>", "<black>").replace("<dark_blue>", "<dark_blue>")
            .replace("<dark_green>", "<dark_green>").replace("<dark_aqua>", "<dark_aqua>")
            .replace("<dark_red>", "<dark_red>").replace("<dark_purple>", "<dark_purple>")
            .replace("<gold>", "<gold>").replace("<gray>", "<gray>")
            .replace("<dark_gray>", "<dark_gray>").replace("<blue>", "<blue>")
            .replace("<green>", "<green>").replace("<aqua>", "<aqua>")
            .replace("<red>", "<red>").replace("<light_purple>", "<light_purple>")
            .replace("<yellow>", "<yellow>").replace("<white>", "<white>")
            .replace("<obfuscated>", "<obfuscated>").replace("<bold>", "<bold>")
            .replace("<strikethrough>", "<strikethrough>").replace("<underlined>", "<underlined>")
            .replace("<italic>", "<italic>").replace("<reset>", "<reset>");
        return miniMessageText;
    }

    /**
     * 使用 MiniMessage 解析颜色代码并返回 Component（静态方法，用于兼容旧代码）
     */
    public static Component color(String text) {
        if (text == null) return Component.empty();
        String miniMessageText = colorToString(text);
        return MiniMessage.miniMessage().deserialize(miniMessageText);
    }

    /**
     * 获取 MiniMessageService
     * @return MiniMessageService 实例（可能为本地降级实现）
     */
    public MiniMessageService getMiniMessageService() {
        return miniMessage;
    }

    public static GuangDianQuest getInstance() { return instance; }

    public QuestRepository getQuestRepository() { return questRepository; }
    public PlayerQuestRepository getPlayerRepository() { return playerRepository; }
    public QuestManager getQuestManager() { return questManager; }
    public QuestProgressManager getProgressManager() { return progressManager; }
    public DailyQuestManager getDailyManager() { return dailyManager; }
    public QuestLineManager getQuestLineManager() { return questLineManager; }
    public QuestServiceAdapter getServiceAdapter() { return serviceAdapter; }
    public ExternalServiceIntegration getExternalServices() { return externalServices; }
    public PluginIntegration getPluginIntegration() { return pluginIntegration; }
    public int getMaxActiveQuests() { return maxActiveQuests; }
    public int getDailyQuestLimit() { return dailyQuestLimit; }
    public int getAutoSaveInterval() { return autoSaveInterval; }
}
