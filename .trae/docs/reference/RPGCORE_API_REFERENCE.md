# RPGCore API 参考手册

> 完整的核心服务 API 文档和迁移指南
> **版本: 2.0.0 | 更新: 2026-04-26 | Paper: 1.21.6**

---

## 📋 目录

1. [概述](#概述)
2. [核心服务](#核心服务)
   - [SoundService - 音效服务](#soundservice---音效服务)
   - [ServerService - 服务器服务](#serverservice---服务器服务)
   - [EntityService - 实体服务](#entityservice---实体服务)
   - [PlayerLockManager - 玩家锁管理器](#playerlockmanager---玩家锁管理器)
   - [CacheProvider - 缓存提供者](#cacheprovider---缓存提供者)
   - [EventPublisher - 事件发布器](#eventpublisher---事件发布器)
   - [MiniMessageService - 消息服务](#minimessageservice---消息服务)
3. [迁移指南](#迁移指南)
4. [最佳实践](#最佳实践)

---

## 概述

RPGCore 提供了一系列核心服务，封装了 Paper 1.21.6 中已弃用的 API，为子插件提供统一的调用接口。

### 获取服务

```java
RPGCore rpgCore = RPGCore.getInstance();

// 核心服务
SoundService soundService = rpgCore.getSoundService();
ServerService serverService = rpgCore.getServerService();
EntityService entityService = rpgCore.getEntityService();
PlayerLockManager lockManager = rpgCore.getLockManager();
CacheProvider cacheProvider = rpgCore.getCacheProvider();

// 独立服务
MiniMessageService mm = MiniMessageService.getInstance();
```

---

## 核心服务

### SoundService - 音效服务

**包路径**: `cn.guangdian.rpgcore.sound.SoundService`

**解决问题**:
- ❌ `Sound.valueOf()` 已弃用
- ❌ `Sound.key()` 已弃用

#### API 参考

```java
// 获取服务
SoundService soundService = RPGCore.getInstance().getSoundService();

// 播放音效
soundService.playSound(player, "SUCCESS", 1.0f, 1.0f);
soundService.playSound(location, "CLICK", 0.5f, 1.2f);
soundService.broadcastSound("LEVEL_UP", 1.0f, 1.0f);

// 停止音效
soundService.stopSound(player, "MUSIC");
soundService.stopAllSounds(player);
```

#### 支持的音效别名

| 别名 | 实际音效 |
|------|----------|
| `CLICK`, `BUTTON_CLICK` | minecraft:ui.button.click |
| `SUCCESS`, `LEVEL_UP` | minecraft:entity.player.levelup |
| `ERROR`, `NO` | minecraft:entity.villager.no |
| `PICKUP`, `ITEM_PICKUP` | minecraft:entity.item.pickup |
| `HIT`, `HURT` | minecraft:entity.player.hurt |
| `COIN`, `MONEY` | minecraft:entity.experience_orb.pickup |
| `TELEPORT` | minecraft:item.chorus_fruit.teleport |
| `SPELL`, `CAST_SPELL` | minecraft:entity.evoker.cast_spell |

---

### ServerService - 服务器服务

**包路径**: `cn.guangdian.rpgcore.server.ServerService`

**解决问题**:
- ❌ `Bukkit.spigot().restart()` 已弃用

#### API 参考

```java
// 获取服务
ServerService serverService = RPGCore.getInstance().getServerService();

// 服务器控制
serverService.restart();   // 重启服务器（带5秒延迟通知）
serverService.shutdown();  // 关闭服务器

// TPS 监控
String tps1m = serverService.getFormattedTPS(0);   // 1分钟 TPS
String tps5m = serverService.getFormattedTPS(1);   // 5分钟 TPS

// 内存监控
ServerService.MemoryInfo memory = serverService.getMemoryInfo();
long usedMB = memory.getUsedMemoryMB();
double usedPercent = memory.getUsedPercentage();

// 广播消息
serverService.broadcast(Component.text("§a服务器公告消息"));

// 执行命令
serverService.dispatchCommand("say 服务器即将重启");
```

---

### EntityService - 实体服务

**包路径**: `cn.guangdian.rpgcore.entity.EntityService`

**解决问题**:
- ❌ `VehicleEntityCollisionEvent.setCollisionCancelled()` 已弃用

#### API 参考

```java
// 获取服务
EntityService entityService = RPGCore.getInstance().getEntityService();

// 碰撞控制
entityService.setCollisionCancelled(entity, true);
entityService.handleVehicleCollision(event, true);

// 实体传送
boolean success = entityService.teleportSafely(entity, targetLocation);

// 实体属性
entityService.setInvulnerable(entity, true);
entityService.setSilent(entity, true);
entityService.setGlowing(entity, true);

// 距离计算
boolean inRange = entityService.isInRange(entity1, entity2, 10.0);
```

---

### PlayerLockManager - 玩家锁管理器

**包路径**: `cn.guangdian.rpgcore.concurrency.PlayerLockManager`

**功能**: 提供玩家级别的细粒度锁，支持超时机制和死锁预防。

#### API 参考

```java
RPGCore rpgCore = RPGCore.getInstance();
PlayerLockManager lockManager = rpgCore.getLockManager();

// 单锁操作
lockManager.executeWithLock(playerUUID, () -> {
    playerData.addPoints(100);
});

// 带返回值
int result = lockManager.executeWithLock(playerUUID, () -> {
    return playerData.getPoints();
});

// 双锁操作（转账场景）
boolean success = lockManager.executeWithDualLock(fromUUID, toUUID, () -> {
    if (fromData.hasPoints(amount)) {
        fromData.removePoints(amount);
        toData.addPoints(amount);
        return true;
    }
    return false;
});

// 异常处理
try {
    lockManager.executeWithLock(playerUUID, () -> {
        // 临界区操作
    });
} catch (LockTimeoutException e) {
    logger.warning("获取锁超时: " + e.getMessage());
}
```

---

### CacheProvider - 缓存提供者

**包路径**: `cn.guangdian.rpgcore.api.CacheProvider`

**功能**: 提供统一的缓存管理接口，基于 Caffeine 高性能缓存。

#### API 参考

```java
RPGCore rpgCore = RPGCore.getInstance();
CacheProvider cacheProvider = rpgCore.getCacheProvider();

// 创建 LoadingCache
LoadingCache<String, PlayerData> cache = cacheProvider.getLoadingCache(
    "playerData",
    uuid -> loadFromDatabase(uuid)
);

// 使用缓存
PlayerData data = cache.get(playerUUID);
cache.invalidate(key);
cache.invalidateAll();

// 缓存统计
CacheStats stats = cacheProvider.getStats("playerData");
System.out.println("命中率: " + stats.hitRate());
```

---

### EventPublisher - 事件发布器

**包路径**: `cn.guangdian.rpgcore.event.EventPublisher`

**功能**: 提供带管控的事件发布能力（性能监控、频率限制、日志记录）。

#### API 参考

```java
// 标准发布（带管控）
EventPublisher.publish(new MyCustomEvent(player, data));

// 异步发布（非关键事件）
EventPublisher.publishAsync(new MyCustomEvent(player, data));

// 延迟发布（tick）
EventPublisher.publishLater(new MyCustomEvent(player, data), 20L);

// 直接使用 Bukkit（备选）
Bukkit.getPluginManager().callEvent(new MyCustomEvent(player, data));
```

---

### MiniMessageService - 消息服务

**包路径**: `cn.guangdian.rpgcore.message.MiniMessageService`

**功能**: 提供 MiniMessage 格式的消息处理，支持颜色、渐变、点击事件等。

#### API 参考

```java
MiniMessageService mm = MiniMessageService.getInstance();

// 简单颜色消息
player.sendMessage(mm.red("错误消息"));
player.sendMessage(mm.green("成功消息"));
player.sendMessage(mm.yellow("警告消息"));
player.sendMessage(mm.aqua("信息消息"));
player.sendMessage(mm.gold("金色消息"));

// 复杂格式消息
player.sendMessage(mm.colorize("<gradient:#ff0000:#00ff00>渐变文字</gradient>"));

// 带点击事件
String clickable = "<click:run_command:/help><hover:show_text:'点击查看帮助'><yellow>点击这里</hover></click>";
player.sendMessage(mm.colorize(clickable));

// ActionBar
player.sendActionBar(mm.green("这是一条 ActionBar 消息"));
```

---

## 迁移指南

### 废弃 API 清单

| 废弃 API | 替代 API | 状态 | 迁移优先级 |
|---------|---------|------|-----------|
| `YamlDataStore` | `ConfigurateSupport` | `@Deprecated(forRemoval=true)` | 中 |
| `ServiceInjector` | `GuiceSupport` | `@Deprecated(forRemoval=true)` | 低 |
| `ColorUtil` | `MiniMessageService` | `@Deprecated(forRemoval=false)` | 低 |

### 1. YamlDataStore → ConfigurateSupport

**旧代码（已废弃）**
```java
import cn.guangdian.rpgcore.data.YamlDataStore;

public class OldConfigManager {
    private final YamlDataStore store = YamlDataStore.getInstance();
    
    public void loadConfig(File file) {
        Map<String, Object> data = store.load(file);
        String name = (String) data.get("name");
        int level = (int) data.get("level");
    }
}
```

**新代码（推荐）**
```java
import cn.guangdian.rpgcore.config.ConfigurateSupport;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PlayerConfig {
    private String name = "default";
    private int level = 1;
    
    public String getName() { return name; }
    public int getLevel() { return level; }
}

public class NewConfigManager {
    private ConfigurateSupport<PlayerConfig> config;
    
    public void init() {
        config = ConfigurateSupport.builder(PlayerConfig.class)
            .file("player.yml")
            .autoSave()
            .build();
    }
    
    public PlayerConfig getConfig() {
        return config.get();
    }
}
```

### 2. ServiceInjector → GuiceSupport

**旧代码（已废弃）**
```java
import cn.guangdian.rpgcore.inject.ServiceInjector;

public class OldService {
    @Inject
    private DatabaseService database;
    
    public OldService() {
        ServiceInjector.inject(this);
    }
}
```

**新代码（推荐）**
```java
import cn.guangdian.rpgcore.inject.GuiceSupport;

public class NewService {
    @Inject
    private DatabaseService database;
    
    public NewService() {
        GuiceSupport.injectMembers(this);
    }
}

// 或在插件中
public class MyPlugin extends AbstractRPGPlugin {
    @Inject
    private MyService myService;
    
    @Override
    protected void onPluginEnable() {
        initCommonServices();
        GuiceSupport.childInjector()
            .with(new MyModule())
            .inject(this);
    }
}
```

### 3. 音效迁移

**迁移前（弃用）**
```java
// ❌ 已弃用
Sound sound = Sound.valueOf("ENTITY_PLAYER_LEVELUP");
player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
```

**迁移后（RPGCore）**
```java
// ✅ 使用 RPGCore
SoundService soundService = RPGCore.getInstance().getSoundService();
soundService.playSound(player, "LEVEL_UP", 1.0f, 1.0f);
```

### 4. 服务器重启迁移

**迁移前（弃用）**
```java
// ❌ 已弃用
Bukkit.spigot().restart();
```

**迁移后（RPGCore）**
```java
// ✅ 使用 RPGCore
ServerService serverService = RPGCore.getInstance().getServerService();
serverService.restart();
```

### 5. 碰撞控制迁移

**迁移前（弃用）**
```java
// ❌ 已弃用
@EventHandler
public void onCollision(VehicleEntityCollisionEvent event) {
    event.setCollisionCancelled(true);
}
```

**迁移后（RPGCore）**
```java
// ✅ 使用 RPGCore
@EventHandler
public void onCollision(VehicleEntityCollisionEvent event) {
    EntityService entityService = RPGCore.getInstance().getEntityService();
    entityService.handleVehicleCollision(event, true);
}
```

---

## 最佳实践

### 1. 服务获取

```java
public class MyPlugin extends AbstractRPGPlugin {
    private SoundService soundService;
    private ServerService serverService;
    private EntityService entityService;

    @Override
    protected void onPluginEnable() {
        RPGCore rpgCore = RPGCore.getInstance();
        soundService = rpgCore.getSoundService();
        serverService = rpgCore.getServerService();
        entityService = rpgCore.getEntityService();
    }
}
```

### 2. 音效播放

```java
// 使用别名而不是硬编码字符串
soundService.playSound(player, "SUCCESS", 1.0f, 1.0f);  // ✅
// 而不是
soundService.playSound(player, "minecraft:entity.player.levelup", 1.0f, 1.0f);  // ❌
```

### 3. 错误处理

```java
// 检查返回值
Key soundKey = soundService.parseSound("INVALID_SOUND");
if (soundKey == null) {
    logger.warning("无效的音效名称");
    return;
}
```

### 4. 性能优化

```java
// 缓存服务引用，避免重复获取
private final SoundService soundService = RPGCore.getInstance().getSoundService();

// 使用距离检查而不是计算距离
boolean inRange = entityService.isInRange(entity1, entity2, 10.0);  // ✅
```

---

## 相关文档

- [DEVELOPMENT_GUIDE.md](../../rules/DEVELOPMENT_GUIDE.md) - 开发指南
- [BUILD_GUIDE.md](BUILD_GUIDE.md) - 构建指南

---

*最后更新: 2026-04-26*
