# GuangDianItemTrigger 优化记录

## 优化时间
2026-04-14

## 优化内容

### 1. 主类优化 (GuangDianItemTrigger.java)

#### 新增 RPGCore GameLogger 集成
- 添加 `GameLogger gameLogger` 字段
- 新增 `initRPGCoreServices()` 方法，用于初始化 RPGCore 服务
- 新增日志辅助方法：
  - `logInfo(String message)` - 信息日志
  - `logWarning(String message)` - 警告日志
  - `logSevere(String message)` - 严重错误日志
  - `logSevere(String message, Throwable throwable)` - 带异常的严重错误日志
  - `logDebug(String message)` - 调试日志

#### 优先级模式实现
- 优先使用 RPGCore 的 GameLogger
- 如果 RPGCore 不可用，自动降级到 Bukkit Logger
- 确保插件在有无 RPGCore 的环境下都能正常工作

#### 消息格式更新
- 将所有硬编码消息更新为 MiniMessage 格式
- 使用 `<red>`、`<green>`、`<yellow>`、`<aqua>` 等标签替代旧版颜色代码

### 2. 配置文件检查

配置文件 `config.yml` 已经使用 MiniMessage 格式：
- 消息配置: `<red>你没有权限执行此操作!`
- 触发器动作: `message:<green>触发 <gold>雷霆审判 <green>!`
- 无需修改

## 技术细节

### RPGCore 服务使用
```java
// 初始化 RPGCore 服务
private void initRPGCoreServices() {
    if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
        try {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                gameLogger = rpgCore.getGameLogger();
                // ...
            }
        } catch (Exception e) {
            logWarning("连接 RPGCore 服务失败: " + e.getMessage());
        }
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

### MiniMessage 格式示例
```yaml
messages:
  no-permission: "<red>你没有权限执行此操作!"
  cooldown: "<red>该物品还在冷却中，剩余时间: <yellow>%time%<red>秒"
  trigger-success: "<green>触发成功!"
```

## 兼容性

- **Paper 1.21.6**: 完全兼容
- **RPGCore**: 优先使用 RPGCore 服务
- **独立运行**: 支持（自动降级到 Bukkit Logger）

## 构建验证

```cmd
gradle build -x test
```

构建状态: ✅ 通过
