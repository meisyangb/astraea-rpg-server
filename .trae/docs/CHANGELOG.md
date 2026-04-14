# Changelog

> Astraea RPG 版本更新日志
> 
> 格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)
> 版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)

---

## [1.2.0] - 2026-04-14

### 🆕 Added
- **SoundService 音效服务**: 封装 Paper 1.21.6 弃用的 `Sound.valueOf()` 和 `Sound.key()` API
  - 支持 20+ 常用音效别名（CLICK, SUCCESS, ERROR, COIN, TELEPORT, SPELL 等）
  - 提供 `playSound()`, `broadcastSound()`, `stopSound()` 等方法
  - 位置: `cn.guangdian.rpgcore.sound.SoundService`
- **ServerService 服务器服务**: 封装 `Bukkit.spigot().restart()` 弃用 API
  - 提供 `restart()`, `shutdown()` 服务器控制方法
  - 提供 TPS 监控、内存监控、运行时间查询
  - 位置: `cn.guangdian.rpgcore.server.ServerService`
- **EntityService 实体服务**: 封装 `setCollisionCancelled()` 弃用 API
  - 提供实体碰撞控制、安全传送、距离计算
  - 提供实体属性管理（无敌、静默、发光、可见性）
  - 位置: `cn.guangdian.rpgcore.entity.EntityService`
- **RPGCore 服务导出**: 添加 `getSoundService()`, `getServerService()`, `getEntityService()` 方法

### 📚 Documentation
- 添加 `.trae/docs/reference/RPGCORE_SERVICES.md` 核心服务使用指南
- 更新 `.trae/rules/kaifa.md` 工作流文档（v1.2.0）
- 更新 `.trae/skills/minecraft-rpg-architect/SKILL.md` 技能文档（v2.3.0）
- 添加 RPGCore 服务使用示例和迁移指南

### 🔄 Changed
- **版本更新**: 
  - `kaifa.md`: 1.1.0 → 1.2.0
  - `minecraft-rpg-architect/SKILL.md`: 2.2.0 → 2.3.0

---

## [1.1.0] - 2026-04-14

### 🆕 Added
- **MiniMessage 颜色服务**: 添加 `MiniMessageService` 支持 Adventure MiniMessage 格式
- **Caffeine 缓存**: 添加 `CaffeineCacheProvider` 业界最佳缓存实现
- **AsyncLogger 日志服务**: 添加 `AsyncLogger` 异步日志框架
- **HTTP 客户端**: 添加 `HttpClientImpl` OkHttp 4.12.0 HTTP 客户端
- **Cron 调度器**: 添加 `CronSchedulerImpl` cron4j 2.2.5 定时任务支持
- **审计日志**: 添加 `AuditLog` 接口和 `AuditLogImpl` 实现
- **数据导出**: 添加 `DataExporter` 接口和 `DataExporterImpl` 实现
- **配置迁移**: 添加 `ConfigMigrator` 接口和 `ConfigMigratorImpl` 实现
- **文本显示服务**: 添加 `TextDisplayService` 和 `TextDisplayServiceImpl`
- **BossBar 服务**: 添加 `AdventureBossBarService` Adventure API 实现
- **游戏日志**: 添加 `GameLogger` 接口
- **速率限制器**: 添加 `HttpClient.RateLimiter` 接口和 `RateLimiterImpl` 实现

### 🔄 Changed
- **调度器升级**: 异步任务全面迁移到 Paper 1.21+ `AsyncScheduler`
- **RPGCore.java 重构**: 添加完整服务初始化和 getter 方法
- **ExceptionHandlerImpl**: 迁移到 MiniMessage 颜色服务
- **RpgSkillPointEvent**: 更新文档注释使用 MiniMessage
- **依赖版本**:
  - `HikariCP`: 5.0.1 → 5.1.0
  - `OkHttp`: 4.11.0 → 4.12.0
  - `SLF4J`: 2.0.7 → 2.0.9
  - `Gson`: 2.10.1 (不变)
  - `Cron4J`: 2.2.2 → 2.2.5

### 🐛 Fixed
- **UnifiedSchedulerImpl**: 异步调度器迁移到 `Bukkit.getAsyncScheduler()`
- **UnifiedDataManager**: 异步保存使用 `AsyncScheduler.runNow()`
- **CronSchedulerImpl**: 移除不存在的 `CronExpression` 类调用
- **HttpClientImpl**: `shutdown()` 方法添加异常处理
- **GuangDianArmorStats**: 添加 `--add-modules jdk.incubator.vector` 编译参数

### ⚠️ Deprecated
- `Bukkit.getScheduler()` 同步调度器 (Paper 1.21.6 无替代方案，必须使用)
- `LuckPermsProvider.get()` (已有 null 检查)
- `PlaceholderAPI.setPlaceholders()` (已有 null 检查)

### 🔒 Security
- 所有外部服务调用添加 null 检查
- 资源关闭使用 try-finally 模式

### 📚 Documentation
- 添加 `.trae/docs/reference/CHATCOLOR_MIGRATION_GUIDE.md`
- 更新 `.trae/rules/FORBIDDEN_PATTERNS.md`
- 更新 `.trae/rules/CODE_TEMPLATES.md`
- 添加 `.trae/docs/FIXES/` 修复记录目录

---

## [1.0.0] - 2026-04-10

### 🆕 Added
- 初始化项目架构
- 添加 RPGCore 核心系统
- 集成 24 个 GuangDian* 插件
- 添加 MCP (Model Context Protocol) 服务器管理功能
- 添加 GuangDianMCP 插件用于远程服务器管理

### 🔄 Changed
- 迁移 Bukkit 调度器到 SyncScheduler
- 优化玩家数据管理
- 重构占位符处理流程

### 🐛 Fixed

#### GuangDianBoard 侧边栏
- **修复占位符显示问题**: 配置文件 BOM 导致 YAML 解析失败
- **修复 PlaceholderAPI 检测问题**: RPGCore 启动顺序导致 PlaceholderAPI 未被检测到
- **修复占位符处理顺序**: 调整为先本地替换再 PlaceholderAPI 解析
- **修复世界别名显示**: 确保世界名称显示为中文
- **修复称号显示**: 使用 ExternalServiceIntegration 直接获取 LuckPerms 前缀

#### GuangDianArmorStats 战斗系统
- **修复副手 RPG 装备不生效问题**: `isVanillaWeapon` 方法现在同时检查主手和副手
- **修复副手属性解析**: 添加副手装备属性缓存和解析
- **修复主手切换监听**: 添加 PlayerSwapHandItemsEvent 监听

#### RPGCore 核心
- **修复 PlaceholderAPI 延迟检测**: 添加 `refreshPlaceholderAPI()` 方法支持运行时重新检测
- **修复 ExternalServiceIntegration**: 确保 PlaceholderAPI 在加载后能被正确检测

#### GuangDianMCP MCP服务器
- **修复 SSE 推送空指针**: EventPusher 中 sseHandler 可能为 null 的问题

---

## [0.1.0] - 2026-04-10

### 🆕 Added
- 项目初始化
- Git 版本控制配置
- 基础架构搭建

---

## 版本说明

### 版本号格式
```
[主版本号].[次版本号].[修订号]
```

### 变更类型
- 🆕 `Added` - 新增功能
- 🔄 `Changed` - 变更
- ⚠️ `Deprecated` - 废弃
- 🗑️ `Removed` - 移除
- 🐛 `Fixed` - 修复
- 🔒 `Security` - 安全

---

## 历史版本归档

详细版本说明见 `docs/CHANGELOG/` 目录

---

*最后更新: 2026-04-14*
