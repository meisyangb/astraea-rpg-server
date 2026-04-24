# Astraea RPG 代码模板库

> 开发时可直接使用的代码模板
> **版本: 1.3.0 | 更新: 2026-04-23**

---

## ⚠️ 基本原则

1. **不可随意回滚** - 每次提交必须有明确的理由和测试
2. **脚本采用 CMD 执行** - 禁止使用 PowerShell 执行构建脚本
3. **先构建验证后提交** - 禁止未验证就提交

---

## 1. 插件主类模板 (v1.3.0 更新)

```java
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.sound.SoundService;

public class MyPlugin extends AbstractRPGPlugin {

    // RPGCore 自动注入的字段 (无需手动初始化)
    // protected RPGCore rpgCore;
    // protected SyncScheduler scheduler;
    // protected ExternalServiceIntegration externalServices;

    @Override
    protected void onPluginEnable() {
        // 必须调用 - 初始化通用服务
        initCommonServices();
        
        // 使用 RPGCore 服务
        ServiceRegistry registry = rpgCore.getServiceRegistry();
        
        getLogger().info(getPluginName() + " 已启动");
    }

    @Override
    protected void onPluginDisable() {
        // 确保取消所有任务
        cancelAllTasks();
        
        // 清理资源...
        
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

## 4. 数据处理器模板 (v1.3.0 更新)

```java
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class MyDataHandler extends AbstractPlayerDataHandler {

    private final Map<UUID, PlayerData> dataCache = new ConcurrentHashMap<>();

    public MyDataHandler(JavaPlugin plugin) {
        super(plugin);
        // rpgCore 已由父类初始化
    }.trae/rules/
├── kaifa.md                           #

    @Override
    protected void onPlayerLoad(Player player) {
        // 此方法在异步线程中调用
        UUID uuid = player.getUniqueId();
        try {
            // 从数据库加载数据
            PlayerData data = loadFromDatabase(uuid);
            if (data != null) {
                dataCache.put(uuid, data);
            }
        } catch (Exception e) {
            // 加载失败处理
            plugin.getLogger().warning("加载 " + player.getName() + " 数据失败: " + e.getMessage());
        }
    }

    @Override
    protected void onPlayerSave(Player player) {
        // 此方法在异步线程中调用
        UUID uuid = player.getUniqueId();
        PlayerData data = dataCache.get(uuid);
        if (data != null) {
            try {
                saveToDatabase(uuid, data);
                dataCache.remove(uuid); // 清理缓存
            } catch (Exception e) {
                plugin.getLogger().warning("保存 " + player.getName() + " 数据失败: " + e.getMessage());
            }
        }
    }

    @Override
    public int getPriority() { 
        return 100; // 数字越大优先级越高
    }
    
    @Override
    public String getHandlerName() { 
        return "MyData"; 
    }
    
    @Override
    public boolean shouldLoad(Player player) {
        // 可选：根据条件决定是否加载
        return player.hasPermission("myplugin.use");
    }
    
    @Override
    public boolean shouldSave(Player player) {
        // 可选：根据条件决定是否保存
        return dataCache.containsKey(player.getUniqueId());
    }
}

// 注册处理器 (在插件 onPluginEnable 中)
// new MyDataHandler(this).register();

// 注销处理器 (在插件 onPluginDisable 中)
// new MyDataHandler(this).unregister();
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

## 10. RPGModule 业务模块模板 (v1.3.0 新增)

```java
import cn.guangdian.rpgcore.module.RPGModule;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.plugin.java.JavaPlugin;

public class MyModule extends RPGModule {

    private final JavaPlugin plugin;
    private MiniMessageService miniMessage;

    public MyModule(JavaPlugin plugin) {
        super("MyModule"); // 模块名称
        this.plugin = plugin;
    }

    @Override
    protected void load() {
        // 加载阶段：初始化配置、注册服务等
        // 此阶段插件还未启用，适合轻量初始化
        plugin.getLogger().info("[MyModule] 加载配置...");
    }

    @Override
    protected void enable() {
        // 启用阶段：注册命令、监听器、启动定时任务等
        miniMessage = MiniMessageService.getInstance();
        
        // 注册服务到 RPGCore ServiceRegistry
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            registry.registerService(MyModuleService.class, new MyModuleServiceImpl());
        }
        
        // 注册事件监听器
        // Bukkit.getPluginManager().registerEvents(new MyListener(), plugin);
        
        plugin.getLogger().info("[MyModule] 模块已启用");
    }

    @Override
    protected void disable() {
        // 禁用阶段：取消任务、保存数据、清理资源
        plugin.getLogger().info("[MyModule] 模块已禁用");
    }

    @Override
    protected void destroy() {
        // 销毁阶段：释放数据库连接、关闭文件句柄等
        // 此方法在模块禁用后调用，用于彻底清理
    }
    
    public boolean isMiniMessageAvailable() {
        return miniMessage != null;
    }
}

// 模块服务接口
interface MyModuleService {
    void doSomething();
}

// 模块服务实现
class MyModuleServiceImpl implements MyModuleService {
    @Override
    public void doSomething() {
        // 实现逻辑
    }
}
```

---

## 11. 并发安全模板 (v1.3.0 新增)

```java
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.concurrency.PlayerLockManager;
import cn.guangdian.rpgcore.concurrency.LockTimeoutException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SafeDataManager {
    
    private final PlayerLockManager lockManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    
    public SafeDataManager() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            throw new IllegalStateException("RPGCore 未初始化");
        }
        this.lockManager = rpgCore.getLockManager();
    }
    
    // 单玩家数据修改
    public void addPoints(UUID playerUUID, int amount) {
        try {
            lockManager.executeWithLock(playerUUID, () -> {
                PlayerData data = cache.computeIfAbsent(playerUUID, 
                    uuid -> new PlayerData(uuid));
                data.addPoints(amount);
            });
        } catch (LockTimeoutException e) {
            // 锁超时处理
            plugin.getLogger().warning("获取玩家 " + playerUUID + " 锁超时");
        }
    }
    
    // 多玩家数据修改 (转账、交易等)
    public boolean transferPoints(UUID from, UUID to, int amount) {
        try {
            return lockManager.executeWithDualLock(from, to, () -> {
                PlayerData fromData = cache.get(from);
                PlayerData toData = cache.get(to);
                
                if (fromData == null || toData == null) {
                    return false;
                }
                
                if (!fromData.hasPoints(amount)) {
                    return false;
                }
                
                fromData.removePoints(amount);
                toData.addPoints(amount);
                return true;
            });
        } catch (LockTimeoutException e) {
            plugin.getLogger().warning("转账操作锁超时");
            return false;
        }
    }
    
    // 带返回值的数据读取
    public int getPoints(UUID playerUUID) {
        PlayerData data = cache.get(playerUUID);
        return data != null ? data.getPoints() : 0;
    }
}
```

---

## 12. 事件总线使用模板 (v1.3.0 新增)

```java
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.event.EventHandler;
import cn.guangdian.rpgcore.event.EventPriority;
import cn.guangdian.rpgcore.event.CoreEvent;

// 自定义事件
public class MyCustomEvent extends CoreEvent {
    private final UUID playerId;
    private final String action;
    
    public MyCustomEvent(UUID playerId, String action) {
        super();
        this.playerId = playerId;
        this.action = action;
    }
    
    public UUID getPlayerId() { return playerId; }
    public String getAction() { return action; }
}

// 事件发布者
public class EventPublisher {
    
    private final EventBus eventBus;
    
    public EventPublisher() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            eventBus = rpgCore.getEventBus();
        }
    }
    
    public void publishEvent(UUID playerId, String action) {
        if (eventBus != null) {
            eventBus.publish(new MyCustomEvent(playerId, action));
        }
    }
}

// 事件订阅者
public class EventSubscriber {
    
    private final EventBus eventBus;
    
    public EventSubscriber() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            eventBus = rpgCore.getEventBus();
            registerHandlers();
        }
    }
    
    private void registerHandlers() {
        // 注册事件处理器
        eventBus.registerHandler(MyCustomEvent.class, new EventHandler<>() {
            @Override
            public EventPriority getPriority() {
                return EventPriority.NORMAL;
            }
            
            @Override
            public void handle(MyCustomEvent event) {
                // 处理事件逻辑
                System.out.println("收到事件: " + event.getPlayerId() + " - " + event.getAction());
            }
        });
    }
    
    public void unregister() {
        if (eventBus != null) {
            eventBus.unregisterHandler(MyCustomEvent.class, handler);
        }
    }
}
```

---

*最后更新: 2026-04-23*
