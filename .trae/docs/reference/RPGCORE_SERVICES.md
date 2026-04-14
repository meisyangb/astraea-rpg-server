# RPGCore 核心服务使用指南

> **版本**: 1.2.0 | **更新**: 2026-04-14 | **Paper**: 1.21.6

---

## 📋 目录

1. [概述](#概述)
2. [SoundService - 音效服务](#soundservice---音效服务)
3. [ServerService - 服务器服务](#serverservice---服务器服务)
4. [EntityService - 实体服务](#entityservice---实体服务)
5. [迁移指南](#迁移指南)
6. [最佳实践](#最佳实践)

---

## 概述

RPGCore 提供了一系列核心服务，封装了 Paper 1.21.6 中已弃用的 API，为子插件提供统一的调用接口。

### 获取服务

```java
RPGCore rpgCore = RPGCore.getInstance();

// 音效服务
SoundService soundService = rpgCore.getSoundService();

// 服务器服务
ServerService serverService = rpgCore.getServerService();

// 实体服务
EntityService entityService = rpgCore.getEntityService();
```

---

## SoundService - 音效服务

**包路径**: `cn.guangdian.rpgcore.sound.SoundService`

**解决问题**:
- ❌ `Sound.valueOf()` 已弃用
- ❌ `Sound.key()` 已弃用

### API 参考

#### 解析音效名称

```java
// 获取服务
SoundService soundService = RPGCore.getInstance().getSoundService();

// 使用别名解析
Key soundKey = soundService.parseSound("CLICK");        // minecraft:ui.button.click
Key soundKey2 = soundService.parseSound("SUCCESS");     // minecraft:entity.player.levelup
Key soundKey3 = soundService.parseSound("ERROR");       // minecraft:entity.villager.no

// 使用完整命名空间
Key soundKey4 = soundService.parseSound("minecraft:block.note_block.pling");

// 使用简写名称
Key soundKey5 = soundService.parseSound("block.note_block.harp");
```

#### 播放音效

```java
// 给指定玩家播放
soundService.playSound(player, "SUCCESS", 1.0f, 1.0f);
soundService.playSound(player, "COIN", 0.5f, 1.2f);

// 在指定位置播放
Location location = player.getLocation();
soundService.playSound(location, "CLICK", 1.0f, 1.0f);

// 广播给所有玩家
soundService.broadcastSound("LEVEL_UP", 1.0f, 1.0f);
```

#### 停止音效

```java
// 停止指定音效
soundService.stopSound(player, "MUSIC");

// 停止所有音效
soundService.stopAllSounds(player);
```

#### 创建 Adventure Sound 对象

```java
Sound sound = soundService.createAdventureSound(
    "SUCCESS", 
    Sound.Source.MASTER, 
    1.0f, 
    1.0f
);
player.playSound(sound);
```

### 支持的音效别名

| 别名 | 实际音效 |
|------|----------|
| **UI** | |
| `CLICK`, `BUTTON_CLICK` | minecraft:ui.button.click |
| **成功/失败** | |
| `SUCCESS`, `LEVEL_UP` | minecraft:entity.player.levelup |
| `ERROR`, `NO` | minecraft:entity.villager.no |
| **物品** | |
| `PICKUP`, `ITEM_PICKUP` | minecraft:entity.item.pickup |
| `BREAK`, `ITEM_BREAK` | minecraft:entity.item.break |
| **战斗** | |
| `HIT`, `HURT` | minecraft:entity.player.hurt |
| `ATTACK` | minecraft:entity.player.attack.strong |
| **音符盒** | |
| `NOTE_PLING` | minecraft:block.note_block.pling |
| `NOTE_HARP` | minecraft:block.note_block.harp |
| `NOTE_BASS` | minecraft:block.note_block.bass |
| **门/箱子** | |
| `CHEST_OPEN` | minecraft:block.chest.open |
| `CHEST_CLOSE` | minecraft:block.chest.close |
| `DOOR_OPEN` | minecraft:block.wooden_door.open |
| `DOOR_CLOSE` | minecraft:block.wooden_door.close |
| **经验** | |
| `EXP`, `EXP_ORB` | minecraft:entity.experience_orb.pickup |
| **交易** | |
| `TRADE` | minecraft:entity.villager.trade |
| `TRADE_YES` | minecraft:entity.villager.yes |
| **传送** | |
| `TELEPORT` | minecraft:item.chorus_fruit.teleport |
| `PORTAL` | minecraft:block.portal.travel |
| **施法** | |
| `SPELL`, `CAST_SPELL` | minecraft:entity.evoker.cast_spell |
| `PREPARE_SPELL` | minecraft:entity.evoker.prepare_summon |
| **金币** | |
| `COIN`, `MONEY` | minecraft:entity.experience_orb.pickup |

---

## ServerService - 服务器服务

**包路径**: `cn.guangdian.rpgcore.server.ServerService`

**解决问题**:
- ❌ `Bukkit.spigot().restart()` 已弃用

### API 参考

#### 服务器控制

```java
// 获取服务
ServerService serverService = RPGCore.getInstance().getServerService();

// 重启服务器（带5秒延迟通知）
serverService.restart();

// 关闭服务器
serverService.shutdown();
```

#### TPS 监控

```java
// 获取 TPS 数组（1分钟、5分钟、15分钟）
double[] tps = serverService.getTPS();

// 获取格式化后的 TPS
String tps1m = serverService.getFormattedTPS(0);   // 1分钟 TPS
String tps5m = serverService.getFormattedTPS(1);   // 5分钟 TPS
String tps15m = serverService.getFormattedTPS(2);  // 15分钟 TPS
```

#### 内存监控

```java
// 获取内存信息
ServerService.MemoryInfo memory = serverService.getMemoryInfo();

// 内存使用量（MB）
long usedMB = memory.getUsedMemoryMB();
long freeMB = memory.getFreeMemoryMB();
long totalMB = memory.getTotalMemoryMB();
long maxMB = memory.getMaxMemoryMB();

// 使用百分比
double usedPercent = memory.getUsedPercentage();

// 详细信息
System.out.println(memory);  // Memory[used=512MB, free=256MB, total=768MB, max=2048MB]
```

#### 运行时间

```java
// 获取服务器运行时间（毫秒）
long uptime = serverService.getUptime();

// 获取服务器启动时间戳
long startTime = serverService.getStartTime();
```

#### 广播消息

```java
// 广播给所有玩家
serverService.broadcast(Component.text("§a服务器公告消息"));

// 广播给有指定权限的玩家
serverService.broadcast(
    Component.text("§c管理员通知"), 
    "rpg.admin"
);
```

#### 执行命令

```java
// 以控制台身份执行命令
serverService.dispatchCommand("say 服务器即将重启");
serverService.dispatchCommand("save-all");
```

---

## EntityService - 实体服务

**包路径**: `cn.guangdian.rpgcore.entity.EntityService`

**解决问题**:
- ❌ `VehicleEntityCollisionEvent.setCollisionCancelled()` 已弃用

### API 参考

#### 碰撞控制

```java
// 获取服务
EntityService entityService = RPGCore.getInstance().getEntityService();

// 设置实体碰撞状态（替代 setCollisionCancelled）
entityService.setCollisionCancelled(entity, true);
entityService.setCollisionCancelled(entity, false);

// 检查碰撞状态
boolean cancelled = entityService.isCollisionCancelled(entity);

// 处理载具碰撞事件
@EventHandler
public void onVehicleCollision(VehicleEntityCollisionEvent event) {
    entityService.handleVehicleCollision(event, true);  // 取消碰撞
}
```

#### 实体传送

```java
// 安全传送（检查目标位置是否安全）
Location targetLocation = new Location(world, x, y, z);
boolean success = entityService.teleportSafely(entity, targetLocation);

// 检查位置是否安全
boolean safe = entityService.isLocationSafe(targetLocation);
```

#### 实体属性

```java
// 设置无敌状态
entityService.setInvulnerable(entity, true);
boolean invulnerable = entityService.isInvulnerable(entity);

// 设置静默状态
entityService.setSilent(entity, true);

// 设置发光状态
entityService.setGlowing(entity, true);

// 设置可见性
entityService.setVisible(entity, false);
```

#### 造成伤害

```java
// 对实体造成伤害
entityService.damage(entity, 10.0);
```

#### 距离计算

```java
// 计算两个实体之间的距离
double distance = entityService.getDistance(entity1, entity2);

// 计算距离的平方（性能更好）
double distanceSq = entityService.getDistanceSquared(entity1, entity2);

// 检查是否在指定范围内
boolean inRange = entityService.isInRange(entity1, entity2, 10.0);

// 获取朝向向量
Vector direction = entityService.getDirection(entity);
```

#### 清理状态

```java
// 清除指定实体的碰撞状态
entityService.clearCollisionState(entity.getUniqueId());

// 清除所有碰撞状态
entityService.clearAllCollisionStates();
```

---

## 迁移指南

### 音效迁移

**迁移前** (弃用):
```java
// ❌ 已弃用
Sound sound = Sound.valueOf("ENTITY_PLAYER_LEVELUP");
String key = sound.key().asString();
player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
```

**迁移后** (RPGCore):
```java
// ✅ 使用 RPGCore
SoundService soundService = RPGCore.getInstance().getSoundService();
soundService.playSound(player, "LEVEL_UP", 1.0f, 1.0f);
```

### 服务器重启迁移

**迁移前** (弃用):
```java
// ❌ 已弃用
Bukkit.spigot().restart();
```

**迁移后** (RPGCore):
```java
// ✅ 使用 RPGCore
ServerService serverService = RPGCore.getInstance().getServerService();
serverService.restart();
```

### 碰撞控制迁移

**迁移前** (弃用):
```java
// ❌ 已弃用
@EventHandler
public void onCollision(VehicleEntityCollisionEvent event) {
    event.setCollisionCancelled(true);
}
```

**迁移后** (RPGCore):
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
    // 处理无效音效
    logger.warning("无效的音效名称");
    return;
}
```

### 4. 性能优化

```java
// 缓存服务引用，避免重复获取
private final SoundService soundService = RPGCore.getInstance().getSoundService();

// 使用距离平方比较而不是距离
boolean inRange = entityService.isInRange(entity1, entity2, 10.0);  // ✅
// 而不是
double distance = entityService.getDistance(entity1, entity2);  // ❌ 性能较差
if (distance <= 10.0) { ... }
```

---

## 相关文档

- [FORBIDDEN_PATTERNS.md](../../rules/FORBIDDEN_PATTERNS.md) - 禁止模式清单
- [CODE_TEMPLATES.md](../../rules/CODE_TEMPLATES.md) - 代码模板
- [BUILD_GUIDE.md](BUILD_GUIDE.md) - 构建指南

---

*最后更新: 2026-04-14*
