# Astraea RPG 禁止模式清单

> 所有开发必须遵守的禁止模式，违反将导致代码被拒绝
> **版本: 1.2.0 | 更新: 2026-04-16**

---

## ⚠️ 基本原则

1. **不可随意回滚** - 每次提交必须有明确的理由和测试
2. **脚本采用 CMD 执行** - 禁止使用 PowerShell 执行构建脚本
3. **先构建验证后提交** - 禁止未验证就提交

---

## 1. 调度器禁止项

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

### ✅ 正确 (Paper 1.21.6 AsyncScheduler)
```java
// 异步任务 - 使用 AsyncScheduler
Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> {
    // 异步执行的任务
});

// 异步延迟任务 (毫秒)
Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> {
    // 延迟执行
}, 5000, java.util.concurrent.TimeUnit.MILLISECONDS);

// 异步定时任务 (毫秒)
Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> {
    // 定期执行
}, 0, 60000, java.util.concurrent.TimeUnit.MILLISECONDS);

// 同步任务 - 使用 RPGCore SyncScheduler
RPGCore rpgCore = RPGCore.getInstance();
SyncScheduler scheduler = rpgCore.getScheduler();

long taskId = scheduler.runSyncLater(() -> { }, 50L);
long repeatingId = scheduler.runSyncRepeating(() -> { }, 0L, 20L);
scheduler.runAsync(() -> { });
scheduler.cancelTask(taskId);
```

---

## 2. RPGCore 获取禁止项

### ❌ 禁止
```java
Bukkit.getPlugin("RPGCore")
Bukkit.getPluginManager().getPlugin("RPGCore")
(RPGCore) Bukkit.getPlugin("RPGCore")
```

### ✅ 正确
```java
RPGCore rpgCore = RPGCore.getInstance();
if (rpgCore != null) { }
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

## 4. 外部服务调用禁止项

### ❌ 禁止
```java
LuckPermsProvider.get()
luckPerms.getUserManager().getUser(player.getUniqueId())
PlaceholderAPI.setPlaceholders(player, text)
expansion.unregister()
```

### ✅ 正确
```java
RPGCore rpgCore = RPGCore.getInstance();
ExternalServiceIntegration externalServices = rpgCore.getExternalServices();

if (externalServices.isLuckPermsEnabled()) {
    String prefix = externalServices.getPlayerPrefix(player);
}
if (externalServices.isPlaceholderAPIEnabled()) {
    String parsed = externalServices.parsePlaceholders(player, text);
}
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
public class MyPlugin extends JavaPlugin { }
```

### ✅ 正确
```java
public class MyPlugin extends AbstractRPGPlugin {
    @Override protected void onPluginEnable() { }
    @Override protected void onPluginDisable() { }
    @Override protected String getPluginName() { return "MyPlugin"; }
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

*最后更新: 2026-04-16*
