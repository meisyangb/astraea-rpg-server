# Astraea RPG 代码模板库

> 开发时可直接使用的代码模板
> **版本: 1.1.0 | 更新: 2026-04-14**

---

## ⚠️ 基本原则

1. **不可随意回滚** - 每次提交必须有明确的理由和测试
2. **脚本采用 CMD 执行** - 禁止使用 PowerShell 执行构建脚本
3. **先构建验证后提交** - 禁止未验证就提交

---

## 1. 插件主类模板

```java
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;

public class MyPlugin extends AbstractRPGPlugin {

    protected RPGCore rpgCore;
    protected SyncScheduler scheduler;
    protected ExternalServiceIntegration externalServices;
    protected MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        rpgCore = RPGCore.getInstance();
        scheduler = rpgCore.getScheduler();
        externalServices = rpgCore.getExternalServices();
        miniMessage = MiniMessageService.getInstance();
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
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

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
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return "";

        switch (identifier) {
            case "name": return player.getName();
            case "level": return String.valueOf(getPlayerLevel(player));
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
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MyDataHandler extends AbstractPlayerDataHandler {

    private final MyPlugin plugin;
    private final Map<UUID, PlayerData> dataCache = new ConcurrentHashMap<>();

    public MyDataHandler(MyPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    protected void onPlayerLoad(Player player) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return;

        rpgCore.getScheduler().runAsync(() -> {
            PlayerData data = loadFromDatabase(player.getUniqueId());
            rpgCore.getScheduler().runSyncLater(() -> {
                dataCache.put(player.getUniqueId(), data);
            }, 0L);
        });
    }

    @Override
    protected void onPlayerSave(Player player) {
        PlayerData data = dataCache.remove(player.getUniqueId());
        if (data != null) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().runAsync(() -> {
                    saveToDatabase(player.getUniqueId(), data);
                });
            }
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
    mavenCentral()
    maven { url = 'https://repo.papermc.io/repository/maven-public/' }
    flatDir { dirs 'libs' }
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT'
    compileOnly project(':plugins:RPGCore')
    compileOnly name: 'Vault'
}

jar {
    archiveBaseName.set('插件名')
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

---

## 6. 异步任务模板 (Paper 1.21.6 AsyncScheduler)

```java
// 异步立即任务
Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> {
    // 异步执行的任务
    Object result = doAsyncWork();
    Bukkit.getAsyncScheduler().runNow(plugin, task2 -> {
        // 回调到主线程 (如果需要)
    });
});

// 异步延迟任务 (5秒)
Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> {
    // 延迟执行
}, 5000, java.util.concurrent.TimeUnit.MILLISECONDS);

// 异步定时任务 (每分钟)
Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> {
    // 定期执行
}, 0, 60000, java.util.concurrent.TimeUnit.MILLISECONDS);
```

---

## 7. MiniMessage 消息模板

```java
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;

// 获取服务
MiniMessageService mm = MiniMessageService.getInstance();

// 简单颜色
player.sendMessage(mm.red("错误消息"));
player.sendMessage(mm.green("成功消息"));
player.sendMessage(mm.yellow("警告消息"));
player.sendMessage(mm.aqua("信息消息"));

// 复杂消息
player.sendMessage(mm.colorize("<yellow>玩家 <white>" + player.getName() + " <green>已上线"));

// 带占位符的消息
String message = mm.colorize("<hover:show_text:'<yellow>点击查看'><click:run_command:'/info'><green>点击这里</click></hover></green>");
player.sendMessage(mm.colorize(message));

// ActionBar
player.sendActionBar(mm.green("这是一条 ActionBar 消息"));
```

---

## 8. 全息显示模板 (TextDisplay)

```java
import cn.guangdian.rpgcore.service.api.TextDisplayService;
import cn.guangdian.rpgcore.RPGCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

// 获取服务
RPGCore rpgCore = RPGCore.getInstance();
TextDisplayService textDisplayService = rpgCore.getTextDisplayService();

// 创建全息图
Location location = player.getLocation().add(0, 2, 0);
textDisplayService.createHologram("myHologram_" + player.getUniqueId(), location,
    mm.green("玩家: " + player.getName()));

// 显示给玩家
textDisplayService.showHologramToPlayer("myHologram_" + player.getUniqueId(), player);

// 更新全息图
textDisplayService.updateHologram("myHologram_" + player.getUniqueId(),
    mm.red("新消息!"));

// 移除全息图
textDisplayService.removeHologram("myHologram_" + player.getUniqueId());
```

---

## 9. 缓存使用模板

```java
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.RPGCore;
import com.github.benmanes.caffeine.cache.LoadingCache;

RPGCore rpgCore = RPGCore.getInstance();
CacheProvider cacheProvider = rpgCore.getCacheProvider();

// 获取或创建缓存
LoadingCache<String, PlayerData> playerDataCache = cacheProvider.getLoadingCache(
    "playerData",
    uuid -> loadPlayerDataFromDatabase(uuid)
);

// 使用缓存
String uuid = player.getUniqueId().toString();
PlayerData data = playerDataCache.get(uuid);

// 手动失效
playerDataCache.invalidate(uuid);

// 清空所有缓存
cacheProvider.clear();
```

---

*最后更新: 2026-04-14*
