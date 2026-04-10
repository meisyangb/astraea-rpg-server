---
id: mem-fix-scheduler-migration
type: memory
category: fixes
title: "调度器迁移修复方案"
description: "BukkitRunnable 和 Bukkit.getScheduler() 迁移到 RPGCore SyncScheduler 的完整方案"
date: 2026-04-10
version: 1.0.0
tags: [fix, scheduler, migration, bukkitrunnable, syncscheduler, pattern]
related:
  - log-2026-04-10-scheduler-migration
  - mem-pattern-scheduler-usage
  - doc-skill-minecraft-rpg-architect
status: published
---

# 调度器迁移修复方案

## 问题描述

**症状**: 多个插件使用 `Bukkit.getScheduler()` 和 `BukkitRunnable`，导致：
1. 任务分散在各插件，难以统一管理
2. 任务取消逻辑不完整，可能导致内存泄漏
3. 无法集中监控任务执行情况

**影响范围**: 17个 GuangDian* 插件，47处违规调用

---

## 解决方案

使用 RPGCore 的 **SyncScheduler** 统一调度管理

### 核心优势

| 特性 | Bukkit Scheduler | RPGCore SyncScheduler |
|------|------------------|----------------------|
| 任务管理 | 分散 | 集中统一 |
| 取消机制 | 需手动跟踪 | 自动管理 |
| 监控能力 | 无 | `getActiveTaskCount()` |
| 内存占用 | 每个任务独立对象 | 统一 Map 存储 |

---

## 正确代码示例

### 1. 基本使用

```java
// 获取调度器
RPGCore rpgCore = RPGCore.getInstance();
if (rpgCore == null) return;

SyncScheduler scheduler = rpgCore.getScheduler();

// 延迟执行 (50 ticks = 2.5秒)
long taskId = scheduler.runSyncLater(() -> {
    // 业务逻辑
}, 50L);

// 循环执行 (立即开始，每秒执行)
long repeatingId = scheduler.runSyncRepeating(() -> {
    // 业务逻辑
}, 0L, 20L);

// 异步执行
scheduler.runAsync(() -> {
    // 耗时操作
});
```

### 2. 在 AbstractRPGPlugin 中使用

```java
public class MyPlugin extends AbstractRPGPlugin {
    
    private long updateTaskId = -1;
    
    @Override
    protected void onPluginEnable() {
        // scheduler 已自动注入
        updateTaskId = scheduler.runSyncRepeating(this::update, 0L, 20L);
    }
    
    @Override
    protected void onPluginDisable() {
        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
    }
    
    private void update() {
        // 更新逻辑
    }
}
```

### 3. 任务取消

```java
// 取消单个任务
scheduler.cancelTask(taskId);

// 取消所有任务 (推荐在 onDisable 中使用)
scheduler.cancelAllTasks();
```

---

## 迁移步骤

### Step 1: 添加字段
```java
private SyncScheduler scheduler;
private long taskId = -1;  // 用于跟踪任务
```

### Step 2: 初始化调度器
```java
@Override
protected void onPluginEnable() {
    RPGCore rpgCore = RPGCore.getInstance();
    if (rpgCore != null) {
        this.scheduler = rpgCore.getScheduler();
    }
}
```

### Step 3: 替换调度器调用
```java
// 修改前
Bukkit.getScheduler().runTaskLater(plugin, task, delay);

// 修改后
if (scheduler != null) {
    taskId = scheduler.runSyncLater(task, delay);
}
```

### Step 4: 添加取消逻辑
```java
@Override
protected void onPluginDisable() {
    if (scheduler != null) {
        scheduler.cancelAllTasks();
    }
}
```

---

## 常见错误

### 错误1: 未检查 null
```java
// ❌ 错误
scheduler.runSyncLater(() -> {...}, 50L);  // 可能 NPE

// ✅ 正确
if (scheduler != null) {
    scheduler.runSyncLater(() -> {...}, 50L);
}
```

### 错误2: 未保存 taskId
```java
// ❌ 错误 - 无法取消特定任务
scheduler.runSyncRepeating(() -> {...}, 0L, 20L);

// ✅ 正确
long taskId = scheduler.runSyncRepeating(() -> {...}, 0L, 20L);
// 需要时可以取消
scheduler.cancelTask(taskId);
```

### 错误3: 忘记取消任务
```java
// ❌ 错误 - 插件卸载后任务仍在运行
@Override
protected void onPluginDisable() {
    // 没有取消任务！
}

// ✅ 正确
@Override
protected void onPluginDisable() {
    scheduler.cancelAllTasks();
}
```

---

## 验证清单

- [ ] 所有 `Bukkit.getScheduler()` 已替换
- [ ] 所有 `new BukkitRunnable()` 已替换
- [ ] 添加了 `SyncScheduler` 字段
- [ ] 任务ID已保存（如需单独取消）
- [ ] `onDisable()` 中调用了 `cancelAllTasks()`
- [ ] 检查了 `scheduler != null`

---

## 相关日志

- [2026-04-10 调度器迁移日志](../../logs/categories/migration/2026-04-10-scheduler-migration.md)

---

**创建时间**: 2026-04-10  
**版本**: 1.0.0  
**状态**: 已发布
