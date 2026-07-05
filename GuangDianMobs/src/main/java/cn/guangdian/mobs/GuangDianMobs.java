package cn.guangdian.mobs;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

/**
 * GuangDianMobs v2.0 — MM 风格<br>
 * 技能格式: type{key=val} @Target ~onTrigger:interval [chance]<br>
 * 掉落格式: command{c="rpgitem give <target.name> 物品 1"} @Trigger ~onDeath 0.1
 */
public class GuangDianMobs extends JavaPlugin {

    private Map<String, MobTemplate> mobTemplates = new LinkedHashMap<>();
    private MobSpawner mobSpawner;
    private MobAIController aiController;
    private SkillEngine skillEngine;
    private SpawnManager spawnManager;
    private MobConfigLoader configLoader;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configLoader = new MobConfigLoader(getLogger());
        mobTemplates = configLoader.loadAll(getDataFolder());

        skillEngine = new SkillEngine(this);
        skillEngine.loadSkills(getDataFolder());
        mobSpawner = new MobSpawner(this);
        aiController = new MobAIController(this);
        aiController.start();

        spawnManager = new SpawnManager(this);
        spawnManager.loadAll(getDataFolder());
        spawnManager.start();

        getServer().getPluginManager().registerEvents(new MobListener(this), this);
        var cmd = getCommand("gdmm");
        if (cmd != null) {
            var exec = new MobCommand(this);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }

        getLogger().info("v2.0 已启动 — " + mobTemplates.size() + " 怪物模板");
    }

    @Override
    public void onDisable() {
        if (aiController != null) aiController.stop();
        if (spawnManager != null) spawnManager.stop();
    }

    public void reload() {
        if (aiController != null) aiController.stop();
        if (spawnManager != null) spawnManager.stop();
        mobTemplates = configLoader.loadAll(getDataFolder());
        aiController = new MobAIController(this);
        aiController.start();
        spawnManager = new SpawnManager(this);
        spawnManager.loadAll(getDataFolder());
        spawnManager.start();
    }

    public Map<String, MobTemplate> getMobTemplates() { return mobTemplates; }
    public MobSpawner getMobSpawner() { return mobSpawner; }
    public MobAIController getAIController() { return aiController; }
    public SkillEngine getSkillEngine() { return skillEngine; }
    public SpawnManager getSpawnManager() { return spawnManager; }
}
