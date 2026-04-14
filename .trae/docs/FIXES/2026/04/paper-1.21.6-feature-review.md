# Paper 1.21.6 新特性采用情况审查报告

> 审查日期: 2026-04-14
> 审查范围: 所有插件 (39个)

---

## 📊 总体评估

| 指标 | 结果 | 状态 |
|------|------|------|
| 插件总数 | 39 个 | ✅ |
| 正确继承 AbstractRPGPlugin | 39/39 (100%) | ✅ |
| 使用 TextDisplay 替代 ArmorStand | 10 个文件 | ✅ |
| 使用 SoundService | 16 个文件 | ✅ |
| 无 BukkitRunnable | 0 个文件 | ✅ |
| 无 Bukkit.getPlugin("RPGCore") | 0 个文件 | ✅ |

---

## ✅ 已正确采用的 Paper 1.21.6 新特性

### 1. Display Entities (TextDisplay)
**状态**: ✅ 已采用

已正确使用 `TextDisplay` 替代 `ArmorStand` 用于全息图显示：
- GuangDianName - TextDisplayManager.java
- GuangDianHolo - HologramManager.java
- GuangDianMobHealth - MobHealthDisplayManager.java

### 2. AbstractRPGPlugin 架构
**状态**: ✅ 完全采用

所有 39 个插件都正确继承了 `AbstractRPGPlugin` 而非 `JavaPlugin`。

### 3. SyncScheduler 调度器
**状态**: ✅ 完全采用

- 无 `BukkitRunnable` 使用
- 无 `Bukkit.getScheduler()` 直接调用（仅在 RPGCore 内部实现中使用）

### 4. SoundService 声音系统
**状态**: ✅ 已采用

16 个文件正确使用 `SoundService.getInstance().playSound()`：
- GuangDianTrade
- GuangDianDecompose
- GuangDianLocation
- GuangDianMarket
- GuangDianMenu
- GuangDianArmorStats

### 5. ExternalServiceIntegration
**状态**: ✅ 正确封装

外部服务调用已正确封装在 `ExternalServiceIntegrationImpl` 中：
- LuckPerms 调用
- PlaceholderAPI 调用

---

## 🔴 需立即修复的问题

### 1. sendTitle 废弃方法 (1 个文件)

**文件**: `GuangDianRaid/src/main/java/cn/guangdian/raid/model/RaidTeam.java:85`

**问题代码**:
```java
player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
```

**修复方案**:
```java
AudienceService audience = AudienceService.getInstance();
Component titleComp = MiniMessageService.getInstance().parse(title);
Component subtitleComp = MiniMessageService.getInstance().parse(subtitle);
audience.showTitle(player, titleComp, subtitleComp, fadeIn, stay, fadeOut);
```

### 2. LegacyComponentSerializer.legacySection() (1 个文件)

**文件**: `GuangDianChat/src/main/java/cn/guangdian/chat/adapter/ChatServiceAdapter.java`

**问题代码** (第73、121、130行):
```java
return LegacyComponentSerializer.legacySection().deserialize(suffix);
```

**修复方案**:
```java
return LegacyComponentSerializer.legacyAmpersand().deserialize(suffix);
```

---

## ⚠️ 建议改进的问题

### 1. ChatColor 直接使用 (28 个文件)

**严重程度**: ⚠️ 警告

**问题**: 部分文件直接使用 `ChatColor` 常量构建消息

**影响文件**:
- GemParser.java - 使用 `ChatColor.DARK_RED`, `ChatColor.AQUA` 等

**建议**: 
- 配置文件颜色转换使用 `ChatColor.translateAlternateColorCodes('&', text)` 是允许的
- 直接构建消息应改用 `ColorUtil` 或 MiniMessage

### 2. § 颜色符号使用 (64 个文件)

**严重程度**: ⚠️ 警告

**问题**: 代码中使用了 `§` 颜色符号

**示例**:
```java
// GuangDianTrade.java
prefix = "... §6[§e交易§6] "
```

**建议**: 代码中应使用 `&` 符号或 MiniMessage 标签，`§` 符号仅用于配置文件

### 3. Bukkit.createBossBar 使用 (1 个文件)

**文件**: `GuangDianArmorStats/src/main/java/cn/guangdian/armorstats/manager/BossBarManager.java:84`

**建议**: 使用 `AdventureBossBarService` 替代

### 4. NamespacedKey 创建方式 (约10 个文件)

**建议**: 使用 `PDCKeys.key()` 或 `PDCKeys.playerData()` 替代 `new NamespacedKey(plugin, key)`

---

## 📋 问题汇总

| 问题类型 | 严重程度 | 文件数 | 优先级 |
|----------|----------|--------|--------|
| sendTitle 废弃方法 | 🔴 严重 | 1 | 高 |
| legacySection() 使用 | 🔴 严重 | 1 | 高 |
| Bukkit.createBossBar | ⚠️ 警告 | 1 | 中 |
| ChatColor 直接使用 | ⚠️ 警告 | 28 | 低 |
| § 颜色符号 | ⚠️ 警告 | 64 | 低 |
| NamespacedKey 创建 | ⚠️ 警告 | ~10 | 低 |

---

## 📝 已更新的文档

1. **FORBIDDEN_PATTERNS.md** - 添加了 Paper 1.21.6 新特性禁止模式
2. **CODE_TEMPLATES.md** - 添加了 8 个新模板
3. **code-reviewer/SKILL.md** - 添加了 Paper 1.21.6 检查清单
4. **WORKFLOW.md** - 添加了新特性检查流程
5. **TASK_WORKFLOW.md** - 添加了编码检查项

---

## 🎯 下一步行动

### 高优先级 (立即修复)
- [ ] 修复 RaidTeam.java 的 sendTitle 方法
- [ ] 修复 ChatServiceAdapter.java 的 legacySection 使用

### 中优先级 (建议修复)
- [ ] 改进 BossBarManager.java 使用 AdventureBossBarService

### 低优先级 (可选改进)
- [ ] 逐步迁移 ChatColor 直接使用到 ColorUtil/MiniMessage
- [ ] 逐步迁移 NamespacedKey 创建到 PDCKeys 工具类

---

*审查人: AI Assistant*
*审查日期: 2026-04-14*
