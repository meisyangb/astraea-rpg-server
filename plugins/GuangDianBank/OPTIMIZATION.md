# GuangDianBank 优化说明

> **优化日期**: 2026-04-14  
> **优化版本**: 1.1.0  
> **RPGCore 依赖**: >= 1.0.0

---

## 优化内容

### 1. 日志系统迁移到 RPGCore

#### 变更文件
- `GuangDianBank.java`

#### 优化前
```java
// 使用 Bukkit 原生日志
getLogger().info(getPluginName() + " 已启动");
getLogger().severe("无法创建数据文件: " + e.getMessage());
```

#### 优化后
```java
// 使用 RPGCore GameLogger
logInfo(getPluginName() + " 已启动");
logSevere("无法创建数据文件", e);  // 带异常堆栈
```

#### 降级兼容
当 RPGCore 不可用时，自动降级到 Bukkit 原生日志。

---

### 2. 消息发送系统迁移到 MiniMessage

#### 变更文件
- `GuangDianBank.java`
- `LoanManager.java`

#### 优化前
```java
// 使用 § 符号（已过时）
plugin.sendSuccess(player, "§a你成功贷款 §e" + amount + " §a，期限 " + durationDays + " 天");
plugin.sendError(player, "§c余额不足，无法还款！");
```

#### 优化后
```java
// 使用 RPGCore MiniMessageService
plugin.sendSuccess(player, "✓ 你成功贷款 " + amount + " ，期限 " + durationDays + " 天");
plugin.sendError(player, "✗ 余额不足，无法还款！");
```

#### 实现方式
```java
// GuangDianBank.java 中统一封装
public void sendSuccess(Player player, String message) {
    if (miniMessage != null) {
        player.sendMessage(miniMessage.green(message));
    } else {
        // 降级
        player.sendMessage(Component.text(message).color(NamedTextColor.GREEN));
    }
}

public void sendError(Player player, String message) {
    if (miniMessage != null) {
        player.sendMessage(miniMessage.red(message));
    } else {
        // 降级
        player.sendMessage(Component.text(message).color(NamedTextColor.RED));
    }
}
```

#### 优势
- 统一的 MiniMessage 格式
- 更简洁的 API
- 支持 RPGCore 的 MiniMessage 扩展功能
- 自动降级到 Adventure API

---

## 架构设计

```
┌─────────────────────────────────────────┐
│              RPGCore (核心层)            │
│  ┌─────────────┐ ┌──────────────────┐  │
│  │ GameLogger  │ │ MiniMessage      │  │
│  └─────────────┘ └──────────────────┘  │
└─────────────────────────────────────────┘
                    ▲
                    │ 优先使用
┌───────────────────┼─────────────────────┐
│     GuangDianBank (业务层)              │
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
| `GuangDianBank.java` | 修改 | 添加 RPGCore 服务初始化、日志快捷方法、消息发送方法 |
| `LoanManager.java` | 修改 | 替换 § 符号为 MiniMessage 消息发送 |

---

## 测试建议

1. **正常场景测试**
   - 启动服务器，确认 RPGCore 和 GuangDianBank 都正常加载
   - 执行存款/取款/贷款/还款操作
   - 检查消息颜色显示是否正常
   - 检查日志输出是否正常

2. **降级场景测试**
   - 临时移除 RPGCore，重启服务器
   - 确认 GuangDianBank 仍能正常工作
   - 检查消息降级到 Adventure API
   - 检查日志降级到 Bukkit 原生

3. **功能测试**
   - 贷款申请和发放
   - 贷款还款（全额/部分）
   - 逾期处理
   - 利息计算

---

## 后续优化建议

1. **结构化日志**
   ```java
   // 使用 RPGCore 结构化日志
   logger.infoStructured("loan_created", Map.of(
       "player", playerId.toString(),
       "amount", amount,
       "duration", durationDays
   ));
   ```

2. **缓存优化**
   ```java
   // 使用 RPGCore CacheProvider
   CacheProvider cache = rpgCore.getCacheProvider();
   ```

3. **异步数据库操作**
   ```java
   // 使用 RPGCore AsyncExecutor 进行数据库操作
   asyncExecutor.execute(() -> saveAccountData(playerId));
   ```

---

*最后更新: 2026-04-14*
