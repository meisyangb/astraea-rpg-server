# Astraea RPG 禁止模式清单

> 所有开发必须遵守的禁止模式，违反将导致代码被拒绝
> **版本: 1.3.0 | 更新: 2026-04-23**

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

## 9. 缓存服务禁止项

### ❌ 禁止 (ConcurrentHashMap 简单缓存)
```java
Map<String, Object> cache = new ConcurrentHashMap<>();
cache.put(key, value);
```

### ✅ 正确 (Caffeine)
```java
RPGCore rpgCore = RPGCore.getInstance();
CacheProvider cacheProvider = rpgCore.getCacheProvider();

LoadingCache<String, Object> cache = cacheProvider.getLoadingCache("myCache",
    k -> loadFromDatabase(k));

Object value = cache.get(key);
cache.invalidate(key);
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

## 12. RPGCore 统一服务禁止项 (v1.2.0 新增)

### ❌ 禁止 (重复实现 RPGCore 已提供的功能)

```java
// 禁止: 自己实现 colorize() 方法
private Component colorize(String text) {
    return miniMessage.deserialize(text.replace("&a", "<green>")...);
}

// 禁止: 直接使用 ConcurrentHashMap 存储玩家数据
private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

// 禁止: 手动加载/保存 YAML 文件
YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
yaml.save(file);

// 禁止: 自己实现冷却管理
private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

// 禁止: 直接创建 PlaceholderExpansion
new MyPlaceholderExpansion().register();
```

### ✅ 正确 (使用 RPGCore 统一服务)

```java
// 正确: 使用 UnifiedMessageService
UnifiedMessageService msg = UnifiedMessageService.getInstance();
Component component = msg.colorize("&a成功消息");
msg.sendMessage(player, "&6金色文字");

// 正确: 继承 PlayerDataService
public class MyDataService extends PlayerDataService<MyData> {
    // 自动处理缓存、保存、序列化
}

// 正确: 使用 YamlDataStore
YamlDataStore store = YamlDataStore.getInstance();
Map<String, Object> data = store.load(file);
store.save(file, data);

// 正确: 使用 CooldownManager
CooldownManager cooldown = CooldownManager.getInstance();
cooldown.setCooldown(playerUUID, "action", 10000);

// 正确: 使用 PlaceholderService
PlaceholderService placeholder = PlaceholderService.getInstance();
placeholder.register("my_value", (player, params) -> "value");
```

**原因**: 
- 避免 2500+ 行重复代码
- 统一管理,便于维护
- 内置性能优化和异常处理
- 符合微内核架构原则

**参考文档**:
- [UNIFIED_SERVICES_OVERVIEW.md](../docs/RPGCore/UNIFIED_SERVICES_OVERVIEW.md)
- [ARCHITECTURE_UPGRADE_GUIDE.md](../docs/RPGCore/ARCHITECTURE_UPGRADE_GUIDE.md)

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

// 方式2: 使用 RPGCore TextStripper (推荐 - 同时处理 MiniMessage 和传统颜色码)
import cn.guangdian.rpgcore.util.TextStripper;

String text1 = TextStripper.stripAll(input);           // 剥离所有格式代码
String text2 = TextStripper.stripLegacyColors(input);  // 仅剥离传统颜色码
String text3 = TextStripper.stripMiniMessageTags(input);// 仅剥离 MiniMessage 标签
```

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
- TextStripper 额外处理 & 和 § 传统颜色码
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

*最后更新: 2026-04-23*
