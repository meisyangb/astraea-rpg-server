# 资源关闭顺序模式

> 记录线程池、调度器等资源的正确关闭顺序

## 问题背景

在插件卸载时，如果资源关闭顺序不当，可能导致：
- 异步任务数据丢失
- 线程池未正确终止
- 资源泄漏

## 正确关闭模式

### 线程池关闭

```java
// ✅ 正确顺序
executor.shutdown();  // 第一步：停止接受新任务
try {
    // 第二步：等待现有任务完成
    if (!executor.awaitTermination(timeout, TimeUnit.SECONDS)) {
        // 第三步：超时后强制关闭
        executor.shutdownNow();
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

### 调度器关闭

```java
// ✅ 正确顺序
// 1. 先处理剩余任务/事件
processRemainingEvents();

// 2. 取消定时任务
if (scheduledTask != null) {
    scheduledTask.cancel(false);
}

// 3. 关闭调度器
if (scheduler != null) {
    scheduler.shutdown();
    try {
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
        }
    } catch (InterruptedException e) {
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

## 错误示例

```java
// ❌ 错误：awaitTermination 在 shutdown 之前
executor.awaitTermination(30, TimeUnit.SECONDS);  // 错误！
executor.shutdown();

// ❌ 错误：没有处理剩余任务就直接关闭
scheduler.shutdown();
// 剩余任务丢失！
```

## RPGCore 应用实例

### AsyncExecutor 关闭

位置: `RPGCore.java` onDisable()

```java
if (asyncExecutor != null) {
    getLogger().info("Waiting for async tasks to complete...");
    asyncExecutor.shutdown();
    try {
        if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
            getLogger().warning("Async tasks did not complete in time, forcing shutdown");
            asyncExecutor.shutdownNow();
        }
    } catch (InterruptedException e) {
        asyncExecutor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

### EventBus 批量处理器关闭

位置: `SimpleEventBus.java` shutdown()

```java
public void shutdown() {
    // 先处理剩余事件，避免丢失
    processRemainingEvents();
    
    if (batchTask != null) {
        batchTask.cancel(false);
    }
    if (batchScheduler != null) {
        batchScheduler.shutdown();
        try {
            if (!batchScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                batchScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

## 检查清单

- [ ] `shutdown()` 在 `awaitTermination()` 之前调用
- [ ] 有超时强制关闭逻辑
- [ ] 正确处理 `InterruptedException`
- [ ] 先处理剩余任务/事件再关闭
- [ ] 恢复中断状态 `Thread.currentThread().interrupt()`

## 相关文件

- `RPGCore.java` - 主插件关闭逻辑
- `SimpleEventBus.java` - 事件总线关闭
- `AsyncExecutorImpl.java` - 异步执行器

---
*记录时间: 2026-04-14*
*类型: 代码审查发现*
