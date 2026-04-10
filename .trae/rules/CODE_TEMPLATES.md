# Astraea RPG 代码模板库

> 开发时可直接使用的代码模板

---

## 1. 插件主类模板

```java
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;

public class MyPlugin extends AbstractRPGPlugin {
    
    protected RPGCore rpgCore;
    protected SyncScheduler scheduler;
    protected ExternalServiceIntegration externalServices;
    
    @Override
    protected void onPluginEnable() {
        getLogger().info(getPluginName() + " 已启动");
    }
    
    @Override
    protected void onPluginDisable() {
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        getLogger().info(getPluginName() + " 已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "MyPlugin";
    }
}
```

---

## 2. 服务适配器模板

```java
public class MyServiceAdapter implements MyService {
    
    private final MyPlugin plugin;
    
    public MyServiceAdapter(MyPlugin plugin) {
        this.plugin = plugin;
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(MyService.class, this);
        }
    }
    
    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(MyService.class);
        }
    }
}
```

---

## 3. 占位符扩展模板

```java
public class MyPlaceholder extends PlaceholderExpansion {
    
    private final MyPlugin plugin;
    
    public MyPlaceholder(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override public String getIdentifier() { return "myplugin"; }
    @Override public String getAuthor() { return "Astraea RPG Team"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        switch (identifier) {
            case "name": return player.getName();
            default: return null;
        }
    }
}

// 注册: new MyPlaceholder(this).register();
// 注销: PlaceholderAPI.unregisterExpansion(myPlaceholderInstance);
```

---

## 4. 数据处理器模板

```java
public class MyDataHandler extends AbstractPlayerDataHandler {
    
    private final MyPlugin plugin;
    private final Map<UUID, PlayerData> dataCache = new ConcurrentHashMap<>();
    
    public MyDataHandler(MyPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        plugin.getScheduler().runAsync(() -> {
            PlayerData data = loadFromDatabase(player.getUniqueId());
            plugin.getScheduler().runSyncLater(() -> {
                dataCache.put(player.getUniqueId(), data);
            }, 0L);
        });
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        PlayerData data = dataCache.remove(player.getUniqueId());
        if (data != null) {
            plugin.getScheduler().runAsync(() -> {
                saveToDatabase(player.getUniqueId(), data);
            });
        }
    }
    
    @Override public int getPriority() { return 100; }
    @Override public String getHandlerName() { return "MyData"; }
}
```

---

## 5. build.gradle 模板

```gradle
plugins {
    id 'java'
}

repositories {
    flatDir { dirs 'libs' }
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT'
    compileOnly project(':plugins:RPGCore')
    compileOnly files('libs/Vault.jar')
}

jar {
    archiveBaseName.set('插件名')
}
```

---

*最后更新: 2026-04-10*
