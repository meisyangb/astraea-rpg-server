# Astraea RPG 架构边界定义规范

> 明确各模块功能职责范围，建立清晰的接口规范，避免概念混淆和职责交叉
> **版本: 1.0.0 | 更新: 2026-04-26**

---

## 📋 目录

1. [架构分层原则](#架构分层原则)
2. [边界定义规范](#边界定义规范)
3. [接口规范](#接口规范)
4. [职责划分](#职责划分)
5. [禁止的越界行为](#禁止的越界行为)
6. [最佳实践](#最佳实践)

---

## 架构分层原则

### 三层架构模型

```
┌─────────────────────────────────────────────────────────────┐
│  Layer 3: 业务插件层 (Business Plugins)                      │
│  - GuangDianPoints, GuangDianGuild, GuangDianMarket...      │
│  - 职责: 业务逻辑实现、业务事件定义、业务数据管理            │
│  - 权限: 实现服务接口、发布业务事件、调用基础设施            │
└─────────────────────────────────────────────────────────────┘
                              ↑
                         依赖/实现
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Layer 2: 服务契约层 (Service Contracts)                     │
│  - cn.guangdian.rpgcore.service.api                         │
│  - 职责: 定义业务服务接口规范                                │
│  - 权限: 仅定义接口，不包含实现                              │
└─────────────────────────────────────────────────────────────┘
                              ↑
                         依赖/使用
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Layer 1: 基础设施层 (Infrastructure)                        │
│  - cn.guangdian.rpgcore.api                                 │
│  - 职责: 提供服务注册、调度、缓存等通用能力                  │
│  - 权限: 提供接口和基础实现，不处理业务逻辑                  │
└─────────────────────────────────────────────────────────────┘
```

### 分层依赖规则

| 层级 | 可以依赖 | 禁止依赖 |
|------|----------|----------|
| 业务插件层 | 服务契约层、基础设施层 | 无 |
| 服务契约层 | 基础设施层 | 业务插件层 |
| 基础设施层 | 无 | 服务契约层、业务插件层 |

---

## 边界定义规范

### 1. 基础设施层 (Layer 1)

**包路径**: `cn.guangdian.rpgcore.api`

**职责范围**:
- ✅ 服务注册与发现 (ServiceRegistry)
- ✅ 任务调度 (SyncScheduler, AsyncExecutor)
- ✅ 缓存管理 (CacheProvider)
- ✅ 并发控制 (PlayerLockManager)
- ✅ 事件发布 (EventPublisher)
- ✅ 配置管理 (ConfigManager)
- ✅ 日志服务 (GameLogger)
- ✅ 数据库连接 (CoreDatabase)

**禁止行为**:
- ❌ 定义业务相关接口
- ❌ 处理业务逻辑
- ❌ 定义业务事件
- ❌ 直接操作业务数据

**示例**:
```java
// ✅ 正确: 基础设施只提供通用能力
public interface ServiceRegistry {
    <T> void registerService(Class<T> serviceClass, T service);
    <T> T getService(Class<T> serviceClass);
}

// ❌ 错误: 基础设施层不应定义业务接口
public interface PointsService {  // 这应该在 service.api 包
    void addPoints(UUID playerId, int amount);
}
```

### 2. 服务契约层 (Layer 2)

**包路径**: `cn.guangdian.rpgcore.service.api`

**职责范围**:
- ✅ 定义业务服务接口规范
- ✅ 定义服务间的契约关系
- ✅ 定义数据传输对象 (DTO)

**禁止行为**:
- ❌ 包含实现代码
- ❌ 依赖具体业务插件
- ❌ 处理业务事件

**示例**:
```java
// ✅ 正确: 只定义接口契约
public interface PointsService {
    long getPoints(UUID playerId);
    boolean addPoints(UUID playerId, long amount, String reason);
    boolean removePoints(UUID playerId, long amount, String reason);
}

// ❌ 错误: 契约层不应包含实现
public interface PointsService {
    default long getPoints(UUID playerId) {
        return database.query(...);  // 实现应在业务插件
    }
}
```

### 3. 业务插件层 (Layer 3)

**包路径**: `cn.guangdian.*`

**职责范围**:
- ✅ 实现服务契约接口
- ✅ 定义业务事件
- ✅ 处理业务逻辑
- ✅ 管理业务数据

**禁止行为**:
- ❌ 修改基础设施层代码
- ❌ 直接操作其他插件的内部数据
- ❌ 在 RPGCore 中定义业务事件

**示例**:
```java
// ✅ 正确: 业务插件实现契约接口
public class PointsServiceImpl implements PointsService {
    @Override
    public long getPoints(UUID playerId) {
        // 业务实现
    }
}

// ✅ 正确: 业务插件定义自己的事件
public class PointsTransactionEvent extends Event {
    private final UUID playerId;
    private final long amount;
    // ...
}
```

---

## 接口规范

### 接口命名规范

| 层级 | 命名规则 | 示例 |
|------|----------|------|
| 基础设施 | `*Provider`, `*Manager`, `*Registry` | `CacheProvider`, `PlayerLockManager` |
| 服务契约 | `*Service` | `PointsService`, `GuildService` |
| 业务实现 | `*ServiceImpl`, `*Adapter` | `PointsServiceImpl`, `GuildServiceAdapter` |

### 接口访问权限

```java
// 基础设施接口 - 公开访问
public interface CacheProvider {
    <K, V> LoadingCache<K, V> getLoadingCache(String name, Function<K, V> loader);
}

// 服务契约接口 - 公开访问
public interface PointsService {
    long getPoints(UUID playerId);
}

// 业务实现 - 包级私有或内部类
class PointsServiceImpl implements PointsService {
    // 实现细节不对外暴露
}
```

---

## 职责划分

### 事件系统职责

| 事件类型 | 归属 | 示例 |
|----------|------|------|
| 基础设施事件 | RPGCore | `PlayerDataLoadEvent`, `ModuleEnableEvent` |
| 业务事件 | 业务插件 | `PointsTransactionEvent`, `GuildCreateEvent` |

**规则**:
- 基础设施事件由 RPGCore 定义和发布
- 业务事件由业务插件定义和发布
- 业务插件可以订阅基础设施事件
- 业务插件可以订阅其他业务插件的事件

### 数据管理职责

| 数据类型 | 管理方 | 存储位置 |
|----------|--------|----------|
| 玩家基础数据 | RPGCore | `player_data/` |
| 业务数据 | 业务插件 | `plugins/{插件名}/data/` |
| 配置数据 | 各插件 | `plugins/{插件名}/config.yml` |

### 服务实现职责

| 服务类型 | 定义位置 | 实现位置 |
|----------|----------|----------|
| 基础设施服务 | `rpgcore.api` | `rpgcore.*` (RPGCore内部) |
| 业务服务接口 | `rpgcore.service.api` | 各业务插件 |

---

## 禁止的越界行为

### ❌ 严重违规 (必须杜绝)

1. **基础设施层处理业务逻辑**
   ```java
   // ❌ 禁止: RPGCore 不应包含业务逻辑
   public class RPGCore extends JavaPlugin {
       public void onPlayerKill(Player killer, Player victim) {
           // 处理击杀奖励逻辑 - 错误！
       }
   }
   ```

2. **业务插件修改基础设施**
   ```java
   // ❌ 禁止: 业务插件不应修改 RPGCore 内部
   public class MyPlugin extends AbstractRPGPlugin {
       @Override
       protected void onPluginEnable() {
           // 修改 RPGCore 的内部缓存 - 错误！
           rpgCore.getCacheProvider().getClass().getDeclaredField("cacheMap").set(...);
       }
   }
   ```

3. **在 RPGCore 定义业务事件**
   ```java
   // ❌ 禁止: RPGCore 不应定义业务事件
   package cn.guangdian.rpgcore.event.events;
   
   public class PointsTransactionEvent extends Event {  // 错误！
       // 应该在 GuangDianPoints 中定义
   }
   ```

4. **服务契约层包含实现**
   ```java
   // ❌ 禁止: 契约层不应包含实现
   public interface PointsService {
       default void addPoints(UUID playerId, int amount) {
           // 实现代码 - 错误！
       }
   }
   ```

### ⚠️ 一般违规 (需要整改)

1. **直接依赖具体实现类**
   ```java
   // ⚠️ 不推荐: 应依赖接口而非实现
   PointsServiceImpl service = new PointsServiceImpl();  // 不推荐
   
   // ✅ 推荐: 通过 ServiceRegistry 获取
   PointsService service = registry.getService(PointsService.class);
   ```

2. **跨插件直接访问数据**
   ```java
   // ⚠️ 不推荐: 直接访问其他插件的数据
   GuangDianPoints.getInstance().getPlayerData(playerId);  // 不推荐
   
   // ✅ 推荐: 通过 Service 接口访问
   PointsService service = registry.getService(PointsService.class);
   service.getPoints(playerId);
   ```

---

## 最佳实践

### 1. 服务注册与发现

```java
// 业务插件在启用时注册服务
@Override
protected void onPluginEnable() {
    RPGCore rpgCore = RPGCore.getInstance();
    ServiceRegistry registry = rpgCore.getServiceRegistry();
    
    // 注册服务实现
    registry.registerService(PointsService.class, new PointsServiceImpl(this));
}

// 其他插件通过 ServiceRegistry 获取服务
public void someMethod(Player player) {
    RPGCore rpgCore = RPGCore.getInstance();
    ServiceRegistry registry = rpgCore.getServiceRegistry();
    
    PointsService pointsService = registry.getService(PointsService.class);
    if (pointsService != null) {
        long points = pointsService.getPoints(player.getUniqueId());
    }
}
```

### 2. 事件订阅规范

```java
// 业务插件订阅其他插件的事件
@EventHandler
public void onPointsTransaction(cn.guangdian.points.event.PointsTransactionEvent event) {
    // 处理点数交易事件
}

// 业务插件发布自己的事件
public void transferPoints(UUID from, UUID to, long amount) {
    // 执行转账逻辑
    
    // 发布事件
    Bukkit.getPluginManager().callEvent(
        new PointsTransactionEvent(from, to, amount)
    );
}
```

### 3. 数据访问规范

```java
// ✅ 正确: 通过 Service 接口访问数据
public class MarketServiceImpl implements MarketService {
    private final PointsService pointsService;
    
    public MarketServiceImpl() {
        RPGCore rpgCore = RPGCore.getInstance();
        this.pointsService = rpgCore.getServiceRegistry()
            .getService(PointsService.class);
    }
    
    @Override
    public boolean purchaseItem(Player player, ItemStack item, long price) {
        UUID playerId = player.getUniqueId();
        
        // 通过 Service 接口检查余额
        if (pointsService.getPoints(playerId) >= price) {
            // 执行购买
            pointsService.removePoints(playerId, price, "购买物品");
            return true;
        }
        return false;
    }
}
```

### 4. 错误处理边界

```java
// 基础设施层只处理技术异常
try {
    cache.get(key);
} catch (CacheException e) {
    logger.error("缓存操作失败", e);
    // 不处理业务逻辑
}

// 业务层处理业务异常
try {
    pointsService.transfer(from, to, amount);
} catch (InsufficientPointsException e) {
    player.sendMessage("余额不足");
    // 处理业务逻辑
}
```

---

## 检查清单

### 开发前检查

- [ ] 明确功能属于哪个层级
- [ ] 确定接口定义位置
- [ ] 确定实现位置
- [ ] 确定事件归属

### 代码审查检查

- [ ] 基础设施层无业务逻辑
- [ ] 服务契约层无实现代码
- [ ] 业务插件不修改基础设施
- [ ] 事件归属正确
- [ ] 依赖方向正确（上层依赖下层）

### 架构审查检查

- [ ] 无循环依赖
- [ ] 接口与实现分离
- [ ] 职责单一明确
- [ ] 边界清晰无交叉

---

## 相关文档

- [DEVELOPMENT_GUIDE.md](../../rules/DEVELOPMENT_GUIDE.md) - 开发指南
- [RPGCORE_API_REFERENCE.md](RPGCORE_API_REFERENCE.md) - API 参考手册
- [RPGCORE_DEVELOPMENT_STANDARD.md](../../rules/RPGCORE_DEVELOPMENT_STANDARD.md) - 开发标准

---

*本规范是强制性的，所有开发人员必须严格遵守*
*违反边界定义将导致架构混乱、功能冲突和维护困难*
*最后更新: 2026-04-26*
