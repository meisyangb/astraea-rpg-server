# GuangDianAggro 优化说明

> **优化日期**: 2026-04-14  
> **优化版本**: 1.1.0  
> **RPGCore 依赖**: >= 1.0.0

---

## 优化内容

### 1. 日志系统迁移到 RPGCore

#### 变更文件
- `GuangDianAggro.java`
- `AggroManager.java`

#### 优化前
```java
// 使用 Bukkit 原生日志
getLogger().info("GuangDianAggro 仇恨系统插件已启用!");
getLogger().info("MythicMobs 集成: " + (mythicMobsHook.isEnabled() ? "已启用" : "未启用"));
```

#### 优化后
```java
// 使用 RPGCore GameLogger
logInfo("GuangDianAggro 仇恨系统插件已启用!");
logInfo("MythicMobs 集成: " + (mythicMobsHook.isEnabled() ? "已启用" : "未启用"));
```

#### 新增调试日志
```java
// AggroManager 中新增调试日志
plugin.logDebug("仇恨衰减: " + decayedCount + " 个目标衰减, " + clearedCount + " 个实体清空");
plugin.logDebug("仇恨转移: " + from.getName() + " -> " + to.getName() + " (" + percentage + "%)");
plugin.logDebug("强制目标: " + entity.getType() + " -> " + target.getName());
```

#### 降级兼容
当 RPGCore 不可用时，自动降级到 Bukkit 原生日志。

---

### 2. 任务调度使用 RPGCore

#### 变更文件
- `AggroManager.java`

#### 优化前
```java
// 直接使用 RPGCore 但未检查
RPGCore rpgCore = plugin.getRPGCore();
if (rpgCore == null || rpgCore.getScheduler() == null) return;
```

#### 优化后
```java
// 添加日志和警告
RPGCore rpgCore = plugin.getRPGCore();
if (rpgCore == null || rpgCore.getScheduler() == null) {
    plugin.logWarning("RPGCore 调度器不可用，仇恨衰减任务未启动");
    return;
}

plugin.logInfo("仇恨衰减任务已启动，任务ID: " + decayTaskId);
```

---

## 架构设计

```
┌─────────────────────────────────────────┐
│              RPGCore (核心层)            │
│  ┌─────────────┐ ┌──────────────────┐  │
│  │ GameLogger  │ │ SyncScheduler    │  │
│  └─────────────┘ └──────────────────┘  │
└─────────────────────────────────────────┘
                    ▲
                    │ 优先使用
┌───────────────────┼─────────────────────┐
│     GuangDianAggro (业务层)             │
│  ┌─────────────────────────────────┐   │
│  │  if (rpgCore != null) {         │   │
│  │      useRPGCoreServices();      │   │
│  │  } else {                       │   │
│  │      useFallback(); // 降级     │   │
│  │  }                              │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

---

## 兼容性说明

| 场景 | 行为 |
|------|------|
| RPGCore 存在且正常 | 使用 RPGCore 服务（高性能） |
| RPGCore 不存在 | 自动降级到 Bukkit 原生（兼容） |
| RPGCore 异常 | 使用降级方案（稳定） |

---

## 文件变更列表

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `GuangDianAggro.java` | 修改 | 添加 RPGCore 服务初始化、日志快捷方法 |
| `AggroManager.java` | 修改 | 添加日志记录、优化任务调度提示 |

---

## 测试建议

1. **正常场景测试**
   - 启动服务器，确认 RPGCore 和 GuangDianAggro 都正常加载
   - 检查日志输出是否正常
   - 测试仇恨计算和衰减功能

2. **降级场景测试**
   - 临时移除 RPGCore，重启服务器
   - 确认 GuangDianAggro 仍能正常工作
   - 检查日志降级到 Bukkit 原生

3. **功能测试**
   - 攻击怪物产生仇恨
   - 治疗队友产生仇恨
   - 仇恨衰减和清空
   - 仇恨转移

---

## 后续优化建议

1. **结构化日志**
   ```java
   // 使用 RPGCore 结构化日志
   logger.infoStructured("aggro_transfer", Map.of(
       "entity", entity.getType().toString(),
       "from", from.getName(),
       "to", to.getName(),
       "percentage", percentage
   ));
   ```

2. **采样日志**
   ```java
   // 高频日志使用采样
   logger.infoSampled("aggro-decay", "仇恨衰减统计...");
   ```

3. **缓存优化**
   ```java
   // 使用 RPGCore CacheProvider 缓存仇恨数据
   CacheProvider cache = rpgCore.getCacheProvider();
   ```

---

*最后更新: 2026-04-14*
