# RPGCore 开发核心标准

> 本规范适用于所有基于 RPGCore 的二次开发与扩展
> **版本: 1.1.0 | 更新: 2026-04-23**

---

## 📑 目录

- [一、架构分层原则](#一架构分层原则)
- [二、RPGCore 公共 API 完整参考](#二rpgcore-公共-api-完整参考)
- [三、插件开发规范](#三插件开发规范)
- [四、调度器使用规范](#四调度器使用规范)
- [五、数据生命周期管理](#五数据生命周期管理)
- [六、并发安全规范](#六并发安全规范)
- [七、缓存使用规范](#七缓存使用规范)
- [八、事件通信规范](#八事件通信规范)
- [九、消息与 UI 规范](#九消息与-ui-规范)
- [十、外部服务集成](#十外部服务集成)
- [十一、配置管理规范](#十一配置管理规范)
- [十二、性能调优建议](#十二性能调优建议)
- [十三、日志与监控](#十三日志与监控)
- [十四、安全检查清单](#十四安全检查清单)
- [十五、子插件与 RPGCore 交互规范](#十五子插件与-rpgcore-交互规范)
- [十六、参考文档](#十六参考文档)

---

## 📐 一、架构分层原则

### 1.1 微内核架构
```
┌─────────────────────────────────────────────┐
│               应用层 (GuangDian*)            │  插件业务逻辑
├─────────────────────────────────────────────┤
│           服务层 (ServiceRegistry)           │  服务注册与发现
├─────────────────────────────────────────────┤
│           核心层 (RPGCore)                   │  微内核核心服务
│  - PlayerLifecycle  - ServiceRegistry       │
│  - EventBus         - CacheProvider         │
│  - LockManager      - AsyncExecutor         │
│  - Database         - ConfigManager         │
├─────────────────────────────────────────────┤
│           集成层 (ExternalService)           │  LuckPerms, Vault, PAPI
├─────────────────────────────────────────────┤
│          基础设施层 (Paper API)              │  Bukkit, Paper API
└─────────────────────────────────────────────┘
```

### 1.2 依赖方向
**必须**遵循：应用层 → 服务层 → 核心层 → 集成层 → 基础设施层
**禁止**：下层依赖上层，同层直接依赖（通过服务注册表）

---

## 二、RPGCore 公共 API 完整参考

> 以下是 RPGCore 设计为供外部插件直接调用的所有公共接口和服务。
> 子插件**无需自行实现**这些功能，应直接使用 RPGCore 提供的服务。

### 2.1 核心服务入口

```java
RPGCore rpgCore = RPGCore.getInstance();
```

| 方法 | 返回类型 | 用途 |
|------|----------|------|
| `getScheduler()` | `SyncScheduler` | 统一调度器（同步/异步任务） |
| `getEventBus()` | `EventBus` | 发布-订阅事件系统 |
| `getServiceRegistry()` | `ServiceRegistry` | 服务注册与发现 |
| `getCacheProvider()` | `CacheProvider` | Caffeine 缓存管理 |
| `getLockManager()` | `PlayerLockManager` | 玩家级并发锁 |
| `getPlayerLifecycle()` | `PlayerLifecycleManager` | 玩家数据生命周期管理 |
| `getExternalServices()` | `ExternalServiceIntegration` | LuckPerms/Vault/PAPI 集成 |
| `getMiniMessageService()` | `MiniMessageService` | MiniMessage 消息服务 |
| `getTextDisplayService()` | `TextDisplayService` | TextDisplay 全息图服务 |
| `getMenuService()` | `MenuService` | YAML 驱动菜单服务 |
| `getMessageService()` | `MessageService` | 统一消息服务 |
| `getSoundService()` | `SoundService` | 音效服务 |
| `getCooldownManager()` | `CooldownManager` | 冷却时间管理 |
| `getGameLogger()` | `GameLogger` | 异步日志服务 |
| `getConfigManager()` | `ConfigManager` | 统一配置管理 |
| `getHttpClient()` | `HttpClient` | HTTP 客户端 |
| `getCronScheduler()` | `CronScheduler` | Cron 表达式定时任务 |
| `getConfigMigrator()` | `ConfigMigrator` | 配置版本迁移 |
| `getAuditLog()` | `AuditLog` | 操作审计日志 |
| `getDataExporter()` | `DataExporter` | 数据导出 |
| `getPerformanceMonitor()` | `PerformanceMonitor` | 性能监控 |
| `getExceptionHandler()` | `ExceptionHandler` | 异常处理 |
| `getItemAttributeManager()` | `ItemAttributeManager` | 物品属性管理 |
| `getEntityManager()` | `EntityService` | 实体服务 |
| `getServerService()` | `ServerService` | 服务器服务 |

### 2.2 SyncScheduler - 统一调度器

**获取方式**: `rpgCore.getScheduler()`

| 方法 | 说明 |
|------|------|
| `runSync(Runnable)` | 立即执行同步任务（主线程） |
| `runSyncLater(Runnable, long delayTicks)` | 延迟执行同步任务 |
| `runSyncRepeating(Runnable, long delayTicks, long periodTicks)` | 重复执行同步任务 |
| `runAsync(Runnable)` | 立即执行异步任务 |
| `runAsyncLater(Runnable, long delayTicks)` | 延迟执行异步任务 |
| `runAsyncRepeating(Runnable, long delayTicks, long periodTicks)` | 重复执行异步任务 |
| `cancelTask(long taskId)` | 取消指定任务 |
| `cancelAllTasks()` | 取消所有任务 |
| `getActiveTaskCount()` | 获取活跃任务数 |

**返回**: `runSyncLater/runAsyncLater/runSyncRepeating/runAsyncRepeating` 返回 `long` 任务 ID

### 2.3 EventBus - 事件总线

**获取方式**: `rpgCore.getEventBus()`

| 方法 | 说明 |
|------|------|
| `publish(T event)` | 同步发布事件 |
| `publishAsync(T event)` | 异步发布事件 |
| `subscribe(Class<T>, EventHandler<T>)` | 订阅事件 |
| `unsubscribe(EventHandler<?>)` | 取消订阅 |
| `unsubscribeAll(Class<T>)` | 取消某类型所有订阅 |
| `hasSubscribers(Class<T>)` | 是否有订阅者 |
| `getSubscriberCount(Class<T>)` | 获取订阅者数量 |

### 2.4 CacheProvider - 缓存服务

**获取方式**: `rpgCore.getCacheProvider()`

| 方法 | 说明 |
|------|------|
| `<T> T get(String key, Class<T> type)` | 获取缓存 |
| `<T> Optional<T> getOptional(String key, Class<T>)` | 获取缓存（Optional） |
| `<T> void put(String key, T value, Duration ttl)` | 存入缓存 |
| `<T> void put(String key, T value)` | 存入缓存（默认TTL） |
| `<T> T getOrLoad(String key, Class<T>, Supplier<T>, Duration)` | 获取或加载 |
| `invalidate(String key)` | 使缓存失效 |
| `invalidatePattern(String pattern)` | 模式匹配批量失效 |
| `clear()` | 清空所有缓存 |
| `size()` | 获取缓存大小 |
| `containsKey(String key)` | 检查键是否存在 |

### 2.5 PlayerLockManager - 并发锁

**获取方式**: `rpgCore.getLockManager()`

| 方法 | 说明 |
|------|------|
| `executeWithLock(UUID, Runnable)` | 单锁执行（无返回值） |
| `executeWithLock(UUID, Supplier<T>)` | 单锁执行（有返回值） |
| `executeWithDualLock(UUID, UUID, Runnable)` | 双锁执行（防死锁） |
| `executeWithDualLock(UUID, UUID, Supplier<T>)` | 双锁执行（有返回值） |
| `releaseLock(UUID)` | 释放锁 |
| `releaseAllLocks()` | 释放所有锁 |
| `getStats()` | 获取锁统计 |
| `getLockCount()` | 获取锁数量 |

### 2.6 PlayerLifecycleManager - 玩家生命周期

**获取方式**: `rpgCore.getPlayerLifecycle()`

| 方法 | 说明 |
|------|------|
| `registerHandler(PlayerDataHandler)` | 注册数据处理器 |
| `unregisterHandler(PlayerDataHandler)` | 注销数据处理器 |
| `savePlayerData(Player, boolean async)` | 保存玩家数据 |
| `saveAllPlayers(boolean async)` | 保存所有玩家数据 |
| `setAutoSaveInterval(long ticks)` | 设置自动保存间隔 |
| `getHandlers()` | 获取已注册处理器列表 |
| `getHandlerCount()` | 获取处理器数量 |
| `getLoadTime(UUID)` | 获取玩家加载时间 |
| `getSaveTime(UUID)` | 获取玩家保存时间 |

### 2.7 ServiceRegistry - 服务注册表

**获取方式**: `rpgCore.getServiceRegistry()`

| 方法 | 说明 |
|------|------|
| `registerService(Class<T>, T)` | 注册服务（默认优先级） |
| `registerService(Class<T>, T, ServicePriority)` | 注册服务（指定优先级） |
| `getService(Class<T>)` | 获取服务（必须存在） |
| `getOptionalService(Class<T>)` | 获取服务（可选） |
| `hasService(Class<T>)` | 检查服务是否存在 |
| `unregisterService(Class<T>)` | 注销服务 |
| `getServiceCount()` | 获取已注册服务数 |
| `clear()` | 清空所有服务 |

### 2.8 ExternalServiceIntegration - 外部服务集成

**获取方式**: `rpgCore.getExternalServices()`

| 方法 | 说明 |
|------|------|
| `isLuckPermsEnabled()` | LuckPerms 是否启用 |
| `isVaultEnabled()` | Vault 是否启用 |
| `isPlaceholderAPIEnabled()` | PlaceholderAPI 是否启用 |
| `getPlayerPrefix(Player)` | 获取玩家前缀 |
| `getPlayerSuffix(Player)` | 获取玩家后缀 |
| `getPlayerPrimaryGroup(Player)` | 获取玩家主组 |
| `getBalance(Player)` | 获取经济余额 |
| `deposit(Player, double)` | 存款 |
| `withdraw(Player, double)` | 取款 |
| `parsePlaceholders(Player, String)` | 解析占位符 |
| `getExternalServiceStatus()` | 获取所有服务状态 |

### 2.9 内置服务（通过 ServiceRegistry 获取）

RPGCore 自动注册以下服务，子插件可通过 `getServiceRegistry().getService()` 获取：

| 服务类 | 获取方式 | 说明 |
|--------|----------|------|
| `MiniMessageService` | `rpgCore.getMiniMessageService()` | MiniMessage 消息 |
| `SoundService` | `rpgCore.getSoundService()` | 音效播放 |
| `CooldownManager` | `rpgCore.getServiceRegistry().getService(CooldownManager.class)` | 冷却管理 |
| `YamlDataStore` | `rpgCore.getServiceRegistry().getService(YamlDataStore.class)` | YAML 数据存储 |
| `CommandFramework` | `rpgCore.getServiceRegistry().getService(CommandFramework.class)` | 命令框架 |
| `PlaceholderService` | `rpgCore.getServiceRegistry().getService(PlaceholderService.class)` | 占位符注册 |
| `AudienceService` | `rpgCore.getServiceRegistry().getService(AudienceService.class)` | 玩家消息 |
| `MenuService` | `rpgCore.getMenuService()` | GUI 菜单服务 |
| `TextDisplayService` | `rpgCore.getTextDisplayService()` | TextDisplay 全息图 |
| `MessageService` | `rpgCore.getMessageService()` | 统一消息服务 |

### 2.10 自定义事件（可通过 EventBus 发布/订阅）

RPGCore 定义了以下自定义事件，子插件可发布或订阅：

| 事件 | 用途 |
|------|------|
| `PlayerDataLoadEvent` | 玩家数据加载事件 |
| `PlayerDataSaveEvent` | 玩家数据保存事件 |
| `ModuleEnableEvent` | 模块启用事件 |
| `HologramCreatedEvent` | 全息图创建事件 |
| `HologramDeletedEvent` | 全息图删除事件 |
| `NPCCreatedEvent` | NPC 创建事件 |
| `NPCInteractEvent` | NPC 交互事件 |
| `PointsTransactionEvent` | 积分交易事件 |
| `RpgEconomyTransactionEvent` | 经济交易事件 |
| `RpgGuildEvent` | 公会事件 |
| `RpgLevelUpEvent` | 升级事件 |
| `RpgMobKillEvent` | 怪物击杀事件 |
| `RpgQuestEvent` | 任务事件 |
| `RpgStatChangeEvent` | 属性变更事件 |
| `PlayerFullHealthEvent` | 满血事件 |
| `PlayerHealthChangedEvent` | 血量变化事件 |
| `PlayerStatsChangedEvent` | 统计数据变化事件 |
| `WorldCreatedEvent` | 世界创建事件 |
| `WorldDeletedEvent` | 世界删除事件 |
| `RpgSkillCastEvent` | 技能施放事件 |
| `RpgSkillCooldownEvent` | 技能冷却事件 |
| `RpgSkillDamageEvent` | 技能伤害事件 |
| `RpgSkillLearnEvent` | 技能学习事件 |
| `RpgSkillPointEvent` | 技能点事件 |
| `RpgSkillUpgradeEvent` | 技能升级事件 |

---

## 🔌 二、插件开发规范

### 2.1 插件主类
所有独立插件**必须**继承 `AbstractRPGPlugin`：

```java
public class GuangDianXXX extends AbstractRPGPlugin {
    
    @Override
    protected void onPluginEnable() {
        // 1. 必须调用
        initCommonServices();
        
        // 2. 初始化插件特定服务
        // ...
    }

    @Override
    protected void onPluginDisable() {
        // 1. 必须调用 - 取消所有调度任务
        cancelAllTasks();
        
        // 2. 清理资源
        // ...
    }

    @Override
    protected String getPluginName() {
        return "GuangDianXXX";
    }
}
```

### 2.2 服务注册与发现
```java
// 注册服务
public class MyServiceAdapter implements MyService {
    public MyServiceAdapter() {
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

// 使用服务
MyService myService = rpgCore.getServiceRegistry().getService(MyService.class);
if (myService != null) {
    myService.doSomething();
}
```

### 2.3 模块系统 (RPGModule)
适用于：插件内功能解耦、按需启停的子系统

**模块生命周期**：
```
Plugin.onEnable()
    → PluginLifecycleManager.loadAllModules()
    → RPGModule.load()
    → PluginLifecycleManager.enableAllModules()
    → RPGModule.enable()

Plugin.onDisable()
    → PluginLifecycleManager.disableAllModules()
    → RPGModule.disable()
    → PluginLifecycleManager.destroyAllModules()
    → RPGModule.destroy()
```

---

## 📅 三、调度器使用规范

### 3.1 异步任务
```java
// ✅ 正确 - 使用 RPGCore SyncScheduler
SyncScheduler scheduler = rpgCore.getScheduler();
scheduler.runAsync(() -> {
    // 异步逻辑
});

// ❌ 禁止
Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
```

### 3.2 同步任务
```java
// ✅ 正确 - 使用 GlobalRegionScheduler
scheduler.runSync(() -> {
    // 主线程逻辑
});

scheduler.runSyncLater(() -> {
    // 延迟执行 (tick)
}, 20L);

// ❌ 禁止
Bukkit.getScheduler().runTask(plugin, task);
```

### 3.3 重复任务
```java
// ✅ 正确
long taskId = scheduler.runSyncRepeating(() -> {
    // 每分钟执行一次 (1200 ticks)
}, 0L, 1200L);

// 取消任务
scheduler.cancelTask(taskId);
```

---

## 🔄 四、数据生命周期管理

### 4.1 玩家数据加载
**必须在异步线程执行**，使用 `AbstractPlayerDataHandler`：

```java
public class PlayerXXXData extends AbstractPlayerDataHandler {
    
    private final Map<UUID, XXXData> cache = new ConcurrentHashMap<>();
    
    public PlayerXXXData(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    protected void onPlayerLoad(Player player) {
        // 已在异步线程中调用
        XXXData data = repository.load(player.getUniqueId()).join();
        cache.put(player.getUniqueId(), data);
    }

    @Override
    protected void onPlayerSave(Player player) {
        // 已在异步线程中调用
        XXXData data = cache.get(player.getUniqueId());
        if (data != null) {
            repository.save(player.getUniqueId(), data);
            cache.remove(player.getUniqueId());
        }
    }

    @Override
    public int getPriority() { return 100; }
    
    @Override
    public String getHandlerName() { return "PlayerXXX"; }
}
```

### 4.2 自动保存
```java
// PlayerLifecycleManager 已内置自动保存机制
// 默认 5 分钟间隔，在 config.yml 中可配置
```

---

## 🔒 五、并发安全规范

### 5.1 玩家级锁
```java
PlayerLockManager lockManager = rpgCore.getLockManager();

// 单玩家锁
lockManager.executeWithLock(playerUUID, () -> {
    // 临界区代码
});

// 双玩家锁 (交易场景)
lockManager.executeWithDualLock(fromUUID, toUUID, () -> {
    // 转账逻辑
});
```

### 5.2 锁使用原则
1. **锁粒度最小化**：只锁定必要的代码段
2. **避免锁嵌套**：不要在一个锁内部获取另一个锁
3. **超时处理**：捕获 `LockTimeoutException`
4. **不持有锁执行 IO**：先获取数据，再获取锁，再修改

---

## 💾 六、缓存使用规范

### 6.1 使用 Caffeine 缓存
```java
CacheProvider cacheProvider = rpgCore.getCacheProvider();

// 获取加载缓存
LoadingCache<String, Object> cache = cacheProvider.getLoadingCache(
    "myCache",
    key -> loadFromDatabase(key)
);

// 使用
Object value = cache.get("key");

// 失效
cache.invalidate("key");
```

### 6.2 缓存策略
- **短期缓存** (1-5分钟)：临时数据、频繁变化数据
- **中期缓存** (5-30分钟)：玩家配置、权限数据
- **长期缓存** (30+分钟)：静态数据、不变化数据

---

## 📢 七、事件通信规范

### 7.1 使用 EventBus
```java
// 发布事件
EventBus eventBus = rpgCore.getEventBus();
eventBus.publish(new MyCustomEvent(player, data));

// 订阅事件
eventBus.registerHandler(MyCustomEvent.class, event -> {
    // 处理逻辑
});
```

### 7.2 事件优先级
```java
public enum EventPriority {
    LOWEST,   // -2 最低
    LOW,      // -1 较低
    NORMAL,   // 0  默认
    HIGH,     // +1 较高
    HIGHEST   // +2 最高
}
```

---

## 🎨 八、消息与 UI 规范

### 8.1 消息发送
```java
// 使用 MiniMessageService
MiniMessageService mm = MiniMessageService.getInstance();
player.sendMessage(mm.green("成功消息"));
player.sendMessage(mm.colorize("<yellow>玩家 <white>" + name));

// ActionBar
player.sendActionBar(mm.aqua("提示消息"));
```

### 8.2 全息图 (TextDisplay)
```java
TextDisplayService textDisplay = rpgCore.getTextDisplayService();

// 创建
textDisplay.createHologram("hologramId", location, mm.green("全息图内容"));

// 显示
textDisplay.showHologramToPlayer("hologramId", player);

// 更新
textDisplay.updateHologram("hologramId", mm.red("更新内容"));

// 移除
textDisplay.removeHologram("hologramId");
```

### 8.3 GUI 菜单
```java
// 使用 GUIManager (YAML 驱动)
GUIManager guiManager = GUIManager.getInstance();
guiManager.openMenu(player, "menuId");
```

---

## 🔧 九、配置管理规范

### 9.1 配置迁移
```java
// 使用 ConfigMigrator 自动处理版本升级
ConfigMigrator migrator = rpgCore.getConfigMigrator();
migrator.migrate(configFile, targetVersion);
```

### 9.2 配置版本
所有插件配置文件**必须**包含 `config-version` 字段

---

## ⚡ 十、性能调优建议

### 10.1 异步化原则
- **所有 IO 操作**必须异步
- **数据库查询**必须异步
- **文件读写**必须异步
- **网络请求**必须异步

### 10.2 批量处理
高并发场景使用批量处理减少竞争：
```java
EventBus 内置批量处理器
- batch-size: 10-100
- batch-interval-ms: 50ms
```

### 10.3 资源释放
```java
// 插件禁用时确保释放
@Override
protected void onPluginDisable() {
    cancelAllTasks();
    // 关闭连接池
    // 清理缓存
    // 注销服务
}
```

---

## 📝 十一、日志与监控

### 11.1 日志级别
```java
logger.info("普通信息");      // 重要事件
logger.warning("警告信息");   // 可恢复异常
logger.severe("错误信息");    // 严重错误
logger.fine("调试信息");      // 开发调试
```

### 11.2 性能监控
```java
PerformanceMonitor monitor = rpgCore.getPerformanceMonitor();

OperationTimer timer = monitor.startTimer("operation_name");
// 执行操作
timer.stop();
```

---

## 🚨 十四、安全检查清单

- [ ] 所有数据库操作使用参数化查询
- [ ] 玩家输入进行验证和清理
- [ ] 敏感数据不落盘（密码、Token）
- [ ] 权限检查使用 LuckPerms
- [ ] 操作审计使用 AuditLog

---

## � 十五、子插件与 RPGCore 交互规范

> 本章节明确规定子插件如何与 RPGCore 主插件进行交互、调用服务和管理依赖。

### 15.1 交互架构原则

#### 单向依赖
```
子插件 (GuangDian*)  ──依赖──>  RPGCore
RPGCore  ──不依赖──>  子插件
```

**规则**：
- 子插件依赖 RPGCore 提供的服务
- RPGCore 不依赖任何子插件
- 子插件之间不直接互相依赖

#### 服务通信模式
```
子插件 A  ──发布事件──>  EventBus  ──订阅事件──>  子插件 B
子插件 A  ──注册服务──>  ServiceRegistry  ──获取服务──>  子插件 B
```

### 15.2 接口调用规范

#### 必须使用 RPGCore 服务
| 功能 | 禁止实现 | 必须使用 |
|------|----------|----------|
| 调度任务 | `BukkitRunnable`, `Bukkit.getScheduler()` | `rpgCore.getScheduler()` |
| 玩家数据 | 自行实现缓存 | `AbstractPlayerDataHandler` |
| 并发控制 | `synchronized`, `ReentrantLock` | `rpgCore.getLockManager()` |
| 消息发送 | `ChatColor`, `§` 符号 | `MiniMessageService` |
| 缓存管理 | `ConcurrentHashMap` | `rpgCore.getCacheProvider()` |
| 配置管理 | 手动读取 YAML | `rpgCore.getConfigManager()` |
| 外部集成 | `LuckPermsProvider.get()` | `rpgCore.getExternalServices()` |
| 日志记录 | `System.out`, `Bukkit.getLogger()` | `rpgCore.getGameLogger()` |
| 命令注册 | `onCommand()` | `CommandFramework` |
| 占位符 | 直接创建 `PlaceholderExpansion` | `PlaceholderService` |

#### 服务获取顺序
```java
// 优先级1: RPGCore 提供的方法
MiniMessageService mm = rpgCore.getMiniMessageService();

// 优先级2: ServiceRegistry 获取
MiniMessageService mm = rpgCore.getServiceRegistry()
    .getService(MiniMessageService.class);

// 优先级3: 单例模式（仅当以上都不可用）
MiniMessageService mm = MiniMessageService.getInstance();
```

### 15.3 依赖管理原则

#### build.gradle 依赖配置
```gradle
dependencies {
    // 必须依赖 RPGCore
    compileOnly project(':plugins:RPGCore')
    
    // Paper API
    compileOnly 'io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT'
    
    // 可选依赖（外部插件）
    compileOnly name: 'LuckPerms'
    compileOnly name: 'Vault'
    compileOnly name: 'PlaceholderAPI'
}
```

#### plugin.yml 配置
```yaml
name: GuangDianXXX
version: 1.0.0
main: cn.guangdian.XXX.XXXPlugin
api-version: '1.21.6'

# 必须依赖 RPGCore
depend:
  - RPGCore

# 软依赖（可选）
softdepend:
  - LuckPerms
  - Vault
  - PlaceholderAPI
```

### 15.4 兼容性要求

#### RPGCore 版本兼容
```java
// 检查 RPGCore 版本兼容性
RPGCore rpgCore = RPGCore.getInstance();
if (rpgCore == null) {
    getLogger().severe("未找到 RPGCore 插件!");
    getServer().getPluginManager().disablePlugin(this);
    return;
}

String version = rpgCore.getDescription().getVersion();
getLogger().info("RPGCore 版本: " + version);
```

#### 服务可用性检查
```java
@Override
protected void onPluginEnable() {
    initCommonServices();
    
    // 检查必要服务是否可用
    if (!isRPGCoreAvailable()) {
        getLogger().severe("RPGCore 服务不可用!");
        return;
    }
    
    // 检查可选服务
    if (isExternalServicesAvailable()) {
        if (externalServices.isLuckPermsEnabled()) {
            // 使用 LuckPerms 功能
        }
    }
}
```

### 15.5 子插件标准模板

```java
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.message.MiniMessageService;

public class GuangDianXXX extends AbstractRPGPlugin {

    @Override
    protected void onPluginEnable() {
        // 1. 必须调用 - 初始化通用服务
        initCommonServices();
        
        // 2. 注册数据处理器（如果需要）
        new XXXDataHandler(this).register();
        
        // 3. 注册服务（如果提供）
        ServiceRegistry registry = rpgCore.getServiceRegistry();
        registry.registerService(XXXService.class, new XXXServiceImpl());
        
        // 4. 注册命令
        CommandFramework command = rpgCore.getServiceRegistry()
            .getService(CommandFramework.class);
        command.registerCommand(new XXXCommand());
        
        getLogger().info(getPluginName() + " 已启动");
    }

    @Override
    protected void onPluginDisable() {
        // 1. 必须调用 - 取消所有任务
        cancelAllTasks();
        
        // 2. 注销服务
        ServiceRegistry registry = rpgCore.getServiceRegistry();
        registry.unregisterService(XXXService.class);
        
        // 3. 注销数据处理器
        // XXXDataHandler.unregister();
        
        getLogger().info(getPluginName() + " 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianXXX";
    }
}
```

### 15.6 跨插件通信

#### 方式1：事件总线（推荐）
```java
// 插件 A 发布事件
EventBus eventBus = rpgCore.getEventBus();
eventBus.publish(new XXXCompletedEvent(playerUUID, result));

// 插件 B 订阅事件
eventBus.subscribe(XXXCompletedEvent.class, event -> {
    // 处理事件
});
```

#### 方式2：服务注册表
```java
// 插件 A 注册服务
rpgCore.getServiceRegistry()
    .registerService(XXXService.class, new XXXServiceImpl());

// 插件 B 使用服务
XXXService xxxService = rpgCore.getServiceRegistry()
    .getService(XXXService.class);
xxxService.doSomething();
```

#### 禁止：直接调用其他插件类
```java
// ❌ 禁止
GuangDianYYY yyyPlugin = (GuangDianYYY) 
    Bukkit.getPluginManager().getPlugin("GuangDianYYY");
yyyPlugin.doSomething();

// ✅ 正确 - 通过服务注册表
GuangDianYYYService yyyService = rpgCore.getServiceRegistry()
    .getService(GuangDianYYYService.class);
yyyService.doSomething();
```

### 15.7 错误处理规范

#### 异常处理
```java
// 使用 RPGCore ExceptionHandler
ExceptionHandler handler = rpgCore.getExceptionHandler();

// 安全执行
handler.safeRun(() -> {
    // 可能抛出异常的操作
});

// 安全执行带返回值
Result result = handler.safeCall(() -> {
    return loadData();
}, defaultValue);
```

#### 服务降级
```java
// 优雅降级
MiniMessageService mm;
if (rpgCore != null) {
    mm = rpgCore.getMiniMessageService();
} else {
    mm = MiniMessageService.getInstance();
}

// 外部服务降级
if (externalServices.isLuckPermsEnabled()) {
    prefix = externalServices.getPlayerPrefix(player);
} else {
    prefix = "";
}
```

---

## �📚 十六、参考文档

- [FORBIDDEN_PATTERNS.md](./FORBIDDEN_PATTERNS.md) - 禁止模式清单
- [CODE_TEMPLATES.md](./CODE_TEMPLATES.md) - 代码模板库
- [RPGCore API 文档](../../plugins/RPGCore/src/main/java/cn/guangdian/rpgcore/api/) - API 接口定义
- [kaifa.md](./kaifa.md) - 任务分类与路由

---

*本规范由 RPGCore 团队维护*
*最后更新: 2026-04-23*