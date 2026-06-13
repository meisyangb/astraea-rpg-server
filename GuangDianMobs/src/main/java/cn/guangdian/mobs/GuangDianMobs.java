package cn.guangdian.mobs;

import cn.guangdian.mobs.aggro.api.AggroService;
import cn.guangdian.mobs.aggro.adapter.AggroServiceAdapter;
import cn.guangdian.mobs.aggro.hook.MythicMobsHook;
import cn.guangdian.mobs.aggro.listener.AggroListener;
import cn.guangdian.mobs.aggro.manager.AggroManager;
import cn.guangdian.mobs.aggro.placeholder.AggroPlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.mobs.manager.MobManager;
import cn.guangdian.mobs.manager.SkillManager;
import cn.guangdian.mobs.manager.SpawnManager;
import cn.guangdian.mobs.manager.DropManager;
import cn.guangdian.mobs.manager.BossBarManager;
import cn.guangdian.mobs.manager.SpawnPointManager;
import cn.guangdian.mobs.ai.MobAIController;
import cn.guangdian.mobs.listener.MobListener;
import cn.guangdian.mobs.command.MobCommand;
import cn.guangdian.mobs.command.SpawnPointCommand;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * GuangDianMobs - 光点怪物插件
 * 
 * 职责：自定义怪物系统 + 仇恨系统
 * - 自定义怪物属性（血量、伤害、防御等）
 * - 怪物技能系统
 * - 怪物生成控制
 * - 掉落表管理
 * - 仇恨管理系统（已合并 GuangDianAggro）
 * 
 * 设计原则：
 * 1. 职责单一：只管理怪物，不管理物品（由RPGItems管理）
 * 2. 数据独立：怪物配置独立存储
 * 3. 可扩展：支持技能扩展
 * 
 * RPGCore 集成：
 * - MiniMessageService: 消息格式化
 * - SyncScheduler: 任务调度
 * - ServiceRegistry: 注册 MobService 供其他插件使用
 */
public class GuangDianMobs extends AbstractRPGPlugin {

    private static GuangDianMobs instance;
    private MobManager mobManager;
    private SkillManager skillManager;
    private SpawnManager spawnManager;
    private DropManager dropManager;
    private BossBarManager bossBarManager;
    private SpawnPointManager spawnPointManager;
    private MobAIController aiController;
    
    // Aggro 系统（已合并）
    private MythicMobsHook mythicMobsHook;
    private AggroManager aggroManager;
    private AggroServiceAdapter aggroServiceAdapter;
    private AggroPlaceholder aggroPlaceholder;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 检查 RPGCore 是否可用
        if (rpgCore == null) {
            getLogger().severe("RPGCore 未加载，GuangDianMobs 无法启动");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 保存默认配置
        saveDefaultConfig();
        saveResource("mobs.yml", false);
        saveResource("skills.yml", false);
        saveResource("drops.yml", false);
        saveResource("spawns.yml", false);

        // 初始化 MythicMobs Hook
        mythicMobsHook = new MythicMobsHook();
        mythicMobsHook.init();

        // 初始化仇恨系统
        aggroManager = new AggroManager(this, mythicMobsHook);
        aggroManager.loadConfig();

        // 注册仇恨监听器
        getServer().getPluginManager().registerEvents(new AggroListener(this, aggroManager), this);

        // 注册仇恨服务
        aggroServiceAdapter = new AggroServiceAdapter(this, aggroManager);

        // 注册仇恨占位符
        if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
            aggroPlaceholder = new AggroPlaceholder(this, aggroManager);
            aggroPlaceholder.register();
        }

        // 初始化管理器
        mobManager = new MobManager(this);
        skillManager = new SkillManager(this);
        spawnManager = new SpawnManager(this);
        dropManager = new DropManager(this);
        bossBarManager = new BossBarManager(this);
        spawnPointManager = new SpawnPointManager(this);
        aiController = new MobAIController(this);

        // 加载数据
        mobManager.loadMobs();
        skillManager.loadSkills();
        dropManager.loadDrops();
        spawnManager.loadSpawns();

        // 注册监听器
        getServer().getPluginManager().registerEvents(new MobListener(this), this);

        // 注册命令
        if (getCommand("gdmm") != null) {
            getCommand("gdmm").setExecutor(new MobCommand(this));
        }
        if (getCommand("gdmsp") != null) {
            getCommand("gdmsp").setExecutor(new SpawnPointCommand(this));
        }

        // 注册服务
        rpgCore.getServiceRegistry().registerService(MobManager.class, mobManager);

        getLogger().info("GuangDianMobs 已启动!");
        getLogger().info("加载了 " + mobManager.getMobCount() + " 个怪物配置");
        getLogger().info("加载了 " + skillManager.getSkillCount() + " 个技能");
        getLogger().info("仇恨系统: " + (aggroManager.isEnabled() ? "已启用" : "已禁用"));
        getLogger().info("MythicMobs 集成: " + (mythicMobsHook.isEnabled() ? "已启用" : "未启用"));
    }

    @Override
    protected void onPluginDisable() {
        // 注销仇恨占位符
        if (aggroPlaceholder != null) {
            PlaceholderAPI.unregisterExpansion(aggroPlaceholder);
            aggroPlaceholder = null;
        }

        // 注销仇恨服务
        if (aggroServiceAdapter != null) {
            aggroServiceAdapter.unregister();
            aggroServiceAdapter = null;
        }

        // 停止仇恨衰减任务
        if (aggroManager != null) {
            aggroManager.stopDecayTask();
            aggroManager.clearAll();
        }

        // 清理Boss血条
        if (bossBarManager != null) {
            bossBarManager.cleanup();
        }

        // 清理刷新点
        if (spawnPointManager != null) {
            spawnPointManager.cleanup();
        }

        // 清理AI控制器
        if (aiController != null) {
            aiController.cleanup();
        }

        // 注销服务
        if (rpgCore != null) {
            try {
                rpgCore.getServiceRegistry().unregisterService(MobManager.class);
            } catch (Exception e) {
                getLogger().warning("注销服务时出错: " + e.getMessage());
            }
        }

        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        getLogger().info("GuangDianMobs 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianMobs";
    }

    public static GuangDianMobs getInstance() {
        return instance;
    }

    public MobManager getMobManager() { return mobManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public DropManager getDropManager() { return dropManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public SpawnPointManager getSpawnPointManager() { return spawnPointManager; }
    public MobAIController getAIController() { return aiController; }
    
    // Aggro 系统访问方法
    public AggroManager getAggroManager() { return aggroManager; }
    public AggroService getAggroService() { return aggroManager; }
    public MythicMobsHook getMythicMobsHook() { return mythicMobsHook; }
}
