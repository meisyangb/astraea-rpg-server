---
name: minecraft-rpg-architect
version: 2.4.0
updated: 2026-04-26
changelog:
  - version: 2.4.0
    date: 2026-04-26
    changes:
      - "文档整合：合并 FORBIDDEN_PATTERNS + CODE_TEMPLATES → DEVELOPMENT_GUIDE.md"
      - "文档整合：合并 RPGCORE_SERVICES + MIGRATION_GUIDE → RPGCORE_API_REFERENCE.md"
      - "更新所有文档引用链接"
  - version: 2.3.0
    date: 2026-04-14
    changes:
      - "新增 RPGCore 核心服务：SoundService, ServerService, EntityService"
      - "添加 Paper 1.21.6 弃用 API 的封装方案"
  - version: 2.2.0
    date: 2026-04-14
    changes:
      - "添加基本原则：不可随意回滚、CMD执行、先构建验证后提交"
      - "更新构建命令：PowerShell → CMD"
      - "更新依赖版本：HikariCP 5.1.0, OkHttp 4.12.0, SLF4J 2.0.9"
      - "更新迁移状态：RPGCore 已完成 Paper 1.21.6 全面升级"
  - version: 2.1.0
    date: 2026-04-10
    changes:
      - "更新违规扫描结果: 52处 Bukkit.getScheduler() 待修复"
      - "更新 MIGRATION STATUS: 24个插件架构评估完成"
      - "添加知识库导航链接"
      - "更新插件数量: 24个 GuangDian* 插件"
  - version: 2.0.0
    date: 2025-04
    changes:
      - "初始版本发布"
      - "定义 FORBIDDEN PATTERNS"
      - "建立 RPGCore 架构规范"
description: >
  Astraea RPG 阿斯特瑞亚服务器 — Minecraft Paper 1.21.6 RPG插件架构师。
  专注于 GuangDian* 插件体系的高性能设计、RPGCore统一服务规范、MythicMobs配置、
  装备/副本/技能/NPC系统开发与优化。
  当用户请求：开发插件、配置系统、设计装备、编写MythicMobs配置、优化性能时触发。
---

# Astraea RPG 阿斯特瑞亚 — 架构师规范手册 v2.3

> **AI使用说明**: 每次生成代码前，必须对照 FORBIDDEN PATTERNS 检查清单执行自我审查。
> 任何与禁止模式匹配的代码，必须主动拒绝并给出正确替代方案。

---

## ⚠️ 基本原则 (必须遵守)

1. **不可随意回滚** - 每次提交必须有明确的理由和测试
2. **脚本采用 CMD 执行** - 禁止使用 PowerShell 执行构建脚本
3. **先构建验证后提交** - 禁止未验证就提交

---

## 环境基线

| 项目 | 值 |
|------|---|
| 服务端 | Paper 1.21.6 |
| JDK | JDK 21 (tools/jdk-21.0.10+7) |
| 构建工具 | Gradle 9.4.0 |
| 插件数量 | 24个 GuangDian* + RPGCore + MythicMobs |
| Adventure | 4.26.1 |
| Caffeine | 3.1.8 |
| 项目根目录 | `e:\原创RPG服务端` |
| 服务器名称 | Astraea RPG 阿斯特瑞亚 (星辰女神) |

---

## ❌ FORBIDDEN PATTERNS — AI生成代码时必须主动拒绝

> 以下模式一旦出现，立即停止输出，给出正确替代，并说明原因。

### 调度器禁止项

```
❌ new BukkitRunnable() { ... }.runTaskTimer(...)
❌ new BukkitRunnable() { ... }.runTaskLater(...)
❌ Bukkit.getScheduler().scheduleSyncRepeatingTask(...)
❌ Bukkit.getScheduler().runTaskAsynchronously(...)  // 异步任务
❌ task.cancel()  // 来自BukkitTask
```

```java
// ✅ 正确替代 - 异步任务使用 AsyncScheduler (Paper 1.21+)
Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> {
    // 异步执行
});

// 异步延迟任务 (毫秒)
Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> {
    // 延迟执行
}, 5000, java.util.concurrent.TimeUnit.MILLISECONDS);

// 异步定时任务 (毫秒)
Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> {
    // 定期执行
}, 0, 60000, java.util.concurrent.TimeUnit.MILLISECONDS);

// ✅ 正确替代 - 同步任务使用 SyncScheduler
RPGCore rpgCore = RPGCore.getInstance();
SyncScheduler scheduler = rpgCore.getScheduler();
long id = scheduler.runSyncLater(() -> { ... }, 50L);
scheduler.cancelTask(id);
```

### 颜色服务禁止项

```
❌ ChatColor.RED + "text"
❌ player.sendMessage("§c错误消息")
❌ player.sendMessage("&a成功消息")
```

```java
// ✅ 正确替代 - MiniMessage
MiniMessageService mm = MiniMessageService.getInstance();
player.sendMessage(mm.red("错误消息"));
player.sendMessage(mm.green("成功消息"));
player.sendMessage(mm.colorize("<yellow>普通消息<reset> <red>错误"));
```

### 外部服务禁止项

```
❌ LuckPermsProvider.get()
❌ luckPerms.getUserManager().getUser(...)  // 直接调用
❌ PlaceholderAPI.setPlaceholders(player, text)
❌ placeholders.unregister()  // PlaceholderExpansion 没有此方法！
```

```java
// ✅ 正确替代
RPGCore rpgCore = RPGCore.getInstance();
ExternalServiceIntegration externalServices = rpgCore.getExternalServices();

if (externalServices.isLuckPermsEnabled()) {
    String prefix = externalServices.getPlayerPrefix(player);
}
if (externalServices.isPlaceholderAPIEnabled()) {
    String parsed = externalServices.parsePlaceholders(player, text);
}
PlaceholderAPI.unregisterExpansion(expansion);  // 注销占位符的唯一正确方式
```

### MythicMobs禁止项

```
❌ new NamespacedKey("mythicmobs", "item")   // 旧版Key，已废弃
❌ meta.getPersistentDataContainer().get(new NamespacedKey("mythicmobs", "item"), ...)
```

```java
// ✅ 正确替代 (新版本使用 "type")
NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
String typeId = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
```

### 插件结构禁止项

```
❌ class MyPlugin extends JavaPlugin  // 新插件不允许直接继承JavaPlugin
❌ Bukkit.getPlugin("RPGCore")  // 不允许通过名称获取核心
❌ RPGCore core = (RPGCore) Bukkit.getPlugin("RPGCore")  // 同上
```

```java
// ✅ 正确替代
class MyPlugin extends AbstractRPGPlugin  // 必须继承插件基类
RPGCore rpgCore = RPGCore.getInstance()  // 单例获取
```

### API版本禁止项 (1.21.6迁移)

```
❌ ArmorStand 用于全息图显示
❌ player.sendMessage(String) // 使用 Component
❌ PlayerInteractAtEntityEvent 用于NPC交互
```

```java
// ✅ 正确替代
TextDisplay  // 全息图用TextDisplay
player.sendMessage(Component.text("text").color(NamedTextColor.RED))  // Adventure API
// NPC交互用ProtocolLib数据包拦截
```

### 日志服务禁止项

```
❌ System.out.println("调试信息")
❌ Bukkit.getLogger().info("消息")
```

```java
// ✅ 正确替代
RPGCore rpgCore = RPGCore.getInstance();
GameLogger logger = rpgCore.getGameLogger();
logger.info("消息");
logger.warning("警告");
```

### 脚本执行禁止项

```
❌ PowerShell 执行脚本
❌ & "D:\gradle\gradle-9.4.0\bin\gradle.bat" build  // PowerShell 方式
```

```cmd
✅ 正确替代 - CMD 执行
cd /d e:\原创RPG服务端
set JAVA_HOME=e:\原创RPG服务端\tools\jdk-21.0.10+7
D:\gradle\gradle-9.4.0\bin\gradle.bat build --no-configuration-cache -x test
```

---

## ✅ 核心架构规范

### 新插件开发必须遵守的结构

```java
// 1. 主类继承 AbstractRPGPlugin
public class MyPlugin extends AbstractRPGPlugin {
    protected MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        miniMessage = MiniMessageService.getInstance();
        getLogger().info(getPluginName() + " 已启动");
    }

    @Override
    protected void onPluginDisable() {
        scheduler.cancelAllTasks();
        serviceAdapter.unregister();
        getLogger().info(getPluginName() + " 已关闭");
    }

    @Override protected String getPluginName() { return "MyPlugin"; }
}

// 2. 服务适配器实现ServiceRegistry注册
public class MyServiceAdapter implements MyService {
    public MyServiceAdapter(MyPlugin plugin) {
        RPGCore rpgCore = RPGCore.getInstance();
        rpgCore.getServiceRegistry().registerService(MyService.class, this);
    }
    public void unregister() {
        RPGCore.getInstance().getServiceRegistry().unregisterService(MyService.class);
    }
}
```

### SyncScheduler 使用规范

```java
// 获取: rpgCore.getScheduler() 或 AbstractRPGPlugin 中直接用 scheduler
long id1 = scheduler.runSyncRepeating(task, 0L, 20L);   // 每秒
long id2 = scheduler.runSyncLater(task, 100L);           // 5秒后
scheduler.runAsync(heavyTask);                            // 异步
scheduler.cancelTask(id1);      // 取消单个
scheduler.cancelAllTasks();     // 插件卸载时
```

### AsyncScheduler 使用规范 (Paper 1.21+)

```java
// 异步立即任务
Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> {
    // 异步执行的任务
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

### ExternalServiceIntegration 使用规范

```java
// 总是先检查可用性
RPGCore rpgCore = RPGCore.getInstance();
ExternalServiceIntegration externalServices = rpgCore.getExternalServices();

if (externalServices.isLuckPermsEnabled()) {
    String prefix = externalServices.getPlayerPrefix(player);
}
if (externalServices.isVaultEnabled()) {
    double balance = externalServices.getBalance(player);
}
if (externalServices.isPlaceholderAPIEnabled()) {
    String parsed = externalServices.parsePlaceholders(player, text);
}
```

---

## 📋 MIGRATION STATUS — 迁移状态追踪

| 插件 | 迁移状态 | 说明 |
|------|---------|------|
| **RPGCore** | ✅ 已完成 | Paper 1.21.6 全面升级完成 |
| **GuangDianArmorStats** | ✅ 已完成 | jdk.incubator.vector 已修复 |
| **GuangDianPoints** | ✅ 已完成 | 使用 PlayerLifecycleManager + DataHandler |
| **GuangDianMarket** | ✅ 已完成 | 使用 PlayerLifecycleManager + DataHandler |
| **GuangDianQuest** | ✅ 已完成 | 使用 PlayerLifecycleManager + DataHandler |
| 其他 GuangDian* | ⏳ 待迁移 | 子插件暂不处理 |

---

## 📦 构建命令速查 (CMD)

> **唯一构建方法**: 必须使用 CMD 执行

```cmd
cd /d e:\原创RPG服务端
set JAVA_HOME=e:\原创RPG服务端\tools\jdk-21.0.10+7
D:\gradle\gradle-9.4.0\bin\gradle.bat build --no-configuration-cache -x test
```

### 构建输出位置
```
plugins/{插件名}/build/libs/{插件名}-1.0.0.jar
```

---

## 🔍 代码审查清单 — 每次提交前自检

在提交任何代码前，确认以下全部为 ✅：

### 基本原则检查
- [ ] **不可随意回滚** - 提交有明确的理由
- [ ] **CMD 执行** - 使用 CMD 而非 PowerShell
- [ ] **先构建验证** - 已执行构建并通过

### 结构检查
- [ ] 新插件主类继承 `AbstractRPGPlugin` 而非 `JavaPlugin`
- [ ] 已创建并注册 `ServiceAdapter`
- [ ] `onPluginDisable()` 调用了 `scheduler.cancelAllTasks()`
- [ ] `onPluginDisable()` 调用了 `serviceAdapter.unregister()`

### 调度器检查
- [ ] 异步任务使用 `Bukkit.getAsyncScheduler()`
- [ ] 同步任务使用 `SyncScheduler`
- [ ] 无 `new BukkitRunnable()` 调用
- [ ] 无 `Bukkit.getScheduler().runTaskAsynchronously()` 调用

### 颜色服务检查
- [ ] 无 `ChatColor.` 调用
- [ ] 无 `§` 颜色码
- [ ] 使用 `MiniMessageService` 或 `Component`

### 外部服务检查
- [ ] 无 `LuckPermsProvider.get()` 直接调用
- [ ] 无 `PlaceholderAPI.setPlaceholders()` 直接调用
- [ ] 占位符注销使用 `PlaceholderAPI.unregisterExpansion()`

### API版本检查
- [ ] 无 `ArmorStand` 用于显示目的
- [ ] MythicMobs物品检测使用 `mythicmobs:type`

### RPGCore 服务检查
- [ ] 音效使用 `SoundService` 而非 `Sound.valueOf()`
- [ ] 服务器重启使用 `ServerService` 而非 `Bukkit.spigot().restart()`
- [ ] 实体碰撞使用 `EntityService` 而非 `setCollisionCancelled()`

---

## 📦 RPGCore 核心服务 (v1.2.0+)

RPGCore 提供了封装 Paper 1.21.6 弃用 API 的核心服务：

### SoundService - 音效服务
```java
// 获取服务
SoundService soundService = RPGCore.getInstance().getSoundService();

// 播放音效（解决 Sound.valueOf() 弃用）
soundService.playSound(player, "SUCCESS", 1.0f, 1.0f);
soundService.playSound(location, "CLICK", 0.5f, 1.2f);
soundService.broadcastSound("LEVEL_UP", 1.0f, 1.0f);

// 停止音效
soundService.stopSound(player, "MUSIC");
soundService.stopAllSounds(player);
```

**支持的别名**: `CLICK`, `SUCCESS`, `ERROR`, `PICKUP`, `HIT`, `COIN`, `TELEPORT`, `SPELL` 等

### ServerService - 服务器服务
```java
// 获取服务
ServerService serverService = RPGCore.getInstance().getServerService();

// 重启服务器（解决 Bukkit.spigot().restart() 弃用）
serverService.restart();

// 关闭服务器
serverService.shutdown();

// TPS 监控
String tps1m = serverService.getFormattedTPS(0);

// 内存监控
ServerService.MemoryInfo memory = serverService.getMemoryInfo();
long usedMB = memory.getUsedMemoryMB();
```

### EntityService - 实体服务
```java
// 获取服务
EntityService entityService = RPGCore.getInstance().getEntityService();

// 设置碰撞状态（解决 setCollisionCancelled() 弃用）
entityService.setCollisionCancelled(entity, true);

// 处理载具碰撞
@EventHandler
public void onCollision(VehicleEntityCollisionEvent event) {
    entityService.handleVehicleCollision(event, true);
}

// 安全传送
boolean success = entityService.teleportSafely(entity, location);

// 距离计算
boolean inRange = entityService.isInRange(entity1, entity2, 10.0);
```

**详细文档**: [.trae/docs/reference/RPGCORE_API_REFERENCE.md](../../docs/reference/RPGCORE_API_REFERENCE.md)

---

## 📁 配置文件路径速查

| 配置 | 路径 |
|------|------|
| RPGCore | `server/plugins/RPGCore/config.yml` |
| 装备属性 | `plugins/GuangDianArmorStats/src/main/resources/config.yml` |
| 技能系统 | `plugins/GuangDianArmorStats/src/main/resources/skills.yml` |
| BOSS属性 | `plugins/GuangDianArmorStats/src/main/resources/boss_stats.yml` |
| 伤害公式 | `plugins/GuangDianArmorStats/src/main/resources/damage_formula.yml` |
| 宝石系统 | `plugins/GuangDianArmorStats/src/main/resources/gems.yml` |
| 锻造图纸 | `plugins/GuangDianForge/src/main/resources/recipes.yml` |
| NPC配置 | `plugins/GuangDianNPC/src/main/resources/npcs.yml` |

---

*详细示例见 examples.md | 完整配置参考见 reference.md*
