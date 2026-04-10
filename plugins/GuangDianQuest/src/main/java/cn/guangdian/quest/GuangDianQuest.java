package cn.guangdian.quest;

import cn.guangdian.quest.adapter.QuestServiceAdapter;
import cn.guangdian.quest.command.QuestCommand;
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
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        
        hookRPGCore();

        initComponents();

        registerCommands();

        registerListeners();

        registerPlaceholderAPI();

        serviceAdapter = new QuestServiceAdapter(this);

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
        var plugin = Bukkit.getPluginManager().getPlugin("RPGCore");
        if (plugin instanceof RPGCore core) {
            externalServices = core.getExternalServices();
            scheduler = core.getScheduler();
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
        return color(msg);
    }

    public static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
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
    public int getMaxActiveQuests() { return maxActiveQuests; }
    public int getDailyQuestLimit() { return dailyQuestLimit; }
    public int getAutoSaveInterval() { return autoSaveInterval; }
}
