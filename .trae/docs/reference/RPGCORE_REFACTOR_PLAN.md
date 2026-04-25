# RPGCore 微内核架构重构方案

> **版本**: 3.0.0  
> **日期**: 2026-04-25  
> **作者**: Astraea RPG Team

---

## 一、现状分析

### 1.1 代码规模

| 指标 | 数值 |
|------|------|
| Java 文件总数 | 147 个 |
| 废弃API文件 | 16 个 |
| 主类行数 | 784 行 |
| 初始化组件数 | 30+ 个 |
| 依赖库数量 | 10+ 个 |

### 1.2 核心问题

#### 问题1: 多套机制并存

| 机制 | 现状 | 问题 |
|------|------|------|
| **事件系统** | Bukkit Event + RPGCore EventBus | 两套发布/订阅机制，开发者困惑 |
| **服务注册** | SimpleServiceRegistry + Services + Bukkit ServicesManager | 三层注册，状态不一致风险 |
| **配置管理** | ConfigManager + ConfigurateManager + ConfigurateSupport | 三个入口，API混乱 |
| **数据存储** | YamlDataStore + ConfigurateSupport | 废弃API未清理 |
| **依赖注入** | ServiceInjector + GuiceSupport | 两套DI，重复代码 |

#### 问题2: 主类膨胀

```java
// RPGCore.java 初始化了 30+ 个组件
private EventBus eventBus;
private ServiceRegistry serviceRegistry;
private CacheProvider cacheProvider;
private AsyncExecutor asyncExecutor;
private PlayerLockManager lockManager;
private PerformanceMonitor performanceMonitor;
// ... 还有 20+ 个
```

#### 问题3: 废弃API堆积

```
@Deprecated 文件:
├── CoreEvent.java              # 事件基类
├── EventBus.java               # 事件总线接口
├── SimpleServiceRegistry.java  # 服务注册表
├── YamlDataStore.java          # YAML存储
├── ServiceInjector.java        # 依赖注入
├── ColorUtil.java              # 颜色工具
└── ... 共 16 个文件
```

#### 问题4: 文档与实现不一致

- `FORBIDDEN_PATTERNS.md` 禁止使用 RPGCore EventBus
- 但代码里仍在初始化和使用 EventBus
- 开发者不知道该听谁的

---

## 二、目标架构

### 2.1 设计原则

| 原则 | 说明 |
|------|------|
| **单一入口** | 每种机制只有一个API入口 |
| **最小知识** | 业务插件只需知道 RPGCore 类 |
| **渐进增强** | 基础功能开箱即用，高级功能可选 |
| **向后兼容** | 废弃API保留一个版本周期 |

### 2.2 目标架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    RPGCore 3.0 微内核                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              RPGCore (统一入口)                       │   │
│  │                                                       │   │
│  │  .scheduler()  → Scheduler    统一调度               │   │
│  │  .config()     → Config       统一配置               │   │
│  │  .events()     → Events       统一事件               │   │
│  │  .data()       → Data         统一数据               │   │
│  │  .services()   → Services     统一服务               │   │
│  │  .message()    → Message      统一消息               │   │
│  │  .cache()      → Cache        统一缓存               │   │
│  │  .log()        → Log          统一日志               │   │
│  │                                                       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              内核组件 (Core Components)               │   │
│  │                                                       │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐    │   │
│  │  │Scheduler│ │ Config  │ │ Events  │ │  Data   │    │   │
│  │  │ Impl    │ │ Manager │ │ Helper  │ │ Manager │    │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘    │   │
│  │                                                       │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐    │   │
│  │  │Services │ │ Message │ │  Cache  │ │   Log   │    │   │
│  │  │Registry │ │ Service │ │Provider │ │ Factory │    │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘    │   │
│  │                                                       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              扩展点 (Extension Points)                │   │
│  │                                                       │   │
│  │  • DatabaseDriver (MySQL/SQLite/YAML)                │   │
│  │  • CacheStrategy (Caffeine/Guava/Custom)             │   │
│  │  • MessageFormat (MiniMessage/Legacy)                │   │
│  │                                                       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 统一API设计

#### 2.3.1 调度器 API

```java
public interface Scheduler {
    
    // 同步任务 (主线程)
    void runSync(Runnable task);
    void runSyncLater(Runnable task, long ticks);
    long runSyncRepeating(Runnable task, long delay, long period);
    
    // 异步任务 (后台线程)
    void runAsync(Runnable task);
    void runAsyncLater(Runnable task, long delayMs);
    long runAsyncRepeating(Runnable task, long delayMs, long periodMs);
    
    // 任务管理
    void cancelTask(long taskId);
    void cancelAllTasks();
}

// 使用方式
RPGCore.scheduler().runAsync(() -> {
    // 异步任务
});
```

#### 2.3.2 配置 API

```java
public interface ConfigManager {
    
    // 加载配置
    <T> Config<T> load(String name, Class<T> type);
    <T> Config<T> load(String name, Class<T> type, Supplier<T> defaults);
    
    // 获取已加载配置
    <T> Optional<Config<T>> get(String name);
    
    // 重载配置
    void reload(String name);
    void reloadAll();
}

public interface Config<T> {
    T get();
    void save();
    void reload();
    Path getPath();
}

// 使用方式
@ConfigSerializable
public class MyConfig {
    private String name = "default";
    private int value = 100;
}

Config<MyConfig> config = RPGCore.config().load("myplugin", MyConfig.class);
String name = config.get().getName();
```

#### 2.3.3 事件 API

```java
public interface Events {
    
    // 发布事件 (Bukkit 原生)
    void publish(Event event);
    
    // 订阅事件
    <T extends Event> void subscribe(Class<T> eventType, Consumer<T> handler);
    <T extends Event> void subscribe(Class<T> eventType, EventPriority priority, Consumer<T> handler);
    
    // 取消订阅
    <T extends Event> void unsubscribe(Class<T> eventType);
}

// 使用方式
RPGCore.events().subscribe(PlayerJoinEvent.class, event -> {
    Player player = event.getPlayer();
    player.sendMessage(Component.text("欢迎!"));
});

RPGCore.events().publish(new MyCustomEvent(player));
```

#### 2.3.4 数据 API

```java
public interface DataManager {
    
    // 注册数据处理器
    void register(String pluginName, Class<? extends PlayerData> dataType);
    void register(PlayerDataHandler handler);
    
    // 获取玩家数据
    <T extends PlayerData> T get(Player player, Class<T> type);
    <T extends PlayerData> Optional<T> getOptional(Player player, Class<T> type);
    
    // 保存玩家数据
    void save(Player player);
    void saveAll();
}

// 使用方式
public class MyPlayerData implements PlayerData {
    private UUID playerId;
    private int level;
    
    @Override
    public void load(UUID playerId) {
        // 从存储加载
    }
    
    @Override
    public void save(UUID playerId) {
        // 保存到存储
    }
}

// 注册
RPGCore.data().register("myplugin", MyPlayerData.class);

// 使用
MyPlayerData data = RPGCore.data().get(player, MyPlayerData.class);
```

#### 2.3.5 服务 API

```java
public interface Services {
    
    // 注册服务
    <T> void register(Class<T> type, T instance);
    <T> void registerLazy(Class<T> type, Supplier<T> factory);
    
    // 获取服务
    <T> T get(Class<T> type);
    <T> Optional<T> getOptional(Class<T> type);
    <T> T getOrDefault(Class<T> type, T defaultValue);
    
    // 注销服务
    <T> T unregister(Class<T> type);
    
    // 查询
    boolean isRegistered(Class<?> type);
    int getServiceCount();
}

// 使用方式
RPGCore.services().register(MyService.class, new MyServiceImpl());
MyService service = RPGCore.services().get(MyService.class);
```

---

## 三、迁移策略

### 3.1 迁移原则

1. **渐进式迁移**: 每个版本只迁移一部分
2. **向后兼容**: 废弃API保留一个版本周期
3. **双轨运行**: 新旧API并存期间，新API优先
4. **文档先行**: 先更新文档，再改代码

### 3.2 版本规划

```
┌─────────────────────────────────────────────────────────────┐
│                      版本迁移路线图                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  v2.2.0 (第一阶段) ✅ 已完成                                 │
│  ├── 实现统一 API 入口                                      │
│  ├── RPGCore.scheduler()                                   │
│  ├── RPGCore.config()                                      │
│  ├── RPGCore.events()                                      │
│  └── 更新所有文档                                           │
│                                                             │
│  v2.3.0 (第二阶段) ✅ 已完成                                 │
│  ├── RPGCore.data()                                        │
│  ├── RPGCore.services()                                    │
│  ├── RPGCore.message()                                     │
│  ├── 迁移 GuangDianPoints, GuangDianMarket, GuangDianQuest │
│  └── 标记废弃API为 forRemoval=true                          │
│                                                             │
│  v3.0.0 (当前版本) ✅ CoreEvent迁移完成                       │
│  ├── 保留废弃API兼容                                        │
│  ├── 所有废弃API标记 @Deprecated(forRemoval=true)           │
│  ├── CoreEvent子类全部迁移到Bukkit原生事件 ✅                │
│  ├── 迁移剩余业务插件到新API                                │
│  └── 更新迁移文档                                           │
│                                                             │
│  v4.0.0 ✅ 已完成                                          │
│  ├── 删除所有废弃 API (EventBus, CoreEvent等)               │
│  ├── 删除 SimpleServiceRegistry                            │
│  ├── 简化 RPGCore 主类                                     │
│  ├── 已迁移:                                                │
│  │   ├── getEventBus() → 25处调用 ✅                        │
│  │   ├── getServiceRegistry() → 104处调用 ✅               │
│  │   └── getCacheProvider() → 44处调用 ✅                  │
│  └── 清理无用代码 ✅                                        │
│                                                             │
│  v5.0.0 ✅ 已完成                                          │
│  ├── 删除 RPGCore 内部废弃实现类                           │
│  │   ├── SimpleEventBus.java ✅                            │
│  │   ├── MBassadorEventBus.java ✅                         │
│  │   └── HighPerformanceCacheProvider.java ✅              │
│  ├── 简化 RPGCore 主类 (移除废弃字段初始化) ✅             │
│  ├── 简化 TTLCacheManager (移除 Mode 枚举) ✅              │
│  └── 最终构建验证 ✅                                       │
│                                                             │
│  v6.0.0 ✅ 已完成                                          │
│  ├── 删除 CoreEvent.java (所有子类已迁移) ✅               │
│  ├── 清理 RPGCore 内部废弃引用 ✅                          │
│  │   ├── RPGCoreModule.java (Guice绑定) ✅                 │
│  │   └── RPGModule.java (EventBus引用) ✅                  │
│  ├── 删除 EventBus.java 接口 ✅                            │
│  ├── 删除 EventBusSupport.java ✅                          │
│  ├── 删除 EventHandler.java ✅                             │
│  ├── 删除 EventPriority.java ✅                            │
│  └── 最终构建验证 ✅                                       │
│                                                             │
│  📋 重构完成总结                                           │
│  ├── 已删除文件: 8个                                       │
│  ├── 已迁移调用: 200+处                                    │
│  ├── 已简化类: 5个                                         │
│  └── 构建状态: ✅ 通过                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 详细迁移步骤

#### 第一阶段: 统一入口 (v2.2.0)

**步骤 1.1: 创建统一入口类**

```java
// 新建 RPGCore.java 中的统一入口方法
public class RPGCore extends JavaPlugin {
    
    // 统一入口
    public static Scheduler scheduler() { return getInstance().schedulerImpl; }
    public static ConfigManager config() { return getInstance().configManager; }
    public static Events events() { return getInstance().eventsHelper; }
    public static DataManager data() { return getInstance().dataManager; }
    public static Services services() { return Services.getInstance(); }
    public static MessageService message() { return getInstance().miniMessageService; }
    
    // 保留旧方法 (标记废弃)
    @Deprecated
    public SyncScheduler getScheduler() { return scheduler; }
    @Deprecated
    public EventBus getEventBus() { return eventBus; }
}
```

**步骤 1.2: 创建 Events 辅助类**

```java
public class EventsHelper implements Events {
    
    @Override
    public void publish(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }
    
    @Override
    public <T extends Event> void subscribe(Class<T> eventType, Consumer<T> handler) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEvent(T event) {
                handler.accept(event);
            }
        }, RPGCore.getInstance());
    }
}
```

**步骤 1.3: 更新文档**

- 更新 `FORBIDDEN_PATTERNS.md`
- 更新 `CODE_TEMPLATES.md`
- 创建 `MIGRATION_GUIDE.md`

#### 第二阶段: 数据和服务统一 (v2.3.0)

**步骤 2.1: 重构 PlayerLifecycleManager**

```java
public class DataManagerImpl implements DataManager {
    
    private final Map<String, PlayerDataHandler> handlers = new ConcurrentHashMap<>();
    
    @Override
    public void register(String pluginName, Class<? extends PlayerData> dataType) {
        PlayerDataHandler handler = new PlayerDataHandlerAdapter(pluginName, dataType);
        handlers.put(pluginName, handler);
    }
    
    @Override
    public <T extends PlayerData> T get(Player player, Class<T> type) {
        // 从缓存获取或加载
    }
}
```

**步骤 2.2: 迁移业务插件**

每个业务插件需要:
1. 实现新的 PlayerData 接口
2. 使用 `RPGCore.data().register()` 注册
3. 移除自定义的数据加载/保存逻辑

#### 第三阶段: 清理废弃代码 (v3.0.0)

**步骤 3.1: 删除废弃类**

```
删除列表:
├── event/CoreEvent.java
├── event/EventBus.java
├── event/SimpleEventBus.java
├── event/MBassadorEventBus.java
├── event/EventBusSupport.java
├── event/EventHandler.java
├── event/EventPriority.java
├── service/SimpleServiceRegistry.java
├── data/YamlDataStore.java
├── inject/ServiceInjector.java
├── util/ColorUtil.java
└── 所有 event/events/*.java (改为 Bukkit Event)
```

**步骤 3.2: 简化 RPGCore 主类**

```java
public class RPGCore extends JavaPlugin {
    
    // 只保留核心组件
    private SchedulerImpl scheduler;
    private ConfigManagerImpl configManager;
    private EventsHelper eventsHelper;
    private DataManagerImpl dataManager;
    
    @Override
    public void onEnable() {
        initCore();
        getLogger().info("RPGCore 3.0 enabled!");
    }
    
    private void initCore() {
        scheduler = new SchedulerImpl(this);
        configManager = new ConfigManagerImpl(this);
        eventsHelper = new EventsHelper(this);
        dataManager = new DataManagerImpl(this);
    }
}
```

---

## 四、风险评估

### 4.1 风险矩阵

| 风险 | 概率 | 影响 | 等级 | 缓解措施 |
|------|------|------|------|----------|
| 业务插件不兼容 | 中 | 高 | **高** | 提供迁移指南，保留废弃API一个版本 |
| 性能下降 | 低 | 中 | **中** | 性能测试，基准对比 |
| 数据丢失 | 低 | 高 | **高** | 自动备份，迁移验证 |
| 开发者困惑 | 中 | 中 | **中** | 详细文档，示例代码 |
| 构建失败 | 低 | 中 | **中** | CI/CD，自动化测试 |

### 4.2 回滚方案

```
┌─────────────────────────────────────────────────────────────┐
│                      回滚策略                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Git 分支策略                                            │
│     ├── main (稳定版)                                       │
│     ├── develop (开发版)                                    │
│     └── feature/refactor-v3 (重构分支)                      │
│                                                             │
│  2. 版本标签                                                │
│     ├── v2.1.0-stable (当前稳定版)                          │
│     ├── v2.2.0-migration (第一阶段)                         │
│     └── v3.0.0-final (最终版)                               │
│                                                             │
│  3. 回滚触发条件                                            │
│     ├── 编译失败                                            │
│     ├── 测试覆盖率 < 60%                                    │
│     ├── 性能下降 > 10%                                      │
│     └── 业务插件严重Bug                                     │
│                                                             │
│  4. 回滚步骤                                                │
│     ├── 停止部署                                            │
│     ├── 切换到上一个稳定标签                                │
│     ├── 重新构建部署                                        │
│     └── 通知所有开发者                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 五、测试计划

### 5.1 单元测试

```java
// SchedulerTest.java
@Test
void testSyncTask() {
    AtomicInteger counter = new AtomicInteger(0);
    RPGCore.scheduler().runSync(() -> counter.incrementAndGet());
    assertEquals(1, counter.get());
}

@Test
void testAsyncTask() throws InterruptedException {
    AtomicInteger counter = new AtomicInteger(0);
    RPGCore.scheduler().runAsync(() -> counter.incrementAndGet());
    Thread.sleep(100);
    assertEquals(1, counter.get());
}
```

### 5.2 集成测试

```java
// DataMigrationTest.java
@Test
void testPlayerDataMigration() {
    // 1. 创建旧版数据
    YamlDataStore oldStore = YamlDataStore.getInstance();
    Map<String, Object> oldData = new HashMap<>();
    oldData.put("level", 10);
    oldStore.save(file, oldData);
    
    // 2. 迁移到新系统
    Config<PlayerData> newConfig = RPGCore.config().load("player", PlayerData.class);
    
    // 3. 验证数据一致
    assertEquals(10, newConfig.get().getLevel());
}
```

### 5.3 性能测试

| 测试项 | 基准值 | 目标值 | 测试方法 |
|--------|--------|--------|----------|
| 插件启动时间 | < 2s | < 2s | JMH |
| 事件发布延迟 | < 1ms | < 1ms | JMH |
| 数据加载时间 | < 100ms | < 100ms | 手动测试 |
| 内存占用 | < 50MB | < 50MB | VisualVM |

---

## 六、时间规划

### 6.1 里程碑

```
Week 1-2:  v2.2.0 开发
├── 统一入口实现
├── Events 辅助类
├── 文档更新
└── 单元测试

Week 3-4:  v2.2.0 测试与发布
├── 集成测试
├── 性能测试
├── Bug修复
└── 发布 v2.2.0

Week 5-6:  v2.3.0 开发
├── DataManager 重构
├── Services 统一
├── 业务插件迁移
└── 测试

Week 7-8:  v2.3.0 测试与发布
├── 全面测试
├── 迁移验证
└── 发布 v2.3.0

Week 9-10: v3.0.0 开发
├── 删除废弃代码
├── 简化主类
├── 最终测试
└── 发布 v3.0.0
```

### 6.2 资源需求

| 角色 | 人数 | 工作内容 |
|------|------|----------|
| 架构师 | 1 | 设计、评审、决策 |
| 开发者 | 2 | 编码、测试、文档 |
| 测试 | 1 | 测试用例、性能测试 |

---

## 七、验收标准

### 7.1 功能验收

- [ ] 所有统一 API 可用
- [ ] 所有业务插件迁移完成
- [ ] 废弃 API 全部删除
- [ ] 文档更新完成

### 7.2 质量验收

- [ ] 单元测试覆盖率 > 70%
- [ ] 集成测试全部通过
- [ ] 性能测试达标
- [ ] 无严重 Bug

### 7.3 文档验收

- [ ] API 文档完整
- [ ] 迁移指南完整
- [ ] 示例代码完整
- [ ] CHANGELOG 更新

---

## 八、附录

### A. 废弃API清理清单

| 文件 | 废弃版本 | 删除版本 | 替代方案 |
|------|----------|----------|----------|
| CoreEvent.java | 2.0.0 | 3.0.0 | Bukkit Event |
| EventBus.java | 2.0.0 | 3.0.0 | Bukkit Event |
| SimpleEventBus.java | 2.0.0 | 3.0.0 | Bukkit Event |
| MBassadorEventBus.java | 2.0.0 | 3.0.0 | Bukkit Event |
| EventBusSupport.java | 2.0.0 | 3.0.0 | Bukkit Event |
| SimpleServiceRegistry.java | 2.1.0 | 3.0.0 | Services |
| YamlDataStore.java | 2.0.0 | 3.0.0 | ConfigurateSupport |
| ServiceInjector.java | 2.0.0 | 3.0.0 | GuiceSupport |
| ColorUtil.java | 2.0.0 | 3.0.0 | MiniMessageService |

### B. 业务插件迁移清单

| 插件 | 状态 | 迁移内容 |
|------|------|----------|
| GuangDianArmorStats | 待迁移 | 数据存储、事件 |
| GuangDianPoints | 待迁移 | 数据存储 |
| GuangDianMarket | 待迁移 | 数据存储 |
| GuangDianQuest | 待迁移 | 数据存储 |
| GuangDianNPC | 待迁移 | 事件 |
| GuangDianForge | 待迁移 | 数据存储 |

### C. 参考资料

- [Bukkit Event API](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/package-summary.html)
- [Paper AsyncScheduler](https://docs.papermc.io/paper/dev/async-scheduler)
- [Configurate Documentation](https://configurate.aoeu.xyz/)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)

---

*文档版本: 1.0.0 | 最后更新: 2026-04-25*
