# Minecraft RPG 配置参考手册

## 统一服务架构

### RPGCore 核心框架
```
plugins/RPGCore/src/main/java/cn/guangdian/rpgcore/
├── api/
│   ├── ServiceRegistry.java         # 服务注册中心 (21个服务)
│   ├── CacheProvider.java           # 缓存提供者接口
│   ├── AsyncExecutor.java           # 统一异步执行器
│   ├── SyncScheduler.java           # 统一同步调度器 (替代BukkitRunnable)
│   ├── EventBus.java                # 事件总线
│   ├── ConfigManager.java           # 统一配置管理
│   ├── ExceptionHandler.java        # 统一异常处理
│   └── PluginLifecycleManager.java  # 插件生命周期管理
├── integration/
│   ├── ExternalServiceIntegration.java   # 外部服务集成接口
│   └── ExternalServiceIntegrationImpl.java # LuckPerms/Vault/PAPI统一管理
├── scheduler/
│   └── UnifiedSchedulerImpl.java    # 统一调度器实现
├── lifecycle/
│   └── PluginLifecycleManagerImpl.java # 生命周期管理实现
├── config/
│   └── ConfigManagerImpl.java       # 配置管理实现
├── exception/
│   └── ExceptionHandlerImpl.java    # 异常处理实现
├── plugin/
│   └── AbstractRPGPlugin.java       # 插件基类 (推荐继承)
├── cache/
│   └── TTLCacheManager.java         # 高性能TTL缓存 (LinkedHashMap LRU O(1))
├── concurrency/
│   ├── PlayerLockManager.java       # 玩家操作锁 (统一锁管理, 死锁预防)
│   ├── LockStats.java               # 锁统计
│   └── LockTimeoutException.java    # 锁超时异常
├── monitor/
│   ├── PerformanceMonitor.java      # 性能监控
│   └── PerformanceMetrics.java      # 性能指标
├── event/
│   ├── SimpleEventBus.java          # 高性能事件总线 (注册时排序)
│   └── events/                      # 自定义事件
│       ├── PointsTransactionEvent   # 点券交易事件
│       ├── PlayerStatsChangedEvent  # 属性变更事件
│       ├── RpgMobKillEvent          # 怪物击杀事件
│       ├── RpgLevelUpEvent          # 升级事件
│       └── RpgSkillCastEvent        # 技能释放事件
└── service/
    └── api/                         # 服务接口定义 (21个)
        ├── PointsService
        ├── StatsService
        ├── SkillService
        ├── GuildService
        ├── CaveService
        ├── ForgeService
        ├── DropControlService
        ├── CleanerService
        ├── BoardService
        ├── ChatService
        ├── TabService
        ├── DisplayService
        ├── TradeService
        ├── MarketService
        ├── MenuService
        ├── MarriageService
        ├── ItemTriggerService
        ├── QuestService
        ├── LocationService
        ├── NPCService
        └── ConfigService
```

---

## 核心服务层

### ExternalServiceIntegration 外部服务集成

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `getLuckPerms()` | `Optional<LuckPerms>` | 获取 LuckPerms API |
| `getPlayerPrefix(Player)` | `String` | 获取玩家前缀称号 |
| `getPlayerSuffix(Player)` | `String` | 获取玩家后缀 |
| `getPlayerPrimaryGroup(Player)` | `String` | 获取玩家主权限组 |
| `getBalance(Player)` | `double` | 获取玩家余额 (Vault) |
| `deposit(Player, double)` | `boolean` | 存款 |
| `withdraw(Player, double)` | `boolean` | 取款 |
| `parsePlaceholders(Player, String)` | `String` | 解析占位符 |
| `isLuckPermsEnabled()` | `boolean` | LuckPerms 是否可用 |
| `isVaultEnabled()` | `boolean` | Vault 是否可用 |
| `isPlaceholderAPIEnabled()` | `boolean` | PAPI 是否可用 |
| `getExternalServiceStatus()` | `String` | 获取服务状态字符串 |

### SyncScheduler 统一调度器

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `runSync(Runnable)` | `void` | 同步执行 |
| `runSyncLater(Runnable, long)` | `long` | 延迟同步执行，返回任务ID |
| `runSyncRepeating(Runnable, long, long)` | `long` | 重复同步执行，返回任务ID |
| `runAsync(Runnable)` | `long` | 异步执行 |
| `runAsyncLater(Runnable, long)` | `long` | 延迟异步执行 |
| `runAsyncRepeating(Runnable, long, long)` | `long` | 重复异步执行 |
| `cancelTask(long)` | `void` | 取消任务 |
| `cancelAllTasks()` | `void` | 取消所有任务 |
| `getActiveTaskCount()` | `int` | 获取活跃任务数 |

### ConfigManager 统一配置管理

| 方法 | 说明 |
|------|------|
| `get(String pluginName, String key, T defaultValue)` | 获取配置值 |
| `set(String pluginName, String key, Object value)` | 设置配置值 |
| `reload(String pluginName)` | 重载配置 |
| `reloadAll()` | 重载所有配置 |
| `save(String pluginName)` | 保存配置 |
| `saveAll()` | 保存所有配置 |

### ExceptionHandler 统一异常处理

| 方法 | 说明 |
|------|------|
| `safeCall(Supplier<T>, T defaultValue)` | 安全执行，失败返回默认值 |
| `safeRun(Runnable)` | 安全执行，无返回值 |
| `handleException(Throwable, String context)` | 处理异常并记录日志 |
| `setLogLevel(LogLevel)` | 设置日志级别 |

---

## 性能优化实现

### TTLCacheManager 高性能缓存

#### 核心优化

| 优化项 | 实现 | 复杂度 |
|--------|------|--------|
| **LRU淘汰** | `LinkedHashMap(access-order=true)` | **O(1)** |
| **Pattern缓存** | `ConcurrentHashMap<String, Pattern>` | 避免重复编译 |
| **增量过期清理** | 随机抽样50条目/分钟 | 减少扫描开销 |
| **并发读取** | `ConcurrentHashMap` 快速查找 | 线程安全 |

---

### SimpleEventBus 高性能事件总线

#### 核心优化

| 优化项 | 实现 | 效果 |
|--------|------|------|
| **处理器排序** | 注册时排序存储 | **发布O(1)** |
| **并发安全** | `CopyOnWriteArrayList` | 读无锁 |
| **取消检查** | 发布时快速跳过 | 减少无效调用 |

---

### PlayerLockManager 玩家锁管理

#### 死锁预防

```java
// 双锁操作 - 自动按UUID排序防止死锁
public <T> T executeWithDualLock(UUID playerId1, UUID playerId2, Supplier<T> operation) {
    // 按UUID排序获取锁，防止死锁
    UUID first = playerId1.compareTo(playerId2) < 0 ? playerId1 : playerId2;
    UUID second = playerId1.compareTo(playerId2) < 0 ? playerId2 : playerId1;
}
```

---

## 插件基类 AbstractRPGPlugin

### 自动注入的服务

| 字段 | 类型 | 说明 |
|------|------|------|
| `rpgCore` | `RPGCore` | RPGCore 实例 |
| `externalServices` | `ExternalServiceIntegration` | 外部服务集成 |
| `scheduler` | `SyncScheduler` | 统一调度器 |
| `exceptionHandler` | `ExceptionHandler` | 异常处理器 |

### 需要实现的方法

| 方法 | 说明 |
|------|------|
| `onPluginEnable()` | 插件启动逻辑 |
| `onPluginDisable()` | 插件关闭逻辑 |
| `getPluginName()` | 返回插件名称 |

### 提供的工具方法

| 方法 | 说明 |
|------|------|
| `safeCall(Supplier<T>, T)` | 安全执行 |
| `safeRun(Runnable)` | 安全执行 |
| `isRPGCoreAvailable()` | RPGCore 是否可用 |
| `isExternalServicesAvailable()` | 外部服务是否可用 |
| `isSchedulerAvailable()` | 调度器是否可用 |

---

## 已迁移到统一服务的插件

| 插件 | 迁移内容 |
|------|---------|
| **GuangDianBoard** | ExternalServiceIntegration (LuckPerms/PAPI), SyncScheduler |
| **GuangDianName** | ExternalServiceIntegration (LuckPerms), SyncScheduler |

### 迁移前后对比

| 功能 | 迁移前 | 迁移后 |
|------|--------|--------|
| 获取 LuckPerms | `LuckPermsProvider.get()` | `externalServices.getPlayerPrefix(player)` |
| 解析占位符 | `PlaceholderAPI.setPlaceholders(player, text)` | `externalServices.parsePlaceholders(player, text)` |
| 定时任务 | `new BukkitRunnable() {...}.runTaskTimer()` | `scheduler.runSyncRepeating(task, delay, period)` |
| 任务取消 | `task.cancel()` | `scheduler.cancelTask(taskId)` |

---

## 统一线程池优势

| 指标 | 独立 BukkitRunnable | 统一 SyncScheduler |
|------|---------------------|-------------------|
| 任务管理 | 分散在各插件 | 集中管理 |
| 内存占用 | 每个任务独立对象 | 统一 Map 存储 |
| 调度效率 | 分散 | 集中 |
| 监控能力 | 困难 | `getActiveTaskCount()` |
| 关闭清理 | 可能遗漏 | `cancelAllTasks()` |

---

## 装备属性完整列表

### 基础属性
| 属性名 | 格式 | 说明 |
|--------|------|------|
| 生命上限 | `[数值]` | 增加玩家最大生命 |
| 攻击力 | `[min]-[max]` | 基础攻击伤害范围 |
| 防御力 | `[min]-[max]` | 基础防御值范围 |
| 每秒回血 | `[数值]` | 每秒恢复生命值 |
| 生命恢复 | `[百分比]` | 回血效率加成 |
| 护甲值 | `[百分比]` | 护甲减伤比例 |
| 护甲强度 | `[百分比]` | 护甲效果增强 |

### 战斗属性
| 属性名 | 格式 | 说明 |
|--------|------|------|
| 暴击几率 | `[百分比]` | 暴击触发概率 |
| 暴击伤害 | `[百分比]` | 暴击伤害倍率 |
| 暴击抵抗 | `[百分比]` | 减少被暴击几率 |
| 暴伤抵抗 | `[百分比]` | 减少暴击伤害 |
| 闪避 | `[百分比]` | 完全闪避攻击 |
| 招架 | `[百分比]` | 招架减少伤害 |
| 生命吸取 | `[百分比]` | 攻击吸血比例 |

### PVP属性
| 属性名 | 格式 | 说明 |
|--------|------|------|
| PVP攻击力 | `[min]-[max]` | PVP专属攻击力 |
| PVP防御力 | `[min]-[max]` | PVP专属防御力 |
| 护甲穿透 | `[百分比]` | 穿透护甲减伤 |
| 防御穿透 | `[百分比]` | 穿透防御减伤 |

---

## 宝石镶嵌系统

### 宝石槽位格式
```yaml
'&4*&3[&7可镶嵌<红宝石>&3]'   # 红宝石槽(攻击)
'&2*&3[&7可镶嵌<绿宝石>&3]'   # 绿宝石槽(生命)
'&e*&3[&7可镶嵌<黄宝石>&3]'   # 黄宝石槽(护甲)
'&5*&3[&7可镶嵌<紫宝石>&3]'   # 紫宝石槽(闪避)
```

### 宝石属性
| 宝石 | 主属性 | 格式 |
|------|--------|------|
| 红宝石 | 攻击力 | `min-max` |
| 绿宝石 | 生命上限 | `数值` |
| 黄宝石 | 护甲值 | `百分比` |
| 紫宝石 | 闪避 | `百分比` |

---

## MythicMobs 配置模板

### 怪物基础模板
```yaml
怪物名称:
  Type: [实体类型]
  Display: '&[颜色码][名称]'
  Health: [血量]
  Damage: [基础伤害]
  Options:
    MovementSpeed: [0.2-0.35]
    FollowRange: [15-30]
    KnockbackResistance: [0.5-1.0]
    AlwaysShowName: true
    PreventOtherDrops: true
    Despawn: true        # 性能关键
    Persistent: false    # 性能关键
  Skills:
  - [技能] @Target ~onTrigger [几率]
```

---

## 构建命令

```bash
# Windows 环境 (PowerShell)
cd e:\原创RPG服务端
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"

# 构建所有插件
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test

# 构建单个插件
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" :plugins:RPGCore:build --no-configuration-cache -x test
```

---

## 开发命名规范

### 文件命名
- 装备文件: `[副本/系列]装备.yml`
- 怪物文件: `[副本名称].yml`
- 技能文件: `[BOSS名称]技能.yml`

### 内部命名
- 怪物ID: `[副本]怪物类型`
- BOSS ID: 使用BOSS名称
- 物品ID: `[觉醒阶段][名称]`
- 服务接口: `*Service`
- 服务适配器: `*ServiceAdapter`

---

## 伤害计算流程

1. **基础伤害**: `攻击力随机值(min-max)`
2. **暴击判定**: `暴击几率` vs 随机数
3. **暴击倍率**: `暴击伤害` (默认200%)
4. **护甲减伤**: `护甲值` (上限85%)
5. **防御减伤**: `防御力/(防御力+15000)` (上限90%)
6. **穿透计算**: `护甲穿透、防御穿透`
7. **最终减伤上限**: 95%
8. **最小伤害**: 1

---

## 常见问题解决

### PlaceholderExpansion.unregister() 不存在

**问题**: `PlaceholderExpansion` 类没有 `unregister()` 方法

**解决方案**:
```java
// 错误做法
placeholders.unregister();

// 正确做法
PlaceholderAPI.unregisterExpansion(placeholders);
```

### BukkitRunnable 替代方案

**问题**: 分散的 BukkitRunnable 难以管理

**解决方案**:
```java
// 旧方式
BukkitRunnable task = new BukkitRunnable() {
    @Override
    public void run() { ... }
};
task.runTaskTimer(plugin, 0L, 20L);
task.cancel();

// 新方式
long taskId = scheduler.runSyncRepeating(() -> { ... }, 0L, 20L);
scheduler.cancelTask(taskId);
```

### 重复获取 LuckPerms

**问题**: 多个插件各自调用 `LuckPermsProvider.get()`

**解决方案**:
```java
// 旧方式 (每个插件独立获取)
LuckPerms luckPerms = LuckPermsProvider.get();
User user = luckPerms.getUserManager().getUser(player.getUniqueId());

// 新方式 (统一服务)
String prefix = externalServices.getPlayerPrefix(player);
```
