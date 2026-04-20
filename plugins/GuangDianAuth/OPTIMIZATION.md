# GuangDianAuth 优化说明

> **优化日期**: 2026-04-14  
> **优化版本**: 1.1.0  
> **RPGCore 依赖**: >= 1.0.0

---

## 优化内容

### 1. 日志系统迁移到 RPGCore

#### 变更文件
- `GuangDianAuth.java`
- `AuthDataManager.java`
- `AuthCommands.java`

#### 优化前
```java
// 使用 Bukkit 原生日志
plugin.getLogger().info("玩家登录成功");
plugin.getLogger().severe("错误: " + e.getMessage());
```

#### 优化后
```java
// 使用 RPGCore GameLogger
plugin.logInfo("玩家登录成功");
plugin.logSevere("数据库错误", e);  // 带异常堆栈
```

#### 降级兼容
当 RPGCore 不可用时，自动降级到 Bukkit 原生日志。

---

### 2. 异步执行器迁移到 RPGCore

#### 变更文件
- `AuthDataManager.java`

#### 优化前
```java
public CompletableFuture<Boolean> isRegisteredAsync(String playerName) {
    return CompletableFuture.supplyAsync(() -> isRegistered(playerName));
}
```

#### 优化后
```java
public CompletableFuture<Boolean> isRegisteredAsync(String playerName) {
    if (asyncExecutor != null) {
        return asyncExecutor.execute(() -> isRegistered(playerName));
    }
    // 降级：使用默认线程池
    return CompletableFuture.supplyAsync(() -> isRegistered(playerName));
}
```

#### 优势
- 使用 RPGCore 统一管理的线程池
- 更好的性能和资源控制
- 自动降级保证兼容性

---

### 3. 消息发送迁移到 MiniMessageService

#### 变更文件
- `GuangDianAuth.java`
- `AuthCommands.java`

#### 优化前
```java
player.sendMessage(Component.text("登录成功").color(NamedTextColor.GREEN));
```

#### 优化后
```java
if (miniMessage != null) {
    player.sendMessage(miniMessage.green("✓ 登录成功"));
} else {
    // 降级
    player.sendMessage(Component.text("✓ 登录成功").color(NamedTextColor.GREEN));
}
```

#### 优势
- 统一的 MiniMessage 格式
- 更简洁的 API
- 支持 RPGCore 的 MiniMessage 扩展功能

---

## 架构设计

```
┌─────────────────────────────────────────┐
│              RPGCore (核心层)            │
│  ┌─────────────┐ ┌──────────────────┐  │
│  │ GameLogger  │ │ AsyncExecutor    │  │
│  │ MiniMessage │ │ (其他服务...)     │  │
│  └─────────────┘ └──────────────────┘  │
└─────────────────────────────────────────┘
                    ▲
                    │ 优先使用
┌───────────────────┼─────────────────────┐
│     GuangDianAuth (业务层)              │
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
| `GuangDianAuth.java` | 修改 | 添加 RPGCore 服务初始化、日志快捷方法 |
| `AuthDataManager.java` | 修改 | 使用 RPGCore GameLogger、AsyncExecutor |
| `AuthCommands.java` | 修改 | 使用 RPGCore MiniMessageService、GameLogger |

---

## 测试建议

1. **正常场景测试**
   - 启动服务器，确认 RPGCore 和 GuangDianAuth 都正常加载
   - 执行注册/登录/改密/注销操作
   - 检查日志输出是否正常

2. **降级场景测试**
   - 临时移除 RPGCore，重启服务器
   - 确认 GuangDianAuth 仍能正常工作
   - 检查日志降级到 Bukkit 原生

3. **性能测试**
   - 大量并发登录请求
   - 观察异步执行器性能

---

## 后续优化建议

1. **结构化日志**
   ```java
   // 使用 RPGCore 结构化日志
   logger.infoStructured("player_login", Map.of(
       "player", playerName,
       "ip", ip,
       "success", true
   ));
   ```

2. **采样日志**
   ```java
   // 高频日志使用采样
   logger.infoSampled("login-attempt", "登录尝试: " + playerName);
   ```

3. **缓存优化**
   ```java
   // 使用 RPGCore CacheProvider
   CacheProvider cache = rpgCore.getCacheProvider();
   ```

---

*最后更新: 2026-04-14*
