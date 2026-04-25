# Astraea RPG 代码模板库

> 开发时可直接使用的代码模板
> **版本: 1.4.0 | 更新: 2026-04-24**

---

## ⚠️ 基本原则

1. **不可随意回滚** - 每次提交必须有明确的理由和测试
2. **脚本采用 CMD 执行** - 禁止使用 PowerShell 执行构建脚本
3. **先构建验证后提交** - 禁止未验证就提交

---

## 1. 插件主类模板 (v1.4.0 更新)

```java
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.sound.SoundService;
import cn.guangdian.rpgcore.inject.GuiceSupport;
import javax.inject.Inject;

public class MyPlugin extends AbstractRPGPlugin {

    // RPGCore 自动注入的字段 (无需手动初始化)
    // protected RPGCore rpgCore;
    // protected SyncScheduler scheduler;
    // protected ExternalServiceIntegration externalServices;
    
    // Guice 依赖注入示例
    @Inject
    private MyService myService;

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

## 12. 事件系统模板 (v2.0.0 更新)

> **架构**: Bukkit 事件 + RPGCore EventPublisher 管控层
> 
> **原则**: 底层使用 Bukkit 事件系统，可选使用 EventPublisher 获得管控能力

### 12.1 自定义事件定义（继承 Bukkit Event）

```java
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.Player;

// 自定义事件 - 继承 Bukkit Event
public class PlayerLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final int oldLevel;
    private final int newLevel;
    
    public PlayerLevelUpEvent(Player player, int oldLevel, int newLevel) {
        super();
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }
    
    // Getters
    public Player getPlayer() { return player; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
```

### 12.2 发布事件（推荐：使用 EventPublisher 管控层）

```java
import cn.guangdian.rpgcore.event.EventPublisher;
import org.bukkit.Bukkit;

public class LevelSystem {
    
    public void levelUp(Player player, int newLevel) {
        int oldLevel = getPlayerLevel(player);
        setPlayerLevel(player, newLevel);
        
        // 推荐: 使用 EventPublisher（带管控：监控、限流、日志）
        EventPublisher.publish(new PlayerLevelUpEvent(player, oldLevel, newLevel));
        
        // 异步发布（非关键事件）
        EventPublisher.publishAsync(new PlayerLevelUpEvent(player, oldLevel, newLevel));
        
        // 延迟发布
        EventPublisher.publishLater(new PlayerLevelUpEvent(player, oldLevel, newLevel), 20L);
    }
}
```

### 12.3 发布事件（备选：直接使用 Bukkit）

```java
import org.bukkit.Bukkit;

public class LevelSystem {
    
    public void levelUp(Player player, int newLevel) {
        int oldLevel = getPlayerLevel(player);
        setPlayerLevel(player, newLevel);
        
        // 备选: 直接使用 Bukkit（缺少 RPGCore 管控功能）
        Bukkit.getPluginManager().callEvent(
            new PlayerLevelUpEvent(player, oldLevel, newLevel)
        );
    }
}
```

### 12.4 订阅事件（标准 Bukkit 方式）

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class LevelListener implements Listener {
    
    public LevelListener(JavaPlugin plugin) {
        // 注册监听器
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    // 订阅事件（普通优先级）
    @EventHandler
    public void onPlayerLevelUp(PlayerLevelUpEvent event) {
        Player player = event.getPlayer();
        player.sendMessage("恭喜从 " + event.getOldLevel() + " 级升级到 " + event.getNewLevel() + " 级！");
    }
    
    // 订阅事件（指定优先级）
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLevelUpHighPriority(PlayerLevelUpEvent event) {
        // 高优先级处理（先执行）
    }
    
    // 监听取消事件
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLevelUpMonitor(PlayerLevelUpEvent event) {
        // 仅监听，不修改（最后执行）
    }
}
```

### 12.5 旧代码兼容（EventBus 代理模式）

```java
// 旧代码仍可运行（EventBus 已改为 Bukkit 代理）
RPGCore rpgCore = RPGCore.getInstance();
if (rpgCore != null) {
    EventBus eventBus = rpgCore.getEventBus();
    // 这会代理到 Bukkit 事件系统
    eventBus.publish(new PlayerLevelUpEvent(player, oldLevel, newLevel));
}

// 建议迁移到新 API
EventPublisher.publish(new PlayerLevelUpEvent(player, oldLevel, newLevel));
```

---

## 13. Guice 依赖注入模板 (v1.4.0 新增)

```java
import cn.guangdian.rpgcore.inject.GuiceSupport;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import javax.inject.Singleton;

// 方式1: 简单绑定
public class MyPlugin extends AbstractRPGPlugin {
    @Inject
    private MyService myService;
    
    @Override
    protected void onPluginEnable() {
        initCommonServices();
        
        // 使用 Guice 注入成员
        GuiceSupport.injectMembers(this);
        
        // 现在 myService 已自动注入
        myService.doSomething();
    }
}

// 方式2: 使用子注入器
public class MyPlugin extends AbstractRPGPlugin {
    @Inject
    private MyService myService;
    
    @Override
    protected void onPluginEnable() {
        initCommonServices();
        
        // 创建子注入器并注入
        GuiceSupport.childInjector()
            .with(new MyModule())
            .inject(this);
    }
}

// 方式3: 使用简单绑定（无需创建 Module）
public class MyPlugin extends AbstractRPGPlugin {
    @Override
    protected void onPluginEnable() {
        initCommonServices();
        
        GuiceSupport.createChildInjector(binder -> {
            binder.bind(MyService.class).to(MyServiceImpl.class).in(Singleton.class);
            binder.bind(MyRepository.class).to(MyRepositoryImpl.class).in(Singleton.class);
        }).injectMembers(this);
    }
}

// 自定义 Module
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(MyService.class).to(MyServiceImpl.class).in(Singleton.class);
        bind(MyRepository.class).to(MyRepositoryImpl.class).in(Singleton.class);
    }
}

// 服务接口
public interface MyService {
    void doSomething();
}

// 服务实现
@Singleton
public class MyServiceImpl implements MyService {
    @Inject
    public MyServiceImpl() {
    }
    
    @Override
    public void doSomething() {
        // 实现逻辑
    }
}
```

---

## 14. Configurate 配置管理模板 (v1.4.0 新增)

```java
import cn.guangdian.rpgcore.config.ConfigurateSupport;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

// 1. 定义配置类
@ConfigSerializable
public class DatabaseConfig {
    private String host = "localhost";
    private int port = 3306;
    private String username = "root";
    private String password = "";
    private String database = "minecraft";
    
    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDatabase() { return database; }
}

// 2. 在插件中使用
public class MyPlugin extends AbstractRPGPlugin {
    private ConfigurateSupport<DatabaseConfig> dbConfig;
    
    @Override
    protected void onPluginEnable() {
        initCommonServices();
        
        // 加载配置
        dbConfig = ConfigurateSupport.builder(DatabaseConfig.class)
            .file("database.yml")
            .defaultResource("database-default.yml")  // 可选：默认配置
            .autoSave()  // 启用自动保存
            .build();
        
        // 使用配置
        DatabaseConfig config = dbConfig.get();
        String host = config.getHost();
        int port = config.getPort();
    }
    
    public void updateConfig() {
        // 修改配置
        dbConfig.update(config -> {
            // 修改配置值（通过反射，实际应该提供 setter）
        });
        
        // 手动保存
        dbConfig.save();
    }
}
```

---

## 15. SLF4J 日志模板 (v1.4.0 新增)

```java
import cn.guangdian.rpgcore.logging.LoggerFactory;
import org.slf4j.Logger;

public class MyService {
    // 获取日志记录器
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
    
    public void doSomething() {
        // 不同级别的日志
        logger.trace("跟踪信息");
        logger.debug("调试信息: {}", someData);
        logger.info("普通信息: 玩家 {} 执行了命令 {}", playerName, command);
        logger.warn("警告信息");
        logger.error("错误信息", exception);
        
        // 占位符支持（高性能，仅在需要时计算）
        logger.info("玩家 {} 在位置 ({}, {}, {}) 触发事件", 
            player.getName(), 
            location.getX(), 
            location.getY(), 
            location.getZ());
    }
}
```

---

## 16. 自定义事件定义模板 (v2.0.0 新增)

> **重要**: 业务事件应该定义在对应的业务插件中，而不是 RPGCore。
> 参考: [FORBIDDEN_PATTERNS.md 第17章](./FORBIDDEN_PATTERNS.md)

### 16.1 基础事件模板

```java
package cn.guangdian.myplugin.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 玩家XX事件
 * 
 * <p>当玩家...时触发此事件。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class MyCustomEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final String data;
    private final long timestamp;

    /**
     * 创建事件
     * 
     * @param player 玩家
     * @param data 数据
     */
    public MyCustomEvent(Player player, String data) {
        super(!Bukkit.isPrimaryThread()); // 自动检测异步
        this.player = player;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public Player getPlayer() {
        return player;
    }

    public String getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
```

### 16.2 带枚举的复杂事件模板

```java
package cn.guangdian.myplugin.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 交易事件
 * 
 * <p>当玩家进行交易时触发。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class TransactionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID playerId;
    private final TransactionType type;
    private final double amount;
    private final String reason;

    /**
     * 交易类型
     */
    public enum TransactionType {
        DEPOSIT,    // 存入
        WITHDRAW,   // 取出
        TRANSFER,   // 转账
        SET         // 设置
    }

    public TransactionEvent(UUID playerId, TransactionType type, double amount, String reason) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
    }

    public UUID getPlayerId() { return playerId; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public String getReason() { return reason; }

    public boolean isDeposit() {
        return type == TransactionType.DEPOSIT;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
```

### 16.3 事件发布与订阅

```java
// ============ 发布事件 ============

// 方式1: 使用 EventPublisher (推荐，带管控)
import cn.guangdian.rpgcore.event.EventPublisher;

EventPublisher.publish(new MyCustomEvent(player, "data"));

// 方式2: 直接使用 Bukkit
import org.bukkit.Bukkit;

Bukkit.getPluginManager().callEvent(new MyCustomEvent(player, "data"));

// ============ 订阅事件 ============

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MyListener implements Listener {
    
    // 标准订阅
    @EventHandler
    public void onMyCustomEvent(MyCustomEvent event) {
        Player player = event.getPlayer();
        String data = event.getData();
        // 处理事件
    }
    
    // 指定优先级 (HIGH 先执行)
    @EventHandler(priority = EventPriority.HIGH)
    public void onMyCustomEventHigh(MyCustomEvent event) {
        // 高优先级处理
    }
    
    // 仅监听，不修改 (MONITOR 最后执行)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMyCustomEventMonitor(MyCustomEvent event) {
        // 仅监听，不修改事件状态
    }
}

// 注册监听器
Bukkit.getPluginManager().registerEvents(new MyListener(), plugin);
```

### 16.4 跨插件订阅事件

```java
// 订阅其他插件的事件
@EventHandler
public void onPlayerLevelUp(cn.guangdian.classsystem.event.PlayerLevelUpEvent event) {
    Player player = event.getPlayer();
    int newLevel = event.getNewLevel();
    
    // 处理升级奖励
    if (newLevel % 10 == 0) {
        player.sendMessage("恭喜达到 " + newLevel + " 级！");
    }
}

@EventHandler
public void onPointsTransaction(cn.guangdian.points.event.PointsTransactionEvent event) {
    UUID playerId = event.getPlayerId();
    long amount = event.getAmount();
    
    // 记录大额交易
    if (amount > 10000) {
        logger.info("玩家 {} 进行大额交易: {}", playerId, amount);
    }
}
```

### 16.5 事件设计最佳实践

| 实践 | 说明 | 示例 |
|------|------|------|
| **继承 Event** | 必须继承 Bukkit Event | `public class MyEvent extends Event` |
| **HandlerList** | 必须定义静态 HandlerList | `private static final HandlerList HANDLERS` |
| **构造函数** | 调用 super() 指定异步状态 | `super(!Bukkit.isPrimaryThread())` |
| **不可变性** | 事件字段尽量用 final | `private final Player player` |
| **Getter 方法** | 提供完整 getter | `getPlayer()`, `getAmount()` |
| **文档注释** | 清晰的 Javadoc | `@param`, `@since` |
| **包位置** | 放在插件 event 包下 | `cn.guangdian.myplugin.event` |

---

*最后更新: 2026-04-25*
*版本: 1.6.0*
