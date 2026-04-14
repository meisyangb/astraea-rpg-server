# RPGCore 核心框架详细文档

## 1. 框架概述

**RPGCore** 是 Astraea RPG 服务器的核心框架插件，为所有 GuangDian\* 系列业务插件提供统一的基础设施服务。

### 1.1 核心定位

| 特性       | 说明                             |
| -------- | ------------------------------ |
| **框架类型** | Minecraft Paper 1.21.6 服务器插件核心 |
| **架构模式** | 服务定位器 + 事件驱动                   |
| **设计目标** | 统一服务管理、高性能缓存、异步执行、玩家数据生命周期管理   |
| **版本**   | 1.0.0+                         |

### 1.2 核心组件一览

```
RPGCore
├── EventBus (事件总线)
├── ServiceRegistry (服务注册中心)
├── CacheProvider (缓存管理)
├── AsyncExecutor (异步执行器)
├── PlayerLockManager (玩家锁管理器)
├── SyncScheduler (同步调度器)
├── PlayerLifecycleManager (玩家生命周期管理)
├── DisplayService (显示服务)
├── UnifiedDataManager (统一数据管理)
├── ExternalServiceIntegration (外部服务集成)
└── ServiceScanner (服务扫描器)
```

***

## 2. 核心 API 接口体系

### 2.1 ServiceRegistry - 服务注册中心

**接口位置**: `cn.guangdian.rpgcore.api.ServiceRegistry`

**实现位置**: `cn.guangdian.rpgcore.service.SimpleServiceRegistry`

**设计模式**: 服务定位器 (Service Locator Pattern)

```java
public interface ServiceRegistry {
    <T> void registerService(Class<T> serviceClass, T implementation, ServicePriority priority);
    <T> T getService(Class<T> serviceClass);
    <T> Optional<T> getOptionalService(Class<T> serviceClass);
    <T> boolean hasService(Class<T> serviceClass);
    <T> void unregisterService(Class<T> serviceClass);
    int getServiceCount();
    void clear();
}
```

**特性**:

- 双重注册机制：同时注册到内部 Map 和 Bukkit ServicesManager
- 优先级比较：不允许低优先级覆盖高优先级服务
- 线程安全：使用 ConcurrentHashMap 存储

**使用示例**:

```java
// 注册服务
RPGCore rpgCore = RPGCore.getInstance();
ServiceRegistry registry = rpgCore.getServiceRegistry();
registry.registerService(PointsService.class, new PointsServiceImpl(), ServicePriority.HIGH);

// 获取服务
PointsService points = registry.getService(PointsService.class);

// 可选获取
Optional<SkillService> skill = registry.getOptionalService(SkillService.class);
```

### 2.2 EventBus - 事件总线

**接口位置**: `cn.guangdian.rpgcore.api.EventBus`

**实现位置**: `cn.guangdian.rpgcore.event.SimpleEventBus`

**设计模式**: 发布-订阅 (Publish-Subscribe Pattern)

```java
public interface EventBus {
    <T extends CoreEvent> void publish(T event);
    <T extends CoreEvent> void publishAsync(T event);
    <T extends CoreEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);
    void unsubscribe(EventHandler<?> handler);
    <T extends CoreEvent> void unsubscribeAll(Class<T> eventType);
    boolean hasSubscribers(Class<? extends CoreEvent> eventType);
    int getSubscriberCount(Class<? extends CoreEvent> eventType);
}
```

**性能优化特性**:

| 优化项                      | 说明                                             |
| ------------------------ | ---------------------------------------------- |
| **批量处理**                 | 高频事件入队，每50ms批量分发                               |
| **动态调整**                 | 根据队列深度自动调整批量大小(20-500)和处理间隔(10-100ms)          |
| **优先级通道**                | PlayerDataLoadEvent/PlayerDataSaveEvent 立即同步处理 |
| **CopyOnWriteArrayList** | 并发读取安全的处理器列表                                   |

**事件流程**:

```
publish(event)
    ↓
isHighPriorityEvent? → YES → dispatchSingle() (同步处理)
    ↓ NO
batchEnabled? → YES → pendingEvents.offer() → 等待批量处理
    ↓ NO
dispatchSingle()
```

### 2.3 SyncScheduler - 同步调度器

**接口位置**: `cn.guangdian.rpgcore.api.SyncScheduler`

**实现位置**: `cn.guangdian.rpgcore.scheduler.UnifiedSchedulerImpl`

```java
public interface SyncScheduler {
    void runSync(Runnable task);
    long runSyncLater(Runnable task, long delayTicks);
    long runSyncRepeating(Runnable task, long delayTicks, long periodTicks);
    long runAsync(Runnable task);
    long runAsyncLater(Runnable task, long delayTicks);
    long runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);
    void cancelTask(long taskId);
    void cancelAllTasks();
    int getActiveTaskCount();
}
```

### 2.4 调度器架构详解 (Paper 1.21+)

#### 2.4.1 底层调度器对比

RPGCore 的 `SyncScheduler` 封装了 Paper 1.21+ 的两种调度器：

| 调度器 | 获取方式 | 时间单位 | 适用场景 | 线程 |
|--------|----------|----------|----------|------|
| **Bukkit Scheduler** | `Bukkit.getScheduler()` | **ticks** (1 tick = 50ms) | 需访问 Bukkit API | 主线程 |
| **AsyncScheduler** | `Bukkit.getAsyncScheduler()` | **毫秒** (必须用 `TimeUnit.MILLISECONDS`) | 耗时操作(I/O、网络) | 工作线程 |

#### 2.4.2 SyncScheduler 方法映射

| SyncScheduler 方法 | 内部实现 | 时间单位 | 说明 |
|-------------------|----------|----------|------|
| `runSync(Runnable)` | `Bukkit.getScheduler().runTask()` | - | 立即同步执行 |
| `runSyncLater(task, delayTicks)` | `Bukkit.getScheduler().runTaskLater()` | **ticks** | 延迟同步执行 |
| `runSyncRepeating(task, delayTicks, periodTicks)` | `Bukkit.getScheduler().runTaskTimer()` | **ticks** | 重复同步执行 |
| `runAsync(Runnable)` | `AsyncScheduler.runNow()` | - | 立即异步执行 |
| `runAsyncLater(task, delayTicks)` | `AsyncScheduler.runDelayed()` | **毫秒** ⚠️ | 延迟异步执行 |
| `runAsyncRepeating(task, delayTicks, periodTicks)` | `AsyncScheduler.runAtFixedRate()` | **毫秒** ⚠️ | 重复异步执行 |

⚠️ **重要**: `runAsyncLater` 和 `runAsyncRepeating` 的 delay/period 参数虽然接收 `long` 类型，但内部会**自动乘以 50**转换为毫秒（`delayMs = delayTicks * 50`）。

#### 2.4.3 AsyncScheduler 原生 API (Paper 1.21+)

当需要直接使用 Paper AsyncScheduler 时：

```java
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;

// 获取 AsyncScheduler
AsyncScheduler asyncScheduler = Bukkit.getAsyncScheduler();

// 立即异步执行
ScheduledTask task1 = asyncScheduler.runNow(plugin, scheduledTask -> {
    // 异步任务 - 不能访问 Bukkit API
});

// 延迟异步执行 (参数: 延迟时间, 时间单位)
ScheduledTask task2 = asyncScheduler.runDelayed(plugin, scheduledTask -> {
    // 延迟 1 秒后执行
}, 1000, TimeUnit.MILLISECONDS);

// 定时异步执行 (参数: 初始延迟, 间隔, 时间单位)
ScheduledTask task3 = asyncScheduler.runAtFixedRate(plugin, scheduledTask -> {
    // 每秒执行一次
}, 0, 1000, TimeUnit.MILLISECONDS);

// 取消任务
task1.cancel();

// 取消所有任务
asyncScheduler.cancelTasks(plugin);
```

#### 2.4.4 常见错误与正确做法

**错误 1**: 异步任务中访问 Bukkit API
```java
// ❌ 错误 - 异步线程中访问 Bukkit API 会抛出异常
scheduler.runAsync(() -> {
    player.sendMessage("Hello");  // 可能导致 ConcurrentModificationException
});

// ✅ 正确 - 访问 Bukkit API 必须在主线程
scheduler.runAsync(() -> {
    // 耗时操作：数据库、网络请求
});
scheduler.runSyncLater(() -> {
    player.sendMessage("Hello");  // 在主线程发送消息
}, 0L);
```

**错误 2**: 时间单位混淆
```java
// ❌ 错误 - 传入 50 以为是 50 ticks (2.5秒)，实际是 50 毫秒
scheduler.runAsyncLater(task, 50L);  // 实际只延迟 50ms = 0.05秒！

// ✅ 正确 - 使用 ticks 时会自动转换
scheduler.runSyncLater(task, 50L);   // 50 ticks = 2.5秒 (正确)
scheduler.runAsyncLater(task, 50L);   // 50 * 50 = 2500ms = 2.5秒 (正确)

// ✅ 直接使用 AsyncScheduler 原生 API 时必须明确单位
asyncScheduler.runDelayed(plugin, task, 2500, TimeUnit.MILLISECONDS);  // 2500ms
asyncScheduler.runDelayed(plugin, task, 50, TimeUnit.SECONDS);         // 50秒
```

**错误 3**: 任务取消不当
```java
// ❌ 错误 - 同步和异步任务取消方式不同
ScheduledTask asyncTask = asyncScheduler.runNow(...);
asyncTask.getTaskId();  // 不存在这个方法！

// ✅ 正确 - SyncScheduler 统一了任务 ID
long taskId = scheduler.runAsync(() -> {...});
scheduler.cancelTask(taskId);  // 统一取消接口
```

#### 2.4.5 最佳实践

```java
public class MyPlugin extends AbstractRPGPlugin {

    private long saveTaskId = -1;

    @Override
    protected void onPluginEnable() {
        // 定时保存 - 使用同步调度器 (访问 Bukkit API)
        saveTaskId = scheduler.runSyncRepeating(() -> {
            saveAllData();
        }, 0L, 12000L);  // 每 10 分钟 (12000 ticks)

        // 异步数据处理 - 不访问 Bukkit API
        scheduler.runAsync(() -> {
            List<PlayerData> data = loadFromDatabase();
            scheduler.runSyncLater(() -> {
                // 回到主线程处理
                processLoadedData(data);
            }, 0L);
        });
    }

    @Override
    protected void onPluginDisable() {
        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
    }
}
```

#### 2.4.6 调度器选择指南

| 场景 | 推荐调度器 | 示例 |
|------|-----------|------|
| 玩家数据保存 | `runSyncLater` (ticks) | `scheduler.runSyncLater(saveTask, 0L)` |
| 定时检查任务 | `runSyncRepeating` (ticks) | `scheduler.runSyncRepeating(checkTask, 0L, 20L)` |
| 数据库查询 | `runAsync` + `runSyncLater` | 异步查，主线程处理结果 |
| HTTP 请求 | `runAsync` | `scheduler.runAsync(httpTask)` |
| 延迟消息 | `runSyncLater` (ticks) | `scheduler.runSyncLater(msgTask, 100L)` |
| 冷却计时器 | `runAsyncRepeating` (ms) | `scheduler.runAsyncRepeating(cooldownCheck, 0L, 1000L)` |

***

## 3. 玩家数据生命周期管理

### 3.1 PlayerLifecycleManager

**位置**: `cn.guangdian.rpgcore.lifecycle.PlayerLifecycleManager`

**职责**: 统一管理玩家的登录/退出数据加载保存

**事件监听**:

- `PlayerJoinEvent` → 触发所有 PlayerDataHandler.onLoad()
- `PlayerQuitEvent` → 触发所有 PlayerDataHandler.onSave()

**核心方法**:

```java
public void registerHandler(PlayerDataHandler handler);  // 注册数据处理器
public void savePlayerData(Player player, boolean async); // 保存指定玩家
public void saveAllPlayers(boolean async);                // 保存所有在线玩家
```

**自动保存**: 每5分钟自动保存一次所有在线玩家数据

**性能监控**: 加载/保存耗时超过100ms的处理器会输出警告日志

### 3.2 AbstractPlayerDataHandler

**位置**: `cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler`

自定义数据处理器需实现:

```java
public interface PlayerDataHandler {
    String getHandlerName();       // 处理器名称
    int getPriority();              // 优先级 (数值越大越先执行)
    boolean shouldLoad(Player player);   // 是否需要加载
    boolean shouldSave(Player player);   // 是否需要保存
    void onLoad(PlayerDataLoadEvent event);  // 加载回调
    void onSave(PlayerDataSaveEvent event);  // 保存回调
}
```

***

## 4. 缓存系统

### 4.1 CacheProvider 接口

**位置**: `cn.guangdian.rpgcore.api.CacheProvider`

```java
public interface CacheProvider {
    <T> T get(String key, Class<T> type);
    <T> void put(String key, T value, Duration ttl);
    <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration ttl);
    void invalidate(String key);
    void invalidatePattern(String pattern);
    void clear();
    CacheStats getStats();
    int size();
    boolean containsKey(String key);
}
```

### 4.2 TTLCacheManager - 模式选择器

**位置**: `cn.guangdian.rpgcore.cache.TTLCacheManager`

**双模式设计**:

| 模式                    | 适用场景            | 特性                          |
| --------------------- | --------------- | --------------------------- |
| **LIGHTWEIGHT**       | 在线<50, 缓存<1000  | 无锁读取, 简单淘汰, 低内存             |
| **HIGH\_PERFORMANCE** | 在线>100, 缓存>1000 | O(1) LRU淘汰, Pattern缓存, 增量清理 |

**委托模式**: TTLCacheManager 只是一个选择器，实际缓存操作委托给:

- `LightweightCacheProvider`
- `HighPerformanceCacheProvider`

***

## 5. 异步执行系统

### 5.1 AsyncExecutor 接口

**位置**: `cn.guangdian.rpgcore.api.AsyncExecutor`

**实现位置**: `cn.guangdian.rpgcore.async.AsyncExecutorImpl`

```java
public interface AsyncExecutor {
    <T> CompletableFuture<T> execute(Callable<T> task);
    CompletableFuture<Void> execute(Runnable task);
    CompletableFuture<Void> submitPlayerSave(UUID playerId, Runnable saveTask);
    CompletableFuture<Void> getPendingSave(UUID playerId);
    boolean cancelPendingSave(UUID playerId);
    boolean awaitTermination(long timeout, TimeUnit unit);
    void shutdown();
    List<Runnable> shutdownNow();
    int getPendingTaskCount();
}
```

**玩家保存合并机制**:

```
同一玩家短时间内多次保存请求 → 合并为一次 → 避免数据库压力
```

**线程池**: 可配置的固定大小线程池，默认4线程

***

## 6. 并发控制

### 6.1 PlayerLockManager

**位置**: `cn.guangdian.rpgcore.concurrency.PlayerLockManager`

**设计目标**: 提供玩家级别的细粒度锁，避免全局锁竞争

**核心方法**:

```java
// 单锁操作
<T> T executeWithLock(UUID playerId, Supplier<T> operation);

// 双锁操作（防死锁，按UUID顺序获取）
<T> T executeWithDualLock(UUID playerId1, UUID playerId2, Supplier<T> operation);
```

**死锁预防**: `executeWithDualLock` 按 UUID 字典顺序获取锁

**超时机制**: 默认3秒超时，可配置

**统计功能**: 记录锁获取成功/超时次数

***

## 7. 外部服务集成

### 7.1 ExternalServiceIntegration

**位置**: `cn.guangdian.rpgcore.integration.ExternalServiceIntegration`

**统一封装**:

| 服务                 | API                   | 功能             |
| ------------------ | --------------------- | -------------- |
| **LuckPerms**      | `getPlayerPrefix()`   | 获取玩家前缀/后缀/主权限组 |
| **Vault**          | `getBalance()`        | 经济系统余额/存取款     |
| **PlaceholderAPI** | `parsePlaceholders()` | 解析占位符          |

**禁止直接调用**:

```java
// ❌ 禁止
LuckPermsProvider.get()
PlaceholderAPI.setPlaceholders(player, text)

// ✅ 正确
externalServices.getPlayerPrefix(player)
externalServices.parsePlaceholders(player, text)
```

***

## 8. 显示服务

### 8.1 DisplayService

**位置**: `cn.guangdian.rpgcore.display.DisplayService`

**功能**:

- 玩家前缀/后缀管理
- 玩家显示名/Tab名管理
- 玩家称号/Title管理

### 8.2 DisplayServiceImpl

**位置**: `cn.guangdian.rpgcore.display.DisplayServiceImpl`

**实现方式**: 使用 Scoreboard Team 存储 prefix/suffix

```java
// 存储结构
displays: Map<UUID, PlayerDisplay>
    └── PlayerDisplay { prefix, suffix, displayName, tabName, title }
```

***

## 9. 服务自动发现机制

### 9.1 @RPGService 注解

**位置**: `cn.guangdian.rpgcore.annotation.RPGService`

```java
@RPGService(
    serviceInterface = PointsService.class,
    priority = ServicePriority.NORMAL,
    name = "",
    lazy = false,
    singleton = true
)
public class PointsServiceImpl implements PointsService { }
```

**属性说明**:

| 属性               | 类型              | 默认值    | 说明     |
| ---------------- | --------------- | ------ | ------ |
| serviceInterface | Class           | **必填** | 服务接口类型 |
| priority         | ServicePriority | NORMAL | 注册优先级  |
| name             | String          | ""     | 服务名称   |
| lazy             | boolean         | false  | 是否延迟加载 |
| singleton        | boolean         | true   | 是否单例   |

### 9.2 @RPGComponent 注解

**位置**: `cn.guangdian.rpgcore.annotation.RPGComponent`

**组件类型**:

- `LISTENER` - 事件监听器
- `COMMAND` - 命令执行器
- `TASK` - 定时任务
- `PLACEHOLDER` - 占位符扩展
- `COMPONENT` - 通用组件

### 9.3 ServiceScanner

**位置**: `cn.guangdian.rpgcore.service.ServiceScanner`

**扫描流程**:

1. 遍历所有已加载插件
2. 解析插件 Jar 文件
3. 查找标注 `@RPGService` 的类
4. 按优先级排序后注册到 ServiceRegistry

***

## 10. 模块系统

### 10.1 RPGModule 基类

**位置**: `cn.guangdian.rpgcore.module.RPGModule`

**生命周期状态机**:

```
CREATED → LOADING → LOADED → ENABLING → ENABLED
                                      ↓
                              DISABLING → DISABLED
                                      ↓
                            DESTROYING → DESTROYED
```

**核心方法**:

```java
// 抽象方法 (必须实现)
protected abstract void registerServices();
protected abstract void registerCommands();
protected abstract void registerListeners();
protected abstract void saveAllData();

// 可选覆盖方法
protected void registerPlaceholders();  // 占位符注册
protected void startTasks();            // 启动定时任务
protected void stopTasks();             // 停止定时任务
```

***

## 11. 统一数据管理

### 11.1 UnifiedDataManager

**位置**: `cn.guangdian.rpgcore.storage.UnifiedDataManager`

**职责**: 统一管理各插件的玩家数据存储

**处理器机制**:

```java
public void registerHandler(PlayerStorageHandler handler);
public void loadPlayerData(UUID playerId);
public void savePlayerData(UUID playerId, boolean async);
```

### 11.2 PlayerStorageHandler 接口

```java
public interface PlayerStorageHandler {
    String getHandlerName();
    int getPriority();
    Object load(UUID playerId);
    void save(UUID playerId, Object data);
}
```

***

## 12. 配置系统

### 12.1 config.yml

**位置**: `plugins/RPGCore/src/main/resources/config.yml`

```yaml
# 数据库配置
database:
  enabled: true
  url: "jdbc:mysql://localhost:3306/mc_rpg"
  max-pool-size: 20

# 异步线程池
async:
  thread-pool-size: 4

# 缓存配置
cache:
  mode: lightweight           # lightweight | high_performance
  max-size: 2000
  default-ttl-minutes: 30

# 锁超时
lock:
  timeout-ms: 3000
```

***

## 13. 统一业务服务接口

### 13.1 已定义的 Service API

RPGCore 定义了26个统一服务接口:

| 分类       | 服务接口                                                   |
| -------- | ------------------------------------------------------ |
| **玩家数据** | PointsService, StatsService, GuildService              |
| **战斗系统** | SkillService, MobHealthService                         |
| **经济系统** | BankService, MarketService, TradeService               |
| **社交系统** | MarriageService, ChatService                           |
| **世界系统** | CaveService, LocationService, ForgeService             |
| **功能系统** | QuestService, NPCService, ConfigService                |
| **显示系统** | DisplayService, BoardService, TabService, MenuService  |
| **物品系统** | ItemTriggerService, DropControlService, CleanerService |
| **其他**   | AuthService, GiftService, AttributeParseService        |

***

## 14. 插件架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      RPGCore                                │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  EventBus   │  │  ServiceRegistry │  │  SyncScheduler  │  │
│  │  事件总线   │  │  服务注册中心 │  │  同步调度器        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Cache     │  │ AsyncExecutor│  │ PlayerLockManager │  │
│  │   缓存     │  │  异步执行器  │  │  玩家锁管理器      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Lifecycle  │  │   Display   │  │   ExternalServices │  │
│  │  生命周期   │  │   显示服务  │  │  外部服务集成      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│               GuangDian* 业务插件                          │
├─────────────────────────────────────────────────────────────┤
│  GuangDianArmorStats │ GuangDianPoints │ GuangDianGuild    │
│  GuangDianQuest      │ GuangDianMarket │ GuangDianNPC      │
│  GuangDianForge      │ GuangDianCaveFu │ GuangDianTab      │
│  ... (24个插件)                                            │
└─────────────────────────────────────────────────────────────┘
```

***

## 15. 关键设计原则

### 15.1 禁止模式清单

| 类别       | ❌ 禁止                          | ✅ 正确                                 |
| -------- | ----------------------------- | ------------------------------------ |
| **调度器**  | `new BukkitRunnable()`        | `scheduler.runSyncLater()`           |
| **核心获取** | `Bukkit.getPlugin("RPGCore")` | `RPGCore.getInstance()`              |
| **外部服务** | `LuckPermsProvider.get()`     | `externalServices.getPlayerPrefix()` |
| **全息图**  | `ArmorStand`                  | `TextDisplay`                        |
| **颜色**   | `ChatColor.RED`               | `NamedTextColor.RED`                 |

### 15.2 性能优化要点

1. **缓存**: 优先使用 CacheProvider，避免重复计算
2. **异步**: 数据库I/O必须在异步线程执行
3. **锁粒度**: 使用 PlayerLockManager 的玩家级锁而非全局锁
4. **批量处理**: 高频事件走 EventBus 批量分发

***

## 16. 文件结构

```
plugins/RPGCore/
├── src/main/java/cn/guangdian/rpgcore/
│   ├── RPGCore.java                      # 主入口类
│   ├── annotation/                       # 注解定义
│   │   ├── RPGComponent.java
│   │   └── RPGService.java
│   ├── api/                             # 核心API接口
│   │   ├── AsyncExecutor.java
│   │   ├── CacheProvider.java
│   │   ├── CacheStats.java
│   │   ├── ConfigManager.java
│   │   ├── DataRepository.java
│   │   ├── EventBus.java
│   │   ├── ExceptionHandler.java
│   │   ├── PluginLifecycleManager.java
│   │   ├── ServicePriority.java
│   │   ├── ServiceRegistry.java
│   │   └── SyncScheduler.java
│   ├── async/                           # 异步执行
│   │   └── AsyncExecutorImpl.java
│   ├── cache/                           # 缓存实现
│   │   ├── HighPerformanceCacheProvider.java
│   │   ├── LightweightCacheProvider.java
│   │   └── TTLCacheManager.java
│   ├── command/                         # 命令框架
│   │   ├── CommandFramework.java
│   │   └── SubCommand.java
│   ├── concurrency/                     # 并发控制
│   │   ├── LockStats.java
│   │   ├── LockTimeoutException.java
│   │   └── PlayerLockManager.java
│   ├── config/                          # 配置管理
│   │   ├── ConfigManager.java
│   │   └── ConfigManagerImpl.java
│   ├── database/                        # 数据库
│   │   └── CoreDatabase.java
│   ├── display/                         # 显示服务
│   │   ├── DisplayService.java
│   │   └── DisplayServiceImpl.java
│   ├── event/                           # 事件系统
│   │   ├── CoreEvent.java
│   │   ├── EventHandler.java
│   │   ├── EventPriority.java
│   │   ├── SimpleEventBus.java
│   │   └── events/                      # 具体事件
│   │       ├── skill/                   # 技能相关事件
│   │       └── *.java                   # 其他业务事件
│   ├── exception/                       # 异常处理
│   │   └── ExceptionHandlerImpl.java
│   ├── integration/                     # 外部服务集成
│   │   ├── ExternalServiceIntegration.java
│   │   ├── ExternalServiceIntegrationImpl.java
│   │   ├── RPGCoreHelper.java
│   │   └── UnifiedScheduler.java
│   ├── lifecycle/                       # 生命周期
│   │   ├── AbstractPlayerDataHandler.java
│   │   ├── PlayerDataHandler.java
│   │   ├── PlayerDataLoadEvent.java
│   │   ├── PlayerDataSaveEvent.java
│   │   └── PlayerLifecycleManager.java
│   ├── module/                          # 模块系统
│   │   ├── RPGModule.java
│   │   ├── armorstats/
│   │   └── points/
│   ├── monitor/                         # 性能监控
│   │   ├── OperationTimer.java
│   │   ├── PerformanceMetrics.java
│   │   ├── PerformanceMonitor.java
│   │   └── PerformanceReport.java
│   ├── permission/                      # 权限管理
│   │   └── PermissionManager.java
│   ├── plugin/                          # 插件基类
│   │   └── AbstractRPGPlugin.java
│   ├── scheduler/                       # 调度器
│   │   └── UnifiedSchedulerImpl.java
│   ├── service/                         # 服务管理
│   │   ├── ServiceScanner.java
│   │   ├── SimpleConfigService.java
│   │   ├── SimpleServiceRegistry.java
│   │   └── api/                         # 业务服务接口
│   │       ├── AttributeParseService.java
│   │       ├── AuthService.java
│   │       ├── BankService.java
│   │       ├── BoardService.java
│   │       ├── CaveService.java
│   │       ├── ChatService.java
│   │       ├── CleanerService.java
│   │       ├── ConfigService.java
│   │       ├── DisplayService.java
│   │       ├── DropControlService.java
│   │       ├── ForgeService.java
│   │       ├── GiftService.java
│   │       ├── GuildService.java
│   │       ├── ItemTriggerService.java
│   │       ├── LocationService.java
│   │       ├── MarketService.java
│   │       ├── MarriageService.java
│   │       ├── MenuService.java
│   │       ├── MobHealthService.java
│   │       ├── NPCService.java
│   │       ├── PointsService.java
│   │       ├── QuestService.java
│   │       ├── SkillService.java
│   │       ├── StatsService.java
│   │       ├── TabService.java
│   │       ├── TradeService.java
│   │       └── data/
│   │           ├── Loan.java
│   │           └── TransactionRecord.java
│   ├── storage/                         # 存储管理
│   │   ├── PlayerStorageHandler.java
│   │   ├── UnifiedDataManager.java
│   │   └── YamlRepository.java
│   └── util/                            # 工具类
│       ├── OfflinePlayerCache.java
│       └── Permissions.java
└── src/main/resources/
    ├── config.yml
    └── plugin.yml
```

***

## 17. 总结

RPGCore 是 Astraea RPG 服务器的核心基础设施，提供:

1. **统一的服务管理** - ServiceRegistry + @RPGService 自动发现
2. **解耦的事件系统** - EventBus 发布订阅 + 批量处理优化
3. **高性能缓存** - 双模式 TTL 缓存 (lightweight/high\_performance)
4. **玩家数据生命周期** - PlayerLifecycleManager 统一管理
5. **异步执行框架** - AsyncExecutor + 玩家保存合并
6. **并发控制** - PlayerLockManager 细粒度锁防死锁
7. **外部服务封装** - LuckPerms/Vault/PlaceholderAPI 统一接口

所有 GuangDian\* 业务插件都应基于 RPGCore 构建，确保架构统一和性能最优。

***

*文档生成时间: 2026-04-13*
