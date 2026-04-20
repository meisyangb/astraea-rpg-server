# RPGCore 统一服务总览

> **版本**: 1.1.0 | **更新日期**: 2026-04-16
>
> 本文档列出 RPGCore 应该统一提供的所有服务,以及各插件不应重复实现的功能。

---

## 📊 架构原则

### 微内核 + 插件化架构

```
┌──────────────────────────────────────────────────────┐
│              RPGCore (微内核层)                        │
│                                                      │
│  ┌────────────┬────────────┬───────────────────┐    │
│  │ 基础设施    │ 业务服务    │ 工具类             │    │
│  ├────────────┼────────────┼───────────────────┤    │
│  │ EventBus   │ PointsSvc  │ MessageService    │    │
│  │ Scheduler  │ GuildSvc   │ CooldownManager   │    │
│  │ Cache      │ QuestSvc   │ YamlDataStore     │    │
│  │ Lock       │ TradeSvc   │ PlayerDataService │    │
│  └────────────┴────────────┴───────────────────┘    │
└──────────────────────────────────────────────────────┘
                    ↓ 依赖注入 / ServiceRegistry
┌──────────────────────────────────────────────────────┐
│          业务插件层 (GuangDian*)                      │
│                                                      │
│  只关注业务逻辑,通过 RPGCore 获取基础设施              │
│  - 不重复实现 colorize()                             │
│  - 不直接操作 ConcurrentHashMap                       │
│  - 不调用 Bukkit.getScheduler()                      │
│  - 不手动读写 YAML                                   │
└──────────────────────────────────────────────────────┘
```

---

## ✅ RPGCore 已统一提供的服务

### 基础设施层

| 服务 | 位置 | 功能 | 状态 |
|------|------|------|------|
| **EventBus** | `cn.guangdian.rpgcore.event` | 事件总线,批量处理优化 | ✅ 已完成 |
| **ServiceRegistry** | `cn.guangdian.rpgcore.service` | 服务注册中心 | ✅ 已完成 |
| **SyncScheduler** | `cn.guangdian.rpgcore.scheduler` | 统一调度器 (Paper AsyncScheduler封装) | ✅ 已完成 |
| **AsyncExecutor** | `cn.guangdian.rpgcore.async` | 异步执行器 | ✅ 已完成 |
| **CacheProvider** | `cn.guangdian.rpgcore.cache` | TTL缓存 (Caffeine) | ✅ 已完成 |
| **PlayerLockManager** | `cn.guangdian.rpgcore.concurrency` | 玩家级细粒度锁 | ✅ 已完成 |
| **PerformanceMonitor** | `cn.guangdian.rpgcore.monitor` | 性能监控 | ✅ 已完成 |

### 生命周期管理

| 服务 | 位置 | 功能 | 状态 |
|------|------|------|------|
| **PlayerLifecycleManager** | `cn.guangdian.rpgcore.lifecycle` | 玩家登录/退出数据加载保存 | ✅ 已完成 |
| **AbstractPlayerDataHandler** | `cn.guangdian.rpgcore.lifecycle` | 数据处理器基类 | ✅ 已完成 |
| **UnifiedDataManager** | `cn.guangdian.rpgcore.storage` | 统一数据管理器 | ✅ 已完成 |

### 外部服务集成

| 服务 | 位置 | 功能 | 状态 |
|------|------|------|------|
| **ExternalServiceIntegration** | `cn.guangdian.rpgcore.integration` | LuckPerms/Vault/PAPI统一封装 | ✅ 已完成 |
| **MiniMessageService** | `cn.guangdian.rpgcore.message` | MiniMessage消息解析 | ✅ 已完成 |
| **SoundService** | `cn.guangdian.rpgcore.sound` | 音效服务 | ✅ 已完成 |
| **TextDisplayService** | `cn.guangdian.rpgcore.display` | TextDisplay实体服务 | ✅ 已完成 |
| **AudienceService** | `cn.guangdian.rpgcore.message` | Audience消息发送 | ✅ 已完成 |

---

## 🆕 新增统一服务 (v1.1.0)

### P0 - 核心服务 (立即使用)

#### 1. UnifiedMessageService ⭐⭐⭐⭐⭐

**位置**: `cn.guangdian.rpgcore.message.UnifiedMessageService`

**解决的问题**:
- ❌ 15+ 插件各自实现 `colorize()` 方法 (~600行重复代码)
- ❌ 每个插件都要检查玩家在线状态
- ❌ Legacy ↔ MiniMessage 格式转换重复实现

**提供的功能**:
```java
// 颜色转换 (替代各插件的 colorize 方法)
Component component = msg.colorize("&6金色文字 &a绿色文字");

// 智能消息发送 (自动处理离线检查)
msg.sendMessage(playerUUID, "&c错误消息"); // 返回 boolean

// Title 显示
msg.showTitle(player, "&6标题", "&f副标题", 10, 40, 10);

// ActionBar
msg.sendActionBar(player, "&e滚动消息");

// 广播 (支持过滤)
msg.broadcastFiltered("&6[VIP] &f特殊消息", p -> p.hasPermission("vip.receive"));

// 格式转换
String miniMsg = msg.legacyToMiniMessage("&a成功"); // "<green>成功"
String legacy = msg.miniMessageToLegacy("<green>成功"); // "&a成功"
```

**迁移指南**: 见 [ARCHITECTURE_UPGRADE_GUIDE.md](./ARCHITECTURE_UPGRADE_GUIDE.md#步骤-1-替换颜色转换)

---

#### 2. PlayerDataService<T> ⭐⭐⭐⭐⭐

**位置**: `cn.guangdian.rpgcore.data.PlayerDataService`

**解决的问题**:
- ❌ 53+ 文件使用 `Map<UUID, ?> = new ConcurrentHashMap<>()` (~800行重复)
- ❌ 每个插件都实现 loadData()/saveData()
- ❌ 脏数据标记逻辑重复
- ❌ 自动保存任务重复

**提供的功能**:
```java
public class PointsDataService extends PlayerDataService<PointData> {
    public PointsDataService() {
        super("points", PointData.class);
        startAutoSave(6000); // 5分钟自动保存
    }

    @Override
    protected PointData createDefaultData() { /* ... */ }

    @Override
    protected Map<String, Object> serialize(PointData data) { /* ... */ }

    @Override
    protected PointData deserialize(Map<String, Object> data) { /* ... */ }
}

// 使用
PointData data = service.getData(playerUUID);
data.setBalance(data.getBalance() + 100);
service.setData(playerUUID, data); // 自动标记 dirty
```

**内置功能**:
- ✅ ConcurrentHashMap 缓存
- ✅ 脏数据自动标记
- ✅ 定时自动保存
- ✅ YAML 序列化/反序列化
- ✅ 关闭时自动保存
- ✅ 统计信息 (`getStats()`)

**迁移指南**: 见 [ARCHITECTURE_UPGRADE_GUIDE.md](./ARCHITECTURE_UPGRADE_GUIDE.md#步骤-2-迁移玩家数据缓存)

---

#### 3. YamlDataStore ⭐⭐⭐⭐

**位置**: `cn.guangdian.rpgcore.data.YamlDataStore`

**解决的问题**:
- ❌ 36+ 文件重复实现 YAML 加载/保存逻辑 (~1200行)
- ❌ 父目录创建逻辑重复
- ❌ 异常处理不一致

**提供的功能**:
```java
YamlDataStore store = YamlDataStore.getInstance();

// 保存
Map<String, Object> data = new HashMap<>();
data.put("balance", 1000L);
store.save(new File("data/player.yml"), data);

// 加载
Map<String, Object> loaded = store.load(new File("data/player.yml"));

// 备份
store.createBackup(new File("data/player.yml"));
```

---

### P1 - 增强服务 (近期使用)

#### 4. CooldownManager ⭐⭐⭐

**位置**: `cn.guangdian.rpgcore.util.CooldownManager`

**解决的问题**:
- ❌ 多个插件重复实现冷却逻辑
- ❌ `Map<UUID, Long>` 冷却时间管理

**提供的功能**:
```java
CooldownManager cooldown = CooldownManager.getInstance();

// 设置冷却
cooldown.setCooldown(playerUUID, "trade_request", 10000); // 10秒

// 检查冷却
if (cooldown.isOnCooldown(playerUUID, "trade_request")) {
    long remaining = cooldown.getRemainingSeconds(playerUUID, "trade_request");
    // 提示剩余时间
}

// 清除冷却
cooldown.clearCooldown(playerUUID, "trade_request");
```

---

#### 5. PlaceholderService ⭐⭐⭐

**位置**: `cn.guangdian.rpgcore.integration.PlaceholderService`

**解决的问题**:
- ❌ 10+ 插件各自创建 PlaceholderExpansion
- ❌ 占位符前缀不统一

**提供的功能**:
```java
PlaceholderService placeholder = PlaceholderService.getInstance();

// 注册占位符 (自动添加 %rpg_ 前缀)
placeholder.register("points_balance", (player, params) -> {
    return String.valueOf(pointsService.getBalance(player.getUniqueId()));
});

// 在游戏中使用: %rpg_points_balance%
```

---

## 📋 各插件不应重复实现的功能清单

### 🔴 严格禁止 (必须使用 RPGCore 服务)

| 功能 | 禁止做法 | 正确做法 | 原因 |
|------|---------|---------|------|
| **颜色转换** | 自己实现 `colorize()` | `UnifiedMessageService.colorize()` | 避免600+行重复代码 |
| **玩家数据缓存** | `Map<UUID, T> = new ConcurrentHashMap<>()` | `PlayerDataService<T>` | 统一管理,自动保存 |
| **YAML 读写** | `YamlConfiguration.loadConfiguration()` | `YamlDataStore` | 统一异常处理 |
| **调度器** | `Bukkit.getScheduler()` | `SyncScheduler` | Paper 1.21.6 规范 |
| **冷却管理** | `Map<UUID, Long>` | `CooldownManager` | 防止内存泄漏 |

### 🟡 强烈建议 (推荐使用 RPGCore 服务)

| 功能 | 建议做法 | 收益 |
|------|---------|------|
| **消息发送** | `UnifiedMessageService.sendMessage()` | 自动离线检查 |
| **占位符注册** | `PlaceholderService.register()` | 统一前缀管理 |
| **日志记录** | `GameLogger` | 异步日志,性能更好 |
| **权限检查** | `Permissions` 工具类 | 统一权限格式 |

### 🟢 可选优化 (可根据需要选择)

| 功能 | 说明 |
|------|------|
| **GUI 构建** | 未来提供 `GUIBuilder` 框架 |
| **命令框架** | 未来提供注解驱动的命令系统 |
| **事务日志** | 未来提供通用 `TransactionLogService` |

---

## 🎯 架构改进路线图

### Phase 1: 消除重复代码 (1-2周) ✅ 进行中

- [x] 创建 `UnifiedMessageService`
- [x] 创建 `PlayerDataService<T>`
- [x] 创建 `YamlDataStore`
- [x] 创建 `CooldownManager`
- [x] 创建 `PlaceholderService`
- [ ] 迁移 GuangDianPoints (示例插件)
- [ ] 编写迁移指南文档
- [ ] 更新 FORBIDDEN_PATTERNS.md

### Phase 2: 完善服务框架 (2-4周)

- [ ] 升级 `CommandFramework` 支持注解
- [ ] 创建 `GUIBuilder` 框架
- [ ] 添加 ArchUnit 测试检测架构违规
- [ ] 创建插件模板项目

### Phase 3: 自动化与最佳实践 (4-8周)

- [ ] 实现完整的注解驱动开发模式
- [ ] 编写完整的开发者文档
- [ ] 视频教程系列

---

## 📊 预期收益

### 代码量减少

| 项目 | 当前 | 迁移后 | 减少 |
|------|------|--------|------|
| 颜色转换代码 | ~600行 | ~0行 | -600行 |
| YAML 数据加载/保存 | ~1200行 | ~200行 | -1000行 |
| ConcurrentHashMap 缓存 | ~800行 | ~100行 | -700行 |
| 冷却管理 | ~200行 | ~0行 | -200行 |
| PlaceholderAPI 注册 | ~100行 | ~50行 | -50行 |
| **总计** | **~2900行** | **~350行** | **-2550行 (88%)** |

### 可维护性提升

- ✅ 统一的 API,降低学习成本
- ✅ 集中修复 Bug,无需修改多个插件
- ✅ 新功能一次实现,所有插件受益
- ✅ 代码审查更容易 (标准化模式)

### 性能优化

- ✅ `PlayerDataService` 脏数据标记,减少不必要的保存
- ✅ `CooldownManager` 定期清理,防止内存泄漏
- ✅ `UnifiedMessageService` 复用 MiniMessage 实例

---

## 🔗 相关文档

- [架构升级指南](./ARCHITECTURE_UPGRADE_GUIDE.md) - 详细的迁移步骤
- [FORBIDDEN_PATTERNS.md](../../rules/FORBIDDEN_PATTERNS.md) - 禁止模式清单
- [CODE_TEMPLATES.md](../../rules/CODE_TEMPLATES.md) - 代码模板库
- [Paper 1.21.6 特性审查](../FIXES/2026/04/paper-1.21.6-feature-review.md)

---

*最后更新: 2026-04-16*
*作者: Astraea RPG Team*
