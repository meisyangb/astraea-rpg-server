# GuangDianCleaner 优化记录

## 优化时间
2026-04-14

## 优化内容

### 1. 主类优化 (GuangDianCleaner.java)

#### 新增 RPGCore GameLogger 集成
- 添加 `GameLogger gameLogger` 字段
- 新增 `initRPGCoreServices()` 方法，用于初始化 RPGCore 服务
- 新增日志辅助方法：
  - `logInfo(String message)` - 信息日志
  - `logWarning(String message)` - 警告日志
  - `logSevere(String message)` - 严重错误日志
  - `logDebug(String message)` - 调试日志

#### 优先级模式实现
- 优先使用 RPGCore 的 GameLogger
- 如果 RPGCore 不可用，自动降级到 Bukkit Logger
- 确保插件在有无 RPGCore 的环境下都能正常工作

#### 代码更新
- 将所有 `getLogger()` 调用替换为新的日志辅助方法
- 添加类级 Javadoc 说明 RPGCore 服务集成

### 2. 配置文件更新 (config.yml)

将消息配置从旧版 `&` 颜色代码更新为 MiniMessage 格式：

**更新前:**
```yaml
messages:
  prefix: "&6[扫地娘] &r"
  warning: "&e地面物品将在 &c%time% &e秒后清理!"
```

**更新后:**
```yaml
messages:
  prefix: "<gold>[扫地娘] <reset>"
  warning: "<yellow>地面物品将在 <red>%time% <yellow>秒后清理!"
```

## 技术细节

### RPGCore 服务使用
```java
// 初始化 RPGCore 服务
private void initRPGCoreServices() {
    RPGCore rpgCore = RPGCore.getInstance();
    if (rpgCore != null) {
        gameLogger = rpgCore.getGameLogger();
        if (gameLogger != null) {
            logInfo("已连接到 RPGCore GameLogger");
        }
    }
    if (gameLogger == null) {
        logInfo("使用 Bukkit Logger（降级）");
    }
}

// 日志方法（带降级处理）
public void logInfo(String message) {
    if (gameLogger != null) {
        gameLogger.info(message);
    } else {
        getLogger().info(message);
    }
}
```

### 已有的 RPGCore 集成
- ✅ `AbstractRPGPlugin` 基类
- ✅ `SyncScheduler` 任务调度器
- ✅ `CleanerServiceAdapter` 服务适配器
- ✅ `GameLogger` 日志服务（本次新增）

## 兼容性

- **Paper 1.21.6**: 完全兼容
- **RPGCore**: 优先使用 RPGCore 服务
- **独立运行**: 支持（自动降级到本地实现）

## 构建验证

```cmd
gradle build -x test
```

构建状态: ✅ 通过
