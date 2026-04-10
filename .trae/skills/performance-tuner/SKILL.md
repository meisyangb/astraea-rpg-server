---
name: performance-tuner
description: "性能优化专家 - 分析性能瓶颈、优化代码执行效率、JVM调优、内存管理。在性能问题或优化需求时调用。"
---

# 性能优化专家 (Performance Tuner)

> 专业的性能分析和优化技能，提升系统运行效率

---

## 优化领域

### 1. CPU 优化
- 减少不必要的计算
- 优化算法复杂度
- 避免主线程阻塞
- 使用异步处理

### 2. 内存优化
- 减少对象创建
- 使用对象池
- 及时释放资源
- 避免内存泄漏

### 3. I/O 优化
- 数据库查询优化
- 缓存策略设计
- 批量操作
- 异步I/O

### 4. 网络优化
- 减少数据包发送
- 压缩传输数据
- 合理的心跳机制

---

## 常见性能问题及解决方案

### 问题1: 主线程阻塞
```java
// ❌ 错误 - 在主线程执行耗时操作
public void onPlayerJoin(Player player) {
    // 查询数据库 - 阻塞主线程！
    List<Data> data = database.queryAll();
    process(data);
}

// ✅ 正确 - 异步执行
public void onPlayerJoin(Player player) {
    scheduler.runAsync(() -> {
        List<Data> data = database.queryAll();
        // 切回主线程更新
        scheduler.runSyncLater(() -> {
            process(data);
        }, 0L);
    });
}
```

### 问题2: 循环中创建对象
```java
// ❌ 错误 - 每次循环都创建对象
for (Player player : Bukkit.getOnlinePlayers()) {
    String message = new String("Hello " + player.getName());
    player.sendMessage(message);
}

// ✅ 正确 - 复用对象或使用StringBuilder
StringBuilder sb = new StringBuilder();
for (Player player : Bukkit.getOnlinePlayers()) {
    sb.setLength(0);
    sb.append("Hello ").append(player.getName());
    player.sendMessage(sb.toString());
}
```

### 问题3: 频繁的数据库查询
```java
// ❌ 错误 - 每个玩家都查询数据库
for (Player player : Bukkit.getOnlinePlayers()) {
    Data data = database.query(player.getUniqueId());
    updateDisplay(player, data);
}

// ✅ 正确 - 批量查询 + 缓存
// 方案1: 批量查询
List<UUID> uuids = new ArrayList<>();
for (Player player : Bukkit.getOnlinePlayers()) {
    uuids.add(player.getUniqueId());
}
Map<UUID, Data> dataMap = database.queryBatch(uuids);

// 方案2: 使用缓存
for (Player player : Bukkit.getOnlinePlayers()) {
    Data data = cache.get(player.getUniqueId(), () -> {
        return database.query(player.getUniqueId());
    });
    updateDisplay(player, data);
}
```

### 问题4: 不必要的定时任务
```java
// ❌ 错误 - 每个玩家一个定时任务
Map<UUID, Long> tasks = new HashMap<>();
public void startTracking(Player player) {
    long taskId = scheduler.runSyncRepeating(() -> {
        updatePlayer(player);
    }, 0L, 20L);
    tasks.put(player.getUniqueId(), taskId);
}

// ✅ 正确 - 一个全局任务处理所有玩家
long globalTaskId = scheduler.runSyncRepeating(() -> {
    for (Player player : Bukkit.getOnlinePlayers()) {
        updatePlayer(player);
    }
}, 0L, 20L);
```

---

## 性能分析工具

### 1. 代码级分析
```java
// 计时工具
long start = System.nanoTime();
// 待测代码
long duration = System.nanoTime() - start;
plugin.getLogger().info("耗时: " + (duration / 1_000_000) + "ms");
```

### 2. 内存分析
```java
// 内存使用检查
Runtime runtime = Runtime.getRuntime();
long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
plugin.getLogger().info("内存使用: " + usedMemory + "MB");
```

### 3. Spark 性能分析器
```yaml
# 使用 Spark 进行详细分析
# https://www.spigotmc.org/resources/spark.57242/
```

---

## 优化检查清单

### 代码层面
- [ ] 无主线程阻塞操作
- [ ] 循环中无数据库查询
- [ ] 无频繁的对象创建
- [ ] 使用 StringBuilder 进行字符串拼接
- [ ] 集合初始容量合理设置
- [ ] 及时清理不再使用的数据

### 架构层面
- [ ] 使用缓存减少重复计算
- [ ] 异步处理耗时操作
- [ ] 批量操作代替单条操作
- [ ] 合理的任务调度策略
- [ ] 数据压缩和延迟加载

### 配置层面
- [ ] JVM 参数优化
- [ ] 数据库连接池配置
- [ ] 缓存过期策略
- [ ] 日志级别设置

---

## 输出格式

```
╔══════════════════════════════════════════════════════════╗
║  ⚡ 性能分析报告                                         ║
╠══════════════════════════════════════════════════════════╣
║  分析目标: [文件/方法/系统]                              ║
║  分析时间: [时间]                                        ║
╠══════════════════════════════════════════════════════════╣
║  性能瓶颈:                                               ║
║    🔴 严重: [问题描述] - 影响程度: [高/中/低]            ║
║    🟡 警告: [问题描述] - 影响程度: [高/中/低]            ║
║    🟢 建议: [问题描述] - 影响程度: [高/中/低]            ║
╠══════════════════════════════════════════════════════════╣
║  优化建议:                                               ║
║    1. [具体建议]                                         ║
║       → 预期提升: [百分比]                               ║
║       → 实现复杂度: [简单/中等/复杂]                     ║
║                                                          ║
║    2. [具体建议]                                         ║
║       → 预期提升: [百分比]                               ║
║       → 实现复杂度: [简单/中等/复杂]                     ║
╠══════════════════════════════════════════════════════════╣
║  优化代码示例:                                           ║
║    [代码对比]                                            ║
╚══════════════════════════════════════════════════════════╝
```

---

*技能版本: 1.0*
*最后更新: 2026-04-10*
