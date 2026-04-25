# Astraea RPG 禁止模式清单

> 所有开发必须遵守的禁止模式，违反将导致代码被拒绝
> **版本: 1.4.1 | 更新: 2026-04-24**

---

## ⚠️ 基本原则

1. **不可随意回滚** - 每次提交必须有明确的理由和测试
2. **脚本采用 CMD 执行** - 禁止使用 PowerShell 执行构建脚本
3. **先构建验证后提交** - 禁止未验证就提交

---

## 1. 调度器禁止项 (v1.3.0 更新)

### ❌ 禁止 (Bukkit 传统调度器)
```java
new BukkitRunnable() { ... }.runTaskTimer(plugin, delay, period);
new BukkitRunnable() { ... }.runTaskLater(plugin, delay);
Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, delay, period);
Bukkit.getScheduler().runTaskLater(plugin, task, delay);
Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
// 异步任务
Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
```

### ✅ 正确 (Paper 1.21.6 调度器 + RPGCore SyncScheduler)
```java
RPGCore rpgCore = RPGCore.getInstance();
if (rpgCore != null) {
    SyncScheduler scheduler = rpgCore.getScheduler();
    
    // 异步任务 - 使用 RPGCore SyncScheduler
    scheduler.runAsync(() -> {
        // 异步执行的任务
    });

    // 异步延迟任务 (毫秒)
    scheduler.runAsyncDelayed(() -> {
        // 延迟执行
    }, 5000, java.util.concurrent.TimeUnit.MILLISECONDS);

    // 同步任务 - 使用 GlobalRegionScheduler
    scheduler.runSync(() -> { });
    
    // 同步延迟任务 (tick)
    long taskId = scheduler.runSyncLater(() -> { }, 50L);
    
    // 同步重复任务 (tick)
    long repeatingId = scheduler.runSyncRepeating(() -> { }, 0L, 20L);
    
    // 取消任务
    scheduler.cancelTask(taskId);
}
```

---

## 2. RPGCore 获取禁止项

### ❌ 禁止
```java
Bukkit.getPlugin("RPGCore")
Bukkit.getPluginManager().getPlugin("RPGCore")
(RPGCore) Bukkit.getPlugin("RPGCore")
RPGCore rpgCore = (RPGCore) Bukkit.getPluginManager().getPlugin("RPGCore"); // 强制类型转换
```

### ✅ 正确
```java
// 方式1: 使用静态方法获取
RPGCore rpgCore = RPGCore.getInstance();
if (rpgCore != null) {
    // 使用 rpgCore
}

// 方式2: 在 AbstractRPGPlugin 子类中
public class MyPlugin extends AbstractRPGPlugin {
    protected RPGCore rpgCore; // 自动注入
    
    @Override
    protected void onPluginEnable() {
        // rpgCore 已自动初始化
        SyncScheduler scheduler = rpgCore.getScheduler();
    }
}
```

---

## 3. 颜色服务禁止项

### ❌ 禁止 (旧版 ChatColor 和 § 符号)
```java
player.sendMessage(ChatColor.RED + "错误消息");
player.sendMessage("§c错误消息");
player.sendMessage("&a成功消息");
player.spigot().sendMessage(ChatMessageType.ACTION_BAR, "...");
```

### ✅ 正确 (Adventure MiniMessage)
```java
// 方式1: MiniMessageService
MiniMessageService mm = MiniMessageService.getInstance();
player.sendMessage(mm.red("错误消息"));
player.sendMessage(mm.green("成功消息"));
player.sendMessage(mm.colorize("<yellow>普通消息<reset> <red>错误"));

// 方式2: 直接使用 Component
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

player.sendMessage(Component.text("错误消息").color(NamedTextColor.RED));
player.sendMessage(Component.text("普通消息").color(NamedTextColor.YELLOW));

// ActionBar
player.sendActionBar(Component.text("ActionBar消息").color(NamedTextColor.GOLD));
```

---

## 4. 外部服务调用禁止项 (v1.3.0 更新)

### ❌ 禁止
```java
LuckPermsProvider.get()  // 禁止直接使用 LuckPerms 静态获取
luckPerms.getUserManager().getUser(player.getUniqueId())  // 未做 null 检查
PlaceholderAPI.setPlaceholders(player, text)  // 禁止直接调用 PlaceholderAPI
expansion.unregister()  // 禁止直接注销 PlaceholderExpansion
```

### ✅ 正确
```java
// 方式1: 使用 RPGCore 统一外部服务集成 (推荐)
RPGCore rpgCore = RPGCore.getInstance();
if (rpgCore != null) {
    ExternalServiceIntegration externalServices = rpgCore.getExternalServices();
    
    if (externalServices.isLuckPermsEnabled()) {
        String prefix = externalServices.getPlayerPrefix(player);
    }
    if (externalServices.isPlaceholderAPIEnabled()) {
        String parsed = externalServices.parsePlaceholders(player, text);
    }
}

// 方式2: 直接通过 ServicesManager 获取 (当 RPGCore 不可用时)
RegisteredServiceProvider<LuckPerms> provider = 
    Bukkit.getServicesManager().getRegistration(LuckPerms.class);
if (provider != null) {
    LuckPerms luckPerms = provider.getProvider();
    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
    if (user != null) {
        // 使用 user
    }
}

// 注销 PlaceholderExpansion (必须使用)
PlaceholderAPI.unregisterExpansion(expansionInstance);
```

---

## 5. MythicMobs PDC Key 禁止项

### ❌ 禁止
```java
new NamespacedKey("mythicmobs", "item")
```

### ✅ 正确
```java
NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
String typeId = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
```

---

## 6. 插件主类禁止项

### ❌ 禁止
```java
public class MyPlugin extends JavaPlugin { }  // 禁止直接继承 JavaPlugin
```

### ✅ 正确
```java
// 独立插件必须继承 AbstractRPGPlugin
public class MyPlugin extends AbstractRPGPlugin {
    @Override 
    protected void onPluginEnable() {
        initCommonServices(); // 必须调用
        // 插件逻辑
    }
    @Override 
    protected void onPluginDisable() {
        cancelAllTasks(); // 确保取消所有任务
    }
    @Override 
    protected String getPluginName() { 
        return "MyPlugin"; 
    }
}
```

---

## 7. 全息图显示禁止项

### ❌ 禁止 (ArmorStand)
```java
ArmorStand hologram = location.getWorld().spawn(location, ArmorStand.class);
hologram.setCustomName("全息图文字");
hologram.setVisible(false);
```

### ✅ 正确 (Paper 1.21+ TextDisplay)
```java
TextDisplay textDisplay = location.getWorld().spawn(location, TextDisplay.class);
textDisplay.setText("全息图文字");
textDisplay.setBillboard(Display.Billboard.CENTER);
```

---

## 8. 日志服务禁止项

### ❌ 禁止
```java
System.out.println("调试信息");
Bukkit.getLogger().info("消息");
```

### ✅ 正确
```java
RPGCore rpgCore = RPGCore.getInstance();
GameLogger logger = rpgCore.getGameLogger();
logger.info("消息");
logger.warning("警告");
logger.severe("错误");
```

---

## 9. 缓存服务禁止项 (v2.0.0 更新)

### ❌ 禁止 (ConcurrentHashMap 简单缓存)
```java
Map<String, Object> cache = new ConcurrentHashMap<>();
cache.put(key, value);
```

### ❌ 禁止 (使用已废弃的缓存实现)
```java
// 禁止: LightweightCacheProvider 和 HighPerformanceCacheProvider 已废弃
new LightweightCacheProvider(maxSize, ttl, recordStats);
new HighPerformanceCacheProvider(maxSize, ttl, recordStats);
TTLCacheManager.Mode.LIGHTWEIGHT;
TTLCacheManager.Mode.HIGH_PERFORMANCE;
```

### ✅ 正确 (Caffeine - 推荐)
```java
RPGCore rpgCore = RPGCore.getInstance();
CacheProvider cacheProvider = rpgCore.getCacheProvider();

LoadingCache<String, Object> cache = cacheProvider.getLoadingCache("myCache",
    k -> loadFromDatabase(k));

Object value = cache.get(key);
cache.invalidate(key);

// 或直接使用 TTLCacheManager 的 Caffeine 模式
CacheProvider cache = new TTLCacheManager(2000, Duration.ofMinutes(30), true, TTLCacheManager.Mode.CAFFEINE);
```

---

## 10. 脚本执行禁止项

### ❌ 禁止 (PowerShell)
```powershell
# 禁止使用 PowerShell 执行脚本
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build
```

### ✅ 正确 (CMD)
```cmd
cd /d e:\原创RPG服务端
set JAVA_HOME=e:\原创RPG服务端\tools\jdk-21.0.10+7
D:\gradle\gradle-9.4.0\bin\gradle.bat build --no-configuration-cache -x test
```

---

## 11. Git 提交禁止项

### ❌ 禁止
- 不可随意回滚已提交的代码
- 不可提交未编译验证的代码
- 不可提交包含 `TODO: 稍后修复` 的代码

### ✅ 正确
- 修改后必须构建验证
- 提交信息必须包含变更说明
- 重大变更必须更新 CHANGELOG.md

---

## 12. RPGCore 统一服务禁止项 (v1.4.0 更新)

### ❌ 禁止 (重复实现 RPGCore 已提供的功能)

```java
// 禁止: 自己实现 colorize() 方法
private Component colorize(String text) {
    return miniMessage.deserialize(text.replace("&a", "<green>")...);
}

// 禁止: 直接使用 ConcurrentHashMap 存储玩家数据
private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

// 禁止: 手动加载/保存 YAML 文件 (Bukkit API)
YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
yaml.save(file);

// 禁止: 使用已废弃的 YamlDataStore (v1.4.0+)
YamlDataStore store = YamlDataStore.getInstance(); // 已废弃

// 禁止: 使用已废弃的 ServiceInjector (v1.4.0+)
ServiceInjector.inject(this); // 已废弃

// 禁止: 自己实现冷却管理
private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

// 禁止: 直接创建 PlaceholderExpansion
new MyPlaceholderExpansion().register();
```

### ✅ 正确 (使用 RPGCore 统一服务)

```java
// 正确: 使用 MiniMessageService
MiniMessageService mm = MiniMessageService.getInstance();
player.sendMessage(mm.green("成功消息"));

// 正确: 继承 PlayerDataService
public class MyDataService extends PlayerDataService<MyData> {
    // 自动处理缓存、保存、序列化
}

// 正确: 使用 ConfigurateSupport (v1.4.0+ 推荐)
@ConfigSerializable
public class MyConfig {
    private String name = "default";
    // getters...
}

ConfigurateSupport<MyConfig> config = ConfigurateSupport
    .builder(MyConfig.class)
    .file("config.yml")
    .autoSave()
    .build();
MyConfig data = config.get();

// 正确: 使用 GuiceSupport (v1.4.0+ 推荐)
public class MyPlugin extends AbstractRPGPlugin {
    @Inject private MyService myService;
    
    @Override
    protected void onPluginEnable() {
        GuiceSupport.injectMembers(this);
        // 或使用子注入器
        GuiceSupport.childInjector()
            .with(new MyModule())
            .inject(this);
    }
}

// 正确: 使用 CooldownManager
CooldownManager cooldown = CooldownManager.getInstance();
cooldown.setCooldown(playerUUID, "action", 10000);

// 正确: 使用 PlaceholderService
PlaceholderService placeholder = PlaceholderService.getInstance();
placeholder.register("my_value", (player, params) -> "value");

// 正确: 使用 LoggerFactory (SLF4J)
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
logger.info("玩家 {} 执行了命令 {}", playerName, command);

// 正确: 使用 EventBusSupport
EventBusSupport.subscribe(MyEvent.class, event -> {
    // 处理事件
});
```

**废弃API迁移指南**:

| 废弃API | 替代API | 迁移难度 |
|--------|--------|---------|
| `YamlDataStore` | `ConfigurateSupport` | 中 |
| `ServiceInjector` | `GuiceSupport` | 低 |
| `ColorUtil` | `MiniMessageService` | 低 |

**原因**: 
- 避免 2500+ 行重复代码
- 统一管理,便于维护
- 内置性能优化和异常处理
- 符合微内核架构原则
- 使用成熟库 (Guice, Configurate, SLF4J)

**参考文档**:
- [UNIFIED_SERVICES_OVERVIEW.md](../docs/RPGCore/UNIFIED_SERVICES_OVERVIEW.md)
- [MIGRATION_GUIDE.md](../docs/RPGCore/MIGRATION_GUIDE.md) (v1.4.0+)

---

## 13. 文本剥离/序列化禁止项 (v1.2.1 新增)

### ❌ 禁止 (自定义文本剥离实现)

```java
// 禁止: 使用正则表达式自定义剥离颜色代码
private String stripColors(String text) {
    return text.replaceAll("[&§][0-9a-fk-or]", "");
}

// 禁止: 逐字符手动处理颜色代码
private String stripLegacy(String input) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
        char c = input.charAt(i);
        if ((c == '&' || c == '§') && i + 1 < input.length()) {
            i++; // 跳过颜色代码
            continue;
        }
        sb.append(c);
    }
    return sb.toString();
}

// 禁止: 手动拼接 Component 然后转字符串
private String componentToString(Component component) {
    StringBuilder sb = new StringBuilder();
    for (TextContainable t : component.children()) {
        if (t instanceof TextNode node) {
            sb.append(node.content());
        }
    }
    return sb.toString();
}
```

### ✅ 正确 (使用 Adventure API PlainTextComponentSerializer)

```java
// 方式1: 直接使用 PlainTextComponentSerializer (推荐用于 Component)
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
String plainText = serializer.serialize(component);

// 方式2: 使用 RPGCore TextStripper (推荐 - 职责分离的格式处理)
import cn.guangdian.rpgcore.util.TextStripper;

// 根据插件使用的格式类型选择对应方法：

// 1. MiniMessage 格式 (RPGItem、现代GUI等)
String text1 = TextStripper.stripMiniMessage(input);     // 仅剥离 <green> 等标签

// 2. 传统颜色格式 (MythicMobs、物品Lore、旧配置等)
String text2 = TextStripper.stripLegacy(input);          // 仅剥离 & 和 § 颜色码

// 3. 不确定格式或需要兼容两种
String text3 = TextStripper.stripAll(input);             // 同时处理两种格式
String text4 = TextStripper.stripSmart(input);           // 自动检测选择

// 4. 快速检测格式类型
boolean hasMiniMessage = TextStripper.containsMiniMessage(input);
boolean hasLegacy = TextStripper.containsLegacy(input);
```

**使用场景指南**:
| 场景 | 推荐方法 | 说明 |
|------|----------|------|
| RPGItem 物品解析 | `stripMiniMessage()` | RPGItem 使用 MiniMessage 格式 |
| MythicMobs Lore | `stripLegacy()` | 神话生物使用 & 颜色码 |
| 通用配置处理 | `stripSmart()` | 自动检测格式类型 |
| 兼容旧代码 | `stripAll()` | 同时处理两种格式 |

**性能对比**:
| 方法 | 相对性能 | 代码行数 |
|------|---------|----------|
| 自定义正则 | 1x (基准) | ~30-50行 |
| 逐字符处理 | 2-3x | ~40-60行 |
| PlainTextComponentSerializer | 3-5x | 1行 |
| RPGCore TextStripper | 3-5x | 1行 |

**原因**:
- Adventure API 原生实现，经过高度优化
- PlainTextComponentSerializer 处理所有 Component 子类型
- TextStripper 职责分离：MiniMessage 和传统颜色分开处理
- 根据插件实际使用的格式选择对应方法，避免不必要的处理
- 代码量减少约 80%，可维护性显著提升

**参考文档**:
- [TextStripper.java](../../plugins/RPGCore/src/main/java/cn/guangdian/rpgcore/util/TextStripper.java)

---

## 14. 数据生命周期管理禁止项 (v1.3.0 新增)

### ❌ 禁止 (在主线程执行数据加载/保存)

```java
// 禁止: 在 PlayerJoinEvent 中直接执行 IO 操作
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    // 直接加载数据库
    PlayerData data = database.load(player.getUniqueId()); // 阻塞主线程！
    // 或保存
    database.save(player.getUniqueId(), data); // 阻塞主线程！
}

// 禁止: 在 PlayerQuitEvent 中同步等待保存完成
@EventHandler
public void onPlayerQuit(PlayerQuitEvent event) {
    CompletableFuture<Void> saveFuture = saveDataAsync(event.getPlayer());
    saveFuture.join(); // 阻塞主线程等待完成！
}
```

### ✅ 正确 (使用 PlayerLifecycleManager 异步处理)

```java
// 方式1: 继承 AbstractPlayerDataHandler (推荐)
public class MyDataHandler extends AbstractPlayerDataHandler {
    
    public MyDataHandler(JavaPlugin plugin) {
        super(plugin);
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        // RPGCore 自动在异步线程调用此方法
        PlayerData data = repository.load(player.getUniqueId()).join();
        // 数据已在异步线程，可以安全执行 IO
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        // RPGCore 自动在异步线程调用此方法
        repository.save(player.getUniqueId(), dataCache.get(player.getUniqueId()));
    }
    
    @Override
    public int getPriority() { return 100; }
    
    @Override
    public String getHandlerName() { return "MyData"; }
}

// 注册处理器
new MyDataHandler(plugin).register();

// 方式2: 使用 PlayerLifecycleManager 手动管理
PlayerLifecycleManager lifecycle = rpgCore.getPlayerLifecycle();
lifecycle.registerHandler(new PlayerDataHandler() {
    @Override
    public void onLoad(PlayerDataLoadEvent event) {
        // 异步加载逻辑
    }
    @Override
    public void onSave(PlayerDataSaveEvent event) {
        // 异步保存逻辑
    }
    // ... 其他方法
});
```

**原因**:
- 主线程阻塞会导致整个服务器 TPS 下降
- 数据加载/保存必须异步执行
- PlayerLifecycleManager 自动管理加载顺序和线程安全

---

## 15. 并发安全禁止项 (v1.3.0 新增)

### ❌ 禁止 (不使用锁保护玩家数据)

```java
// 禁止: 直接读写玩家数据
@EventHandler
public void onPlayerDeath(PlayerDeathEvent event) {
    PlayerData data = cache.get(event.getPlayer().getUniqueId());
    data.addDeathCount(1); // 多线程并发修改！
}

// 禁止: 使用 ConcurrentHashMap 替代锁
private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();

// 非原子操作仍然不安全
PlayerStats stats = statsCache.get(uuid);
stats.addPoints(100); // 另一个线程可能同时修改！
```

### ✅ 正确 (使用 PlayerLockManager)

```java
RPGCore rpgCore = RPGCore.getInstance();
PlayerLockManager lockManager = rpgCore.getLockManager();

// 方式1: 使用 executeWithLock (推荐 - 自动处理锁获取/释放)
lockManager.executeWithLock(playerUUID, () -> {
    PlayerStats stats = statsCache.get(playerUUID);
    stats.addPoints(100);
});

// 方式2: 使用 executeWithDualLock (用于转账/交易场景)
lockManager.executeWithDualLock(fromUUID, toUUID, () -> {
    PlayerStats from = statsCache.get(fromUUID);
    PlayerStats to = statsCache.get(toUUID);
    if (from.hasPoints(amount)) {
        from.removePoints(amount);
        to.addPoints(amount);
    }
});

// 方式3: 使用 acquireLock 手动管理 (高级场景)
try (var lock = lockManager.acquireLock(playerUUID)) {
    // 临界区代码
} catch (LockTimeoutException e) {
    // 超时处理
}
```

**锁机制保障**:
- UUID 排序防死锁
- 超时自动释放 (5秒默认)
- 线程安全统计
- 死锁检测与恢复

---

---

## 16. 事件系统使用规范 (v2.0.0 更新)

### 架构说明

RPGCore 2.0.0+ 采用 **"Bukkit 事件 + RPGCore 管控层"** 架构：

```
┌─────────────────────────────────────────┐
│         Bukkit 原生事件系统              │ ← 底层统一使用 Bukkit
│    Bukkit.getPluginManager().callEvent() │
└─────────────────────────────────────────┘
              ↑
┌─────────────────────────────────────────┐
│      EventPublisher 管控层（可选）        │ ← RPGCore 增值功能
│    - 性能监控  - 频率限制  - 日志记录      │
└─────────────────────────────────────────┘
              ↑
┌─────────────────────────────────────────┐
│    旧代码: eventBus.publish()           │ ← 代理到 Bukkit
│    新代码: EventPublisher.publish()     │ ← 推荐
│    备选: Bukkit.getPluginManager()      │ ← 标准
└─────────────────────────────────────────┘
```

### ❌ 不推荐 (旧版 EventBus API)

```java
// 不推荐: 使用 RPGCore EventBus 发布事件（已改为 Bukkit 代理）
RPGCore rpgCore = RPGCore.getInstance();
EventBus eventBus = rpgCore.getEventBus();
eventBus.publish(new MyEvent());

// 不推荐: 使用 RPGCore EventHandler 订阅（仍可用但建议迁移）
eventBus.subscribe(MyEvent.class, handler);
```

### ✅ 推荐 (使用 EventPublisher 管控层)

```java
import cn.guangdian.rpgcore.event.EventPublisher;

// 推荐: 使用 EventPublisher 发布事件（带管控）
EventPublisher.publish(new MyCustomEvent(player, data));

// 推荐: 异步发布（非关键事件）
EventPublisher.publishAsync(new MyCustomEvent(player, data));

// 推荐: 延迟发布
EventPublisher.publishLater(new MyCustomEvent(player, data), 20L);

// 推荐: 使用 @EventHandler 订阅（标准 Bukkit 方式）
@EventHandler
public void onMyEvent(MyCustomEvent event) {
    // 处理事件
}
```

### ✅ 备选 (直接使用 Bukkit)

```java
// 备选: 直接使用 Bukkit（缺少 RPGCore 管控功能）
Bukkit.getPluginManager().callEvent(new MyCustomEvent(player, data));

// 订阅方式相同
@EventHandler
public void onMyEvent(MyCustomEvent event) {
    // 处理事件
}
```

### 自定义事件定义

```java
// 正确: 继承 Bukkit Event
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MyCustomEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Object data;
    
    public MyCustomEvent(Player player, Object data) {
        this.player = player;
        this.data = data;
    }
    
    public Player getPlayer() { return player; }
    public Object getData() { return data; }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
```

### 迁移指南

| 旧代码 (EventBus) | 新代码 (EventPublisher) | 说明 |
|-------------------|-------------------------|------|
| `eventBus.publish(event)` | `EventPublisher.publish(event)` | 推荐，带管控 |
| `eventBus.publish(event)` | `Bukkit.getPluginManager().callEvent(event)` | 备选，标准 Bukkit |
| `eventBus.subscribe(Event.class, handler)` | `@EventHandler public void onEvent(Event e)` | 标准 Bukkit 注解 |
| `extends CoreEvent` | `extends Event` | 继承 Bukkit Event |

**重要说明**:
- EventBus 在 2.0.0 中已改造为 Bukkit 代理模式，旧代码仍可运行
- 新代码建议使用 EventPublisher 获得管控能力（监控、限流、日志）
- CoreEvent 已改为继承 Bukkit Event，与所有插件兼容

**参考文档**:
- [Bukkit Event API](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/package-summary.html)
- [RPGCORE_DEVELOPMENT_STANDARD.md](./RPGCORE_DEVELOPMENT_STANDARD.md) 第7章

---

## 17. 事件位置规范 (v2.0.0 新增)

### 架构原则

RPGCore 2.0.0+ 采用 **"基础设施事件在 Core，业务事件在插件"** 的架构：

```
RPGCore (基础设施)
├── PlayerDataLoadEvent      ✅ 保留
├── PlayerDataSaveEvent      ✅ 保留
├── ModuleEnableEvent        ✅ 保留
└── ...

GuangDianArmorStats (业务插件)
├── PlayerStatsChangedEvent  ✅ 业务事件
├── PlayerHealthChangedEvent ✅ 业务事件
├── PlayerFullHealthEvent    ✅ 业务事件
└── ...

GuangDianPoints (业务插件)
├── PointsTransactionEvent   ✅ 业务事件
└── ...
```

### ❌ 禁止 (在 RPGCore 定义业务事件)

```java
// 禁止: 在 RPGCore 中定义业务相关事件
package cn.guangdian.rpgcore.event.events;

public class PlayerLevelUpEvent extends Event { }  // 应该在 GuangDianClass
public class PointsTransactionEvent extends Event { }  // 应该在 GuangDianPoints
public class GuildCreateEvent extends Event { }  // 应该在 GuangDianGuild
```

### ✅ 正确 (业务事件定义在对应插件)

```java
// 正确: 业务事件定义在对应插件中

// GuangDianClass 插件
package cn.guangdian.classsystem.event;

public class PlayerLevelUpEvent extends Event {
    private final Player player;
    private final int oldLevel;
    private final int newLevel;
    
    public PlayerLevelUpEvent(Player player, int oldLevel, int newLevel) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }
    // ... getters
}

// GuangDianPoints 插件
package cn.guangdian.points.event;

public class PointsTransactionEvent extends Event {
    private final UUID playerId;
    private final TransactionType type;
    private final long amount;
    
    public PointsTransactionEvent(UUID playerId, TransactionType type, long amount) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.type = type;
        this.amount = amount;
    }
    // ... getters
}
```

### 事件分类指南

| 事件类型 | 归属 | 示例 |
|---------|------|------|
| **数据生命周期** | RPGCore | PlayerDataLoadEvent, PlayerDataSaveEvent |
| **模块管理** | RPGCore | ModuleEnableEvent |
| **属性/血量** | GuangDianArmorStats | PlayerStatsChangedEvent, PlayerHealthChangedEvent |
| **等级/经验** | GuangDianClass | PlayerLevelUpEvent, PlayerExpChangeEvent |
| **经济/交易** | GuangDianPoints/Market | PointsTransactionEvent, EconomyTransactionEvent |
| **公会** | GuangDianGuild | GuildEvent, GuildCreateEvent |
| **任务** | GuangDianQuest | QuestEvent, QuestCompleteEvent |
| **NPC** | GuangDianNPC | NPCInteractEvent, NPCCreatedEvent |
| **全息图** | GuangDianHolo | HologramCreatedEvent, HologramDeletedEvent |
| **世界** | GuangDianWorld | WorldCreatedEvent, WorldDeletedEvent |

### 跨插件通信

```java
// 方式1: 订阅其他插件的事件（推荐）
@EventHandler
public void onPlayerLevelUp(cn.guangdian.classsystem.event.PlayerLevelUpEvent event) {
    // 处理升级事件
    Player player = event.getPlayer();
    int newLevel = event.getNewLevel();
    // ...
}

// 方式2: 使用 ServiceRegistry 直接调用
RPGCore rpgCore = RPGCore.getInstance();
if (rpgCore != null) {
    ServiceRegistry registry = rpgCore.getServiceRegistry();
    LevelService levelService = registry.getService(LevelService.class);
    if (levelService != null) {
        int level = levelService.getPlayerLevel(player);
    }
}
```

### 废弃事件迁移指南

RPGCore 中的业务事件已标记为 `@Deprecated`，请按以下方式迁移：

| 废弃事件 (RPGCore) | 新事件位置 | 迁移难度 |
|-------------------|-----------|---------|
| `RpgLevelUpEvent` | `cn.guangdian.classsystem.event.PlayerLevelUpEvent` | 低 |
| `PointsTransactionEvent` | `cn.guangdian.points.event.PointsTransactionEvent` | 低 |
| `RpgEconomyTransactionEvent` | `cn.guangdian.market.event.EconomyTransactionEvent` | 低 |
| `PlayerStatsChangedEvent` | `cn.guangdian.armorstats.event.PlayerStatsChangedEvent` | 低 |
| `PlayerHealthChangedEvent` | `cn.guangdian.armorstats.event.PlayerHealthChangedEvent` | 低 |
| `PlayerFullHealthEvent` | `cn.guangdian.armorstats.event.PlayerFullHealthEvent` | 低 |
| `RpgGuildEvent` | `cn.guangdian.guild.event.GuildEvent` | 低 |
| `RpgQuestEvent` | `cn.guangdian.quest.event.QuestEvent` | 低 |
| `NPCInteractEvent` | `cn.guangdian.npc.event.NPCInteractEvent` | 低 |
| `NPCCreatedEvent` | `cn.guangdian.npc.event.NPCCreatedEvent` | 低 |
| `HologramCreatedEvent` | `cn.guangdian.holo.event.HologramCreatedEvent` | 低 |
| `HologramDeletedEvent` | `cn.guangdian.holo.event.HologramDeletedEvent` | 低 |
| `WorldCreatedEvent` | `cn.guangdian.world.event.WorldCreatedEvent` | 低 |
| `WorldDeletedEvent` | `cn.guangdian.world.event.WorldDeletedEvent` | 低 |

**原因**:
- 符合微内核架构原则：RPGCore 只提供基础设施
- 业务插件自治：每个插件管理自己的事件
- 减少 RPGCore 体积：避免核心膨胀
- 明确职责边界：事件归属清晰

---

## 18. 插件依赖架构决策 (v2.0.0 新增)

### 架构方案对比

| 维度 | 方案A: RPGCore 集中 | 方案B: 插件自治+依赖 (推荐) | 方案C: 反射零依赖 |
|------|-------------------|---------------------------|----------------|
| **架构纯度** | ❌ 违反微内核 | ✅ 符合微内核 | ✅ 符合微内核 |
| **代码复杂度** | ✅ 简单 | ✅ 简单 | ❌ 复杂 |
| **类型安全** | ✅ 安全 | ✅ 安全 | ❌ 不安全 |
| **可维护性** | ❌ RPGCore 膨胀 | ✅ 职责清晰 | ❌ 难调试 |
| **成熟服案例** | ❌ 少见 | ✅ 主流 | ❌ 罕见 |

### 推荐方案：插件自治 + 编译期依赖

**决策理由：**

1. **符合成熟商业服实践**
   - Hypixel、Mineplex 等大型服务器都采用插件自治架构
   - RPGCore 只提供基础设施（数据生命周期、模块管理、ServiceRegistry）

2. **依赖关系显式化是优点**
   ```
   GuangDianBoard 依赖 GuangDianArmorStats
   ↓
   说明：Board 需要 ArmorStats 的数据
   ↓
   这是合理的业务依赖，应该显式声明
   ```

3. **避免过度设计**
   - 反射方案增加了复杂度，收益不大
   - 集中管理导致 RPGCore 成为"上帝类"

### 依赖关系分层

```
RPGCore (基础设施层)
    ↓ 所有插件都依赖
GuangDianArmorStats / GuangDianPoints / GuangDianClass (核心业务层)
    ↓ 显示/界面插件依赖
GuangDianBoard / GuangDianName / GuangDianTab (界面展示层)
```

### 实现规范

**1. build.gradle 声明依赖**

```gradle
dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT'
    compileOnly project(':plugins:RPGCore')
    compileOnly project(':plugins:GuangDianArmorStats')  // 为了事件
    compileOnly files('../GuangDianArmorStats/libs/PlaceholderAPI.jar')
}
```

**2. 代码中检查插件是否启用**

```java
public BoardServiceAdapter(GuangDianBoard plugin) {
    this.plugin = plugin;
    
    // 检查依赖插件是否启用
    boolean armorStatsEnabled = Bukkit.getPluginManager()
        .isPluginEnabled("GuangDianArmorStats");
    
    if (armorStatsEnabled) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logger.info("已订阅 PlayerStatsChangedEvent");
    } else {
        logger.warning("GuangDianArmorStats 未启用，功能受限");
    }
}
```

**3. 事件订阅标准写法**

```java
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
public void onPlayerStatsChanged(PlayerStatsChangedEvent event) {
    // Bukkit 自动处理：如果 ArmorStats 未启用，此监听器不会注册
    UUID playerId = event.getPlayerId();
    // 处理...
}
```

### 跨层通信方式选择

| 场景 | 推荐方式 | 示例 |
|------|---------|------|
| **状态变更通知** | Bukkit Event | 属性变化、等级提升 |
| **数据查询** | ServiceRegistry | 获取玩家当前属性 |
| **复杂操作** | Service 接口 | 转账、创建公会 |
| **配置读取** | 直接访问 | 读取其他插件配置 |

### 禁止：反射方案

```java
// ❌ 禁止：使用反射避免依赖
@EventHandler
public void onEvent(Event event) {
    if (event.getClass().getName().equals("...PlayerStatsChangedEvent")) {
        // 反射获取数据，失去类型安全
    }
}

// ✅ 正确：显式依赖，类型安全
@EventHandler
public void onPlayerStatsChanged(PlayerStatsChangedEvent event) {
    UUID playerId = event.getPlayerId();  // 类型安全
}
```

**参考文档**:
- [RPGCORE_DEVELOPMENT_STANDARD.md](./RPGCORE_DEVELOPMENT_STANDARD.md) 第7章
- [Bukkit Event API](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/package-summary.html)

---

*最后更新: 2026-04-25*
