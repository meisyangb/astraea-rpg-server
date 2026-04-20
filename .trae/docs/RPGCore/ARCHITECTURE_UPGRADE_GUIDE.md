# RPGCore 架构升级指南

> **版本**: 1.1.0 | **更新日期**: 2026-04-16
>
> 本文档指导如何将现有插件迁移到 RPGCore 统一架构，消除重复代码，提高可维护性。

---

## 📋 目录

- [概述](#概述)
- [新增统一服务](#新增统一服务)
- [迁移指南](#迁移指南)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

---

## 概述

### 为什么需要统一架构?

在 Astraea RPG 项目中,我们发现大量重复代码散落在各个插件中:

| 重复模式 | 影响范围 | 重复代码量 |
|---------|---------|----------|
| MiniMessage 颜色转换 | 15+ 插件 | ~600 行 |
| YAML 数据加载/保存 | 36+ 文件 | ~1200 行 |
| ConcurrentHashMap 缓存 | 53+ 文件 | ~800 行 |
| Bukkit Scheduler 滥用 | 15+ 文件 | - |
| PlaceholderAPI 注册 | 10+ 插件 | ~100 行 |

**总计**: 约 **2700+ 行重复代码**,通过统一可减少 **40%+** 的代码量。

### 微内核架构原则

```
┌─────────────────────────────────────────┐
│         RPGCore (微内核)                 │
│  ┌───────────┬───────────┬───────────┐  │
│  │ 消息服务   │ 数据服务   │ 工具服务   │  │
│  └───────────┴───────────┴───────────┘  │
└─────────────────────────────────────────┘
              ↓ 依赖注入
┌─────────────────────────────────────────┐
│      业务插件 (GuangDian*)               │
│  只关注业务逻辑,不重复造轮子              │
└─────────────────────────────────────────┘
```

---

## 新增统一服务

### P0 - 核心服务 (立即使用)

#### 1. UnifiedMessageService - 统一消息服务

**位置**: `cn.guangdian.rpgcore.message.UnifiedMessageService`

**功能**:
- ✅ 统一的颜色转换 (`colorize()`)
- ✅ 自动离线检查的消息发送
- ✅ Title/ActionBar 快捷方法
- ✅ Legacy ↔ MiniMessage 格式转换

**使用示例**:
```java
UnifiedMessageService msg = UnifiedMessageService.getInstance();

// ❌ 旧方式 - 每个插件自己实现
private Component colorize(String text) {
    return miniMessage.deserialize(text.replace("&a", "<green>")...);
}
player.sendMessage(colorize("&a成功"));

// ✅ 新方式 - 统一服务
msg.sendMessage(player, "&a成功消息");
msg.sendMessage(playerUUID, "&c错误消息"); // 自动检查在线
msg.showTitle(player, "&6标题", "&f副标题", 10, 40, 10);
```

---

#### 2. PlayerDataService<T> - 玩家数据服务基类

**位置**: `cn.guangdian.rpgcore.data.PlayerDataService`

**功能**:
- ✅ 统一的玩家数据缓存 (ConcurrentHashMap)
- ✅ 自动标记脏数据
- ✅ 定时自动保存
- ✅ YAML 序列化/反序列化
- ✅ 关闭时自动保存

**使用示例**:
```java
// 1. 定义数据类
public class PointData {
    private long balance = 0;
    // getters/setters...
}

// 2. 继承 PlayerDataService
public class PointsDataService extends PlayerDataService<PointData> {
    public PointsDataService() {
        super("points", PointData.class);
    }

    @Override
    protected PointData createDefaultData() {
        return new PointData();
    }

    @Override
    protected Map<String, Object> serialize(PointData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("balance", data.getBalance());
        return map;
    }

    @Override
    protected PointData deserialize(Map<String, Object> data) {
        PointData pd = new PointData();
        pd.setBalance((Long) data.getOrDefault("balance", 0L));
        return pd;
    }
}

// 3. 使用服务
PointsDataService service = new PointsDataService();
service.startAutoSave(6000); // 5分钟自动保存

PointData data = service.getData(playerUUID);
data.setBalance(data.getBalance() + 100);
// 自动标记为 dirty,定时保存
```

**对比旧方式**:
```java
// ❌ 旧方式 - 每个插件都写一遍
private final Map<UUID, PointData> cache = new ConcurrentHashMap<>();
private final Set<UUID> dirtyCache = new HashSet<>();

private void loadData() { /* 30行YAML加载 */ }
private void saveData() { /* 20行YAML保存 */ }
getServer().getScheduler().runTaskTimer(this, this::saveData, 6000, 6000);

// ✅ 新方式 - 只需实现序列化逻辑
public class PointsDataService extends PlayerDataService<PointData> {
    // 只需关注业务逻辑!
}
```

---

#### 3. YamlDataStore - YAML 数据存储工具

**位置**: `cn.guangdian.rpgcore.data.YamlDataStore`

**功能**:
- ✅ 统一的 YAML 读写
- ✅ 自动创建父目录
- ✅ 异常处理
- ✅ 备份功能

**使用示例**:
```java
YamlDataStore store = YamlDataStore.getInstance();

// 保存数据
Map<String, Object> data = new HashMap<>();
data.put("name", "张三");
data.put("level", 10);
store.save(new File("data/player.yml"), data);

// 加载数据
Map<String, Object> loaded = store.load(new File("data/player.yml"));
String name = (String) loaded.get("name");
```

---

### P1 - 增强服务 (近期使用)

#### 4. CooldownManager - 冷却管理器

**位置**: `cn.guangdian.rpgcore.util.CooldownManager`

**功能**:
- ✅ 统一的冷却时间管理
- ✅ 自动清理过期冷却
- ✅ 支持多动作冷却

**使用示例**:
```java
CooldownManager cooldown = CooldownManager.getInstance();

// 设置冷却 (10秒)
cooldown.setCooldown(playerUUID, "trade_request", 10000);

// 检查冷却
if (cooldown.isOnCooldown(playerUUID, "trade_request")) {
    long remaining = cooldown.getRemainingSeconds(playerUUID, "trade_request");
    msg.sendMessage(player, "&c还需等待 " + remaining + " 秒");
    return;
}

// 执行操作...
```

---

#### 5. PlaceholderService - 统一占位符注册

**位置**: `cn.guangdian.rpgcore.integration.PlaceholderService`

**功能**:
- ✅ 统一的 PlaceholderAPI 注册
- ✅ 自动前缀 `%rpg_xxx%`
- ✅ 批量注册

**使用示例**:
```java
PlaceholderService placeholder = PlaceholderService.getInstance();

// 注册占位符
placeholder.register("points_balance", (player, params) -> {
    return String.valueOf(pointsService.getBalance(player.getUniqueId()));
});

// 在游戏中使用: %rpg_points_balance%
```

---

## 迁移指南

### 步骤 1: 替换颜色转换

**查找**: 所有插件中的 `colorize()` / `translateColor()` 方法

**替换为**:
```java
import cn.guangdian.rpgcore.message.UnifiedMessageService;

private final UnifiedMessageService msg = UnifiedMessageService.getInstance();

// 使用
Component component = msg.colorize("&a成功消息");
msg.sendMessage(player, "&6金色文字");
```

---

### 步骤 2: 迁移玩家数据缓存

**查找**: `Map<UUID, ?> = new ConcurrentHashMap<>()`

**替换为**:
```java
import cn.guangdian.rpgcore.data.PlayerDataService;

public class MyDataService extends PlayerDataService<MyData> {
    public MyDataService() {
        super("mydata", MyData.class);
        startAutoSave(6000); // 5分钟自动保存
    }

    @Override
    protected MyData createDefaultData() {
        return new MyData();
    }

    @Override
    protected Map<String, Object> serialize(MyData data) {
        // 序列化逻辑
    }

    @Override
    protected MyData deserialize(Map<String, Object> data) {
        // 反序列化逻辑
    }
}
```

---

### 步骤 3: 修复 Scheduler 违规使用

**查找**: `getServer().getScheduler()`

**替换为**:
```java
import cn.guangdian.rpgcore.api.SyncScheduler;

SyncScheduler scheduler = RPGCore.getInstance().getScheduler();

// 延迟任务
scheduler.runSyncLater(() -> {
    // 任务逻辑
}, 100L);

// 循环任务
scheduler.runSyncRepeating(() -> {
    // 任务逻辑
}, 0L, 20L);
```

---

### 步骤 4: 迁移 PlaceholderAPI 注册

**查找**: `new XxxPlaceholder(this).register()`

**替换为**:
```java
import cn.guangdian.rpgcore.integration.PlaceholderService;

PlaceholderService placeholder = PlaceholderService.getInstance();

placeholder.register("my_value", (player, params) -> {
    return myService.getValue(player.getUniqueId());
});
```

---

## 最佳实践

### 1. 插件主类最小化

```java
@RPGService(serviceInterface = PointsService.class)
public class PointsServiceImpl implements PointsService {

    private final PlayerDataService<PointData> dataService;
    private final UnifiedMessageService msg;

    @Inject
    public PointsServiceImpl(PlayerDataService<PointData> dataService,
                            UnifiedMessageService msg) {
        this.dataService = dataService;
        this.msg = msg;
    }

    // 只关注业务逻辑!
    @Override
    public void addPoints(UUID player, long amount) {
        PointData data = dataService.getData(player);
        data.setBalance(data.getBalance() + amount);
        dataService.setData(player, data);

        msg.sendMessage(player, "&a获得 " + amount + " 点数!");
    }
}
```

---

### 2. 使用注解驱动开发 (未来)

```java
@Command(name="points", permission="guangdian.points.use")
public class PointsCommand extends BaseCommand {

    @SubCommand("give")
    @Permission("guangdian.points.admin")
    public void give(Player sender, @Arg Player target, @Arg long amount) {
        pointsService.addPoints(target.getUniqueId(), amount);
        msg.sendMessage(sender, "&a已给予 " + target.getName() + " " + amount + " 点数");
    }
}
```

---

### 3. 避免常见陷阱

❌ **不要**在每个插件中实现 `colorize()`:
```java
// 错误示范
private Component colorize(String text) {
    return miniMessage.deserialize(text.replace("&a", "<green>")...);
}
```

✅ **应该**使用统一服务:
```java
UnifiedMessageService.getInstance().colorize("&a文本");
```

---

## 常见问题

### Q1: 迁移后会影响性能吗?

**A**: 不会。统一服务经过优化,甚至可能提升性能:
- `PlayerDataService` 内置脏数据标记,减少不必要的保存
- `CooldownManager` 定期清理过期数据,防止内存泄漏
- `UnifiedMessageService` 复用 MiniMessage 实例

---

### Q2: 旧代码还能用吗?

**A**: 可以。新服务与旧代码完全兼容,可以逐步迁移。

---

### Q3: 如何测试迁移后的代码?

**A**:
1. 编译测试: `gradle build --no-configuration-cache -x test`
2. 运行测试: 启动服务器,检查日志无错误
3. 功能测试: 验证所有功能正常工作

---

### Q4: 遇到迁移问题怎么办?

**A**:
1. 查看本文档的示例代码
2. 参考已迁移的插件 (如 GuangDianRaid)
3. 在 GitHub Issues 提问

---

## 附录: 快速参考

### 服务速查表

| 旧方式 | 新方式 | 位置 |
|-------|--------|------|
| `colorize()` | `UnifiedMessageService.colorize()` | `cn.guangdian.rpgcore.message` |
| `Map<UUID, T>` | `PlayerDataService<T>` | `cn.guangdian.rpgcore.data` |
| `YamlConfiguration.load()` | `YamlDataStore.load()` | `cn.guangdian.rpgcore.data` |
| `Bukkit.getScheduler()` | `SyncScheduler` | `cn.guangdian.rpgcore.api` |
| `new PlaceholderExpansion()` | `PlaceholderService.register()` | `cn.guangdian.rpgcore.integration` |
| `Map<UUID, Long>` 冷却 | `CooldownManager` | `cn.guangdian.rpgcore.util` |

---

*最后更新: 2026-04-16*
*作者: Astraea RPG Team*
