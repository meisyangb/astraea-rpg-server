---
name: minecraft-rpg-architect
version: 2.1.0
updated: 2026-04-10
changelog:
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

# Astraea RPG 阿斯特瑞亚 — 架构师规范手册 v2.0

> **AI使用说明**: 每次生成代码前，必须对照 FORBIDDEN PATTERNS 检查清单执行自我审查。
> 任何与禁止模式匹配的代码，必须主动拒绝并给出正确替代方案。

---

## 环境基线

| 项目 | 值 |
|------|---|
| 服务端 | Paper 1.21.6 |
| JDK | JDK 21 (tools/jdk-21.0.10+7) |
| 构建工具 | Gradle 9.4.0 |
| 插件数量 | 24个 GuangDian* + RPGCore + MythicMobs |
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
❌ task.cancel()  // 来自BukkitTask
```

```java
// ✅ 正确替代
long taskId = scheduler.runSyncRepeating(() -> { ... }, delay, period);
scheduler.cancelTask(taskId);
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
externalServices.getPlayerPrefix(player)
externalServices.parsePlaceholders(player, text)
PlaceholderAPI.unregisterExpansion(expansion)  // 注销占位符的唯一正确方式
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
❌ § 颜色码 (ChatColor.RED + "text")
❌ PlayerInteractAtEntityEvent 用于NPC交互
```

```java
// ✅ 正确替代
TextDisplay  // 全息图用TextDisplay
Component.text("text").color(NamedTextColor.RED)  // Adventure API
// NPC交互用ProtocolLib数据包拦截
```

---

## ✅ 核心架构规范

### 新插件开发必须遵守的结构

```java
// 1. 主类继承 AbstractRPGPlugin
public class MyPlugin extends AbstractRPGPlugin {
    @Override protected void onPluginEnable() { /* 业务逻辑 */ }
    @Override protected void onPluginDisable() { /* 清理逻辑 */ }
    @Override protected String getPluginName() { return "MyPlugin"; }
    // 自动注入: rpgCore, externalServices, scheduler, exceptionHandler
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

// 3. 占位符独立实现 (不依赖RPGCore)
public class MyPlaceholder extends PlaceholderExpansion {
    @Override public String getIdentifier() { return "myplugin"; }
    @Override public boolean persist() { return true; }
    // 在主类 onEnable() 注册: new MyPlaceholder(this).register();
    // 注销: PlaceholderAPI.unregisterExpansion(this);  ← 注意是静态方法
}
```

### SyncScheduler 使用规范

```java
// 获取: rpgCore.getScheduler() 或 AbstractRPGPlugin 中直接用 scheduler
long id1 = scheduler.runSyncRepeating(task, 0L, 20L);   // 每秒
long id2 = scheduler.runSyncLater(task, 100L);           // 5秒后
long id3 = scheduler.runAsync(heavyTask);                // 异步
scheduler.cancelTask(id1);      // 取消单个
scheduler.cancelAllTasks();     // 插件卸载时
```

### ExternalServiceIntegration 使用规范

```java
// 总是先检查可用性
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

### 服务获取规范 (跨插件调用)

```java
// 通过ServiceRegistry获取其他插件服务
ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
PointsService points = registry.getService(PointsService.class);
if (points != null) {  // 总是做null检查
    points.addPoints(playerId, amount);
}
```

---

## 📋 MIGRATION STATUS — 迁移状态追踪

> 每次完成迁移后更新此表。AI在生成代码时参考此表判断插件当前状态。

| 插件 | 迁移状态 | 待办事项 |
|------|---------|---------|
| **GuangDianArmorStats** | ✅ 已完成 | 使用 PlayerLifecycleManager + DataHandler |
| **GuangDianPoints** | ✅ 已完成 | 使用 PlayerLifecycleManager + DataHandler |
| **GuangDianMarket** | ✅ 已完成 | 使用 PlayerLifecycleManager + DataHandler |
| **GuangDianQuest** | ✅ 已完成 | 使用 PlayerLifecycleManager + DataHandler |
| **GuangDianName** | ✅ 已完成 | 使用 PlayerLifecycleManager + DataHandler |
| **GuangDianBoard** | ✅ 已完成 | 使用 PlayerLifecycleManager + DataHandler |
| **GuangDianTab** | ⏳ 待迁移 | 52处 Bukkit.getScheduler() 违规 |
| **GuangDianNPC** | ⏳ 待迁移 | ProtocolLib集成确认 |
| **GuangDianMobHealth** | ⏳ 待迁移 | TextDisplay确认 |
| **GuangDianForge** | ⏳ 待迁移 | 调度器迁移 |
| **GuangDianCaveFu** | ⏳ 待迁移 | 调度器迁移 |
| **GuangDianDropControl** | ⏳ 待迁移 | DropControlService注册 |
| **GuangDianCleaner** | ⏳ 待迁移 | CleanerService注册 |
| **GuangDianItemTrigger** | ⏳ 待迁移 | ItemTriggerService注册 |
| **GuangDianWorld** | ⏳ 待迁移 | 待评估 |
| **GuangDianLocation** | ⏳ 待迁移 | LocationService注册 |
| **GuangDianDecompose** | ⏳ 待迁移 | 待评估 |
| **GuangDianGift** | ⏳ 待迁移 | 待评估 |
| **GuangDianMenu** | ⏳ 待迁移 | GUI事件处理 |
| **GuangDianChat** | ⏳ 待迁移 | ChatService注册 |
| **GuangDianMCP** | ⏳ 待迁移 | MCP主控特殊处理 |
| **GuangDianGuild** | ⏳ 待评估 | 仅通知逻辑，可跳过 |
| **GuangDianMarriage** | ⏳ 待评估 | 仅通知逻辑，可跳过 |
| **GuangDianHolo** | ⏳ 待评估 | 仅可见性更新 |
| **GuangDianTrade** | ⏳ 待评估 | 仅状态清理 |
| **RPGCore** | ✅ 核心 | — |

---

### 已创建的统一服务 (RPGCore)

| 服务 | 状态 | 用途 |
|------|------|------|
| PlayerLifecycleManager | ✅ 已完成 | 统一玩家登录/退出数据加载 |
| AbstractPlayerDataHandler | ✅ 已完成 | 数据处理器基类 |
| DisplayService | ✅ 已完成 | 统一显示服务 |
| UnifiedDataManager | ✅ 已完成 | 统一数据管理 |
| ExternalServiceIntegration | ✅ 已完成 | 统一 LuckPerms/Vault/PlaceholderAPI |

---

## 🐛 KNOWN BUGS LOG — 已知问题知识库

> 每次踩坑后必须立即追加此列表。格式: `[日期] 问题描述 → 根因 → 正确解法`

### [2025-04] PlaceholderExpansion 无 unregister() 方法
- **症状**: 调用 `expansion.unregister()` 编译报错或运行时 NoSuchMethodError
- **根因**: PlaceholderExpansion 类从未有过实例方法 unregister()
- **正确解法**: `PlaceholderAPI.unregisterExpansion(expansion)` (静态方法)
- **影响范围**: 所有实现 PlaceholderExpansion 的插件

### [2025-04] MythicMobs PDC Key 变更
- **症状**: 获取MythicMobs物品类型返回null
- **根因**: 新版本将 PDC Key 从 `mythicmobs:item` 改为 `mythicmobs:type`
- **正确解法**: `new NamespacedKey("mythicmobs", "type")`
- **兼容方案**: 同时检查两个Key，优先使用新Key

### [模板行] 新问题追加格式
- **症状**: 
- **根因**: 
- **正确解法**: 
- **影响范围**: 

---

## 🔍 代码审查清单 — 每次提交前自检

在提交任何代码前，确认以下全部为 ✅：

### 结构检查
- [ ] 新插件主类继承 `AbstractRPGPlugin` 而非 `JavaPlugin`
- [ ] 已创建并注册 `ServiceAdapter`
- [ ] `onPluginDisable()` 调用了 `scheduler.cancelAllTasks()`
- [ ] `onPluginDisable()` 调用了 `serviceAdapter.unregister()`

### 调度器检查
- [ ] 无 `new BukkitRunnable()` 调用
- [ ] 无 `Bukkit.getScheduler()` 调用
- [ ] 所有定时任务ID已保存，并在disable时取消

### 外部服务检查
- [ ] 无 `LuckPermsProvider.get()` 直接调用
- [ ] 无 `PlaceholderAPI.setPlaceholders()` 直接调用
- [ ] 占位符注销使用 `PlaceholderAPI.unregisterExpansion()`

### API版本检查
- [ ] 无 `ArmorStand` 用于显示目的
- [ ] 无 `§` 颜色码（使用Adventure API）
- [ ] MythicMobs物品检测使用 `mythicmobs:type` 而非 `mythicmobs:item`

### 性能检查
- [ ] 玩家数据有缓存层（TTLCacheManager）
- [ ] 显示系统有脏标记/防抖机制
- [ ] 无在主线程执行的数据库I/O

---

## 📦 构建命令速查

> **唯一构建方法**: 详见 [.trae/docs/reference/BUILD_GUIDE.md](../../../.trae/docs/reference/BUILD_GUIDE.md)
> 
> 以下命令必须在项目根目录执行：

```powershell
# 1. 设置环境
cd e:\原创RPG服务端
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"

# 2. 构建所有插件 (唯一标准命令)
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test

# 3. 清理并构建
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" clean build --no-configuration-cache -x test
```

### 构建输出位置
```
plugins/{插件名}/build/libs/{插件名}-1.0.0.jar
```

### 依赖文件要求
确保 `plugins/RPGCore/libs/` 包含以下文件：
- `paper-api.jar`
- `PlaceholderAPI.jar`
- `LuckPerms.jar`
- `Vault.jar`
- `ProtocolLib.jar`

---

## 🏗️ 迁移优先级 (P1 → P3)

### P1 — 高影响，立即迁移 (本周)
1. **GuangDianArmorStats** — 核心战斗系统，BukkitRunnable风险最高
2. **GuangDianPoints** — 经济系统，LuckPerms直接调用需修复
3. **GuangDianGuild** — EventBus集成，影响多个下游插件

### P2 — 中影响，计划迁移 (本月)
4. GuangDianTab / GuangDianChat / GuangDianMobHealth
5. GuangDianForge / GuangDianCaveFu / GuangDianMarket
6. GuangDianHolo (ArmorStand → TextDisplay 迁移)

### P3 — 低影响，排期迁移
7. GuangDianWorld / GuangDianDecompose / GuangDianGift
8. GuangDianMarriage / GuangDianTrade / GuangDianMenu

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

## 🎯 PlaceholderAPI 占位符速查

| 插件 | 前缀 | 示例 |
|------|------|------|
| ArmorStats | `%gdrpg_%` | `%gdrpg_attack%`, `%gdrpg_health%` |
| Points | `%gdpoints_%` | `%gdpoints_balance%`, `%gdpoints_rank%` |
| CaveFu | `%gdcave_%` | `%gdcave_level%`, `%gdcave_owner%` |
| Guild | `%gdguild_%` | `%gdguild_name%`, `%gdguild_rank%` |
| Forge | `%forge_%` | `%forge_level%`, `%forge_exp%` |
| Marriage | `%gdmarriage_%` | `%gdmarriage_partner%` |
| Name | `%gdname_%` | `%gdname_prefix%`, `%gdname_suffix%` |

---

*详细示例见 examples.md | 完整配置参考见 reference.md*
