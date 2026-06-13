# GuangDianForge 优化说明

> **优化日期**: 2026-04-14  
> **优化版本**: 1.1.0  
> **RPGCore 依赖**: >= 1.0.0

---

## 优化内容

### 1. 日志系统迁移到 RPGCore

#### 变更文件
- `GuangDianForge.java`

#### 优化前
```java
// 使用 Bukkit 原生日志
getLogger().info("GuangDianForge 已启动! 加载了 " + recipeManager.getAllRecipes().size() + " 个图纸");
```

#### 优化后
```java
// 使用 RPGCore GameLogger
logInfo("GuangDianForge 已启动! 加载了 " + recipeManager.getAllRecipes().size() + " 个图纸");
```

#### 新增日志方法
- `logInfo(String)` - 信息日志
- `logWarning(String)` - 警告日志
- `logSevere(String)` - 严重错误日志
- `logSevere(String, Throwable)` - 带异常的日志
- `logDebug(String)` - 调试日志

#### 降级兼容
当 RPGCore 不可用时，自动降级到 Bukkit 原生日志。

---

### 2. 配置文件添加 MiniMessage 格式消息

#### 变更文件
- `config.yml`

#### 新增内容
```yaml
# 颜色格式说明（MiniMessage）：
# <green> - 绿色（成功消息）
# <red> - 红色（错误消息）
# <yellow> - 黄色（警告/提示消息）
# <gold> - 金色（高亮内容）
# ...

messages:
  # 锻造成功
  forge-success: "<green><bold>✓ 锻造成功！</bold></green> <white>你成功锻造出了 <gold><bold>%item%</bold></gold></white>"
  # 锻造失败
  forge-failure: "<red><bold>✗ 锻造失败！</bold></red> <gray>材料已消耗，但你的锻造技术得到了提升</gray>"
  # 等级提升
  level-up: "<gold><bold>🎉 锻造等级提升！</bold></gold> <white>当前等级: <gold><bold>%level%</bold></gold></white>"
  # ...

gui:
  # GUI 标题使用 MiniMessage 格式
  forge-gui-title: "<dark_gray><bold>🔨 锻造台</bold></dark_gray>"
  recipe-list-gui-title: "<dark_gray><bold>📜 图纸列表</bold></dark_gray>"
```

#### 支持的 MiniMessage 格式
- 基础颜色: `<green>`, `<red>`, `<yellow>`, `<gold>`, `<aqua>`, `<white>`, `<gray>`, `<dark_gray>`
- 格式: `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`
- 表情符号: `✓`, `✗`, `🔨`, `📜`, `🎉`, `⚡`

---

## 架构设计

```
┌─────────────────────────────────────────┐
│              RPGCore (核心层)            │
│  ┌─────────────┐ ┌──────────────────┐  │
│  │ GameLogger  │ │ AsyncExecutor    │  │
│  └─────────────┘ └──────────────────┘  │
└─────────────────────────────────────────┘
                    ▲
                    │ 优先使用
┌───────────────────┼─────────────────────┐
│     GuangDianForge (业务层)             │
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
| `GuangDianForge.java` | 修改 | 添加 RPGCore 服务初始化、日志快捷方法 |
| `config.yml` | 修改 | 添加 MiniMessage 格式的消息配置 |
| `OPTIMIZATION.md` | 新建 | 优化说明文档 |

---

## 测试建议

1. **正常场景测试**
   - 启动服务器，确认 RPGCore 和 GuangDianForge 都正常加载
   - 检查日志输出是否正常
   - 测试锻造功能，查看消息格式是否正确

2. **降级场景测试**
   - 临时移除 RPGCore，重启服务器
   - 确认 GuangDianForge 仍能正常工作
   - 检查日志降级到 Bukkit 原生

3. **功能测试**
   - 锻造成功/失败消息
   - 等级提升消息
   - 学习图纸消息
   - GUI 标题显示

---

## 后续优化建议

1. **结构化日志**
   ```java
   // 使用 RPGCore 结构化日志
   logger.infoStructured("forge_success", Map.of(
       "player", player.getName(),
       "item", itemName,
       "level", forgeLevel
   ));
   ```

2. **采样日志**
   ```java
   // 高频日志使用采样
   logger.infoSampled("forge-attempt", "锻造尝试统计...");
   ```

3. **缓存优化**
   ```java
   // 使用 RPGCore CacheProvider 缓存图纸数据
   CacheProvider cache = rpgCore.getCacheProvider();
   ```

---

*最后更新: 2026-04-14*
