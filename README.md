# Astraea RPG 阿斯特瑞亚

> Minecraft Paper 1.21.6 RPG服务器插件体系
>
> 基于 RPGCore 架构的高性能插件集合

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.6-blue.svg)](https://www.minecraft.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21.6-orange.svg)](https://papermc.io/)
[![JDK](https://img.shields.io/badge/JDK-21-red.svg)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Gradle-9.4.0-green.svg)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 🎮 项目简介

**Astraea RPG** (阿斯特瑞亚) 是一个基于 Minecraft Paper 1.21.6 的高性能 RPG 服务器插件体系。项目采用统一的 RPGCore 架构，提供完整的玩家数据管理、装备系统、经济系统、副本系统等功能。

### 核心特性

- 🏗️ **统一架构** - 基于 RPGCore 的标准化插件开发框架
- ⚡ **高性能** - 优化的数据缓存和异步处理机制
- 🔧 **易扩展** - 服务注册机制支持插件间通信
- 🎨 **现代化** - 使用 TextDisplay、Adventure API 等 1.21+ 特性
- 📦 **完整生态** - 24个 GuangDian* 插件覆盖各类功能

---

## 🏛️ 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                      Astraea RPG                            │
├─────────────────────────────────────────────────────────────┤
│  RPGCore (核心)                                             │
│  ├── PlayerLifecycleManager (玩家生命周期管理)              │
│  ├── ServiceRegistry (服务注册中心)                         │
│  ├── SyncScheduler (同步调度器)                             │
│  ├── ExternalServiceIntegration (外部服务集成)              │
│  └── UnifiedDataManager (统一数据管理)                      │
├─────────────────────────────────────────────────────────────┤
│  GuangDian* Plugins (24个插件)                              │
│  ├── GuangDianArmorStats (装备属性)                        │
│  ├── GuangDianPoints (点数系统)                            │
│  ├── GuangDianName (命名系统)                              │
│  ├── GuangDianMarket (市场系统)                             │
│  ├── GuangDianQuest (任务系统)                             │
│  ├── GuangDianBoard (面板系统)                             │
│  ├── GuangDianTab (Tab列表)                                │
│  ├── GuangDianNPC (NPC系统)                                │
│  ├── GuangDianMobHealth (怪物血量)                         │
│  ├── GuangDianForge (锻造系统)                              │
│  ├── GuangDianCaveFu (洞穴系统)                            │
│  ├── GuangDianGuild (公会系统)                             │
│  ├── GuangDianMarriage (婚姻系统)                            │
│  ├── GuangDianHolo (全息图)                                │
│  ├── GuangDianTrade (交易系统)                             │
│  └── ... (更多)                                             │
├─────────────────────────────────────────────────────────────┤
│  External Integrations                                      │
│  ├── MythicMobs (怪物系统)                                  │
│  ├── LuckPerms (权限管理)                                   │
│  ├── PlaceholderAPI (占位符)                                │
│  └── Vault (经济)                                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 环境要求

| 项目 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | 推荐 Eclipse Temurin |
| Gradle | 9.4.0 | 构建工具 |
| Minecraft | 1.21.6 | Paper 服务端 |
| OS | Windows/Linux | PowerShell 或 Bash |

### 构建项目

```powershell
# 1. 进入项目目录
cd e:\原创RPG服务端

# 2. 设置环境变量
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"

# 3. 构建所有插件 (唯一标准命令)
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test

# 4. 清理并构建
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" clean build --no-configuration-cache -x test
```

### 运行服务器

```powershell
cd server
java -Xms4G -Xmx8G -jar paper-1.21.6.jar nogui
```

### 部署插件

构建完成后，JAR 文件位于：
```
plugins/{插件名}/build/libs/{插件名}-1.0.0.jar
```

复制到 `server/plugins/` 目录即可。

---

## 📁 项目结构

```
astraea-rpg-server/
├── .trae/                          # AI助手配置
│   ├── rules/                      # 开发规则
│   │   ├── kaifa.md              # 任务分类中心
│   │   ├── FORBIDDEN_PATTERNS.md # 禁止模式清单
│   │   └── CODE_TEMPLATES.md     # 代码模板库
│   ├── skills/                    # 技能定义
│   │   ├── minecraft-rpg-architect/
│   │   ├── code-reviewer/
│   │   ├── performance-tuner/
│   │   └── ui-designer/
│   ├── knowledge/                 # 知识库
│   │   ├── INDEX.md              # 知识索引
│   │   ├── memory/              # 记忆中心
│   │   └── logs/               # 日志中心
│   └── docs/                    # 文档中心
│       ├── workflow/            # 工作流程
│       ├── reference/           # 参考文档
│       └── FIXES/              # 修复记录
├── plugins/                       # 插件源码 (24个)
│   ├── RPGCore/                  # 核心插件
│   ├── GuangDianArmorStats/     # 装备属性
│   ├── GuangDianPoints/         # 点数系统
│   └── ...                        # 其他插件
├── server/                        # 服务器文件
│   └── plugins/                  # 编译后的插件
├── tools/                         # 工具链
│   └── jdk-21.0.10+7/          # JDK 21
├── README.md                      # 本文件
└── LICENSE                        # MIT 许可证
```

---

## 🛠️ 开发规范

### 插件开发必须遵守

1. **继承 AbstractRPGPlugin** - 禁止直接继承 JavaPlugin
2. **使用 SyncScheduler** - 禁止 Bukkit.getScheduler()
3. **服务注册** - 通过 ServiceRegistry 注册服务
4. **外部服务** - 通过 ExternalServiceIntegration 访问

### 禁止模式

| 禁止项 | 正确替代 |
|--------|----------|
| `BukkitRunnable` | `SyncScheduler` |
| `Bukkit.getScheduler()` | `RPGCore.getScheduler()` |
| `Bukkit.getPlugin("RPGCore")` | `RPGCore.getInstance()` |
| `ChatColor` | `Adventure API (Component)` |
| `ArmorStand` (显示) | `TextDisplay` |

详细规范见 [.trae/rules/kaifa.md](.trae/rules/kaifa.md)

### 代码审查清单

- [ ] 新插件主类继承 `AbstractRPGPlugin`
- [ ] 使用 `SyncScheduler` 而非 `Bukkit.getScheduler()`
- [ ] 通过 `ExternalServiceIntegration` 访问外部服务
- [ ] 使用 `Adventure API` 而非 `ChatColor`
- [ ] 插件卸载时取消所有调度任务

---

## 📚 文档导航

### 核心规则

| 文档 | 用途 |
|------|------|
| [.trae/rules/kaifa.md](.trae/rules/kaifa.md) | 任务分类中心 |
| [.trae/rules/FORBIDDEN_PATTERNS.md](.trae/rules/FORBIDDEN_PATTERNS.md) | 禁止模式 |
| [.trae/rules/CODE_TEMPLATES.md](.trae/rules/CODE_TEMPLATES.md) | 代码模板 |

### 工作流程

| 文档 | 用途 |
|------|------|
| [.trae/docs/workflow/WORKFLOW.md](.trae/docs/workflow/WORKFLOW.md) | 完整工作流程 |
| [.trae/docs/reference/BUILD_GUIDE.md](.trae/docs/reference/BUILD_GUIDE.md) | 构建指南 |
| [.trae/docs/reference/VERSION_CONTROL.md](.trae/docs/reference/VERSION_CONTROL.md) | 版本控制 |

### 技能文档

| 技能 | 用途 |
|------|------|
| minecraft-rpg-architect | RPG插件开发 |
| code-reviewer | 代码审查 |
| performance-tuner | 性能优化 |
| ui-designer | UI设计 |

### 知识库

| 类型 | 位置 |
|------|------|
| 知识索引 | [.trae/knowledge/INDEX.md](.trae/knowledge/INDEX.md) |
| 修复记录 | [.trae/docs/FIXES/](.trae/docs/FIXES/) |
| 模式记忆 | [.trae/knowledge/memory/patterns/](.trae/knowledge/memory/patterns/) |

---

## 🔧 任务分类

根据任务类型，使用对应的处理方式：

| 任务类型 | 关键词 | 处理方式 |
|----------|--------|----------|
| **构建** | 构建、build、编译 | 执行构建命令 |
| **开发** | 开发、创建、实现 | 激活 minecraft-rpg-architect |
| **修复** | 修复、解决、Bug | 激活技能 + 记录修复 |
| **审查** | 审查、检查、规范 | 激活 code-reviewer |
| **性能** | 性能、优化、卡顿 | 激活 performance-tuner |
| **UI** | UI、界面、菜单 | 激活 ui-designer |
| **版本** | 版本、发布、标签 | 执行版本控制流程 |

详见 [.trae/rules/kaifa.md](.trae/rules/kaifa.md)

---

## 🎯 插件列表

### 核心插件

| 插件 | 功能 | 状态 |
|------|------|------|
| RPGCore | 核心架构 | ✅ 已完成 |

### GuangDian* 插件

| 插件 | 功能 | 状态 |
|------|------|------|
| GuangDianArmorStats | 装备属性系统 | ✅ 已完成 |
| GuangDianPoints | 点数经济系统 | ✅ 已完成 |
| GuangDianMarket | 市场交易系统 | ✅ 已完成 |
| GuangDianQuest | 任务系统 | ✅ 已完成 |
| GuangDianName | 命名系统 | ✅ 已完成 |
| GuangDianBoard | 面板显示系统 | ✅ 已完成 |
| GuangDianTab | Tab列表系统 | ⏳ 待迁移 |
| GuangDianNPC | NPC交互系统 | ⏳ 待迁移 |
| GuangDianMobHealth | 怪物血量显示 | ⏳ 待迁移 |
| GuangDianForge | 锻造系统 | ⏳ 待迁移 |
| GuangDianCaveFu | 洞穴副本系统 | ⏳ 待迁移 |
| GuangDianGuild | 公会系统 | ⏳ 待迁移 |
| GuangDianMarriage | 婚姻系统 | ⏳ 待迁移 |
| GuangDianHolo | 全息图系统 | ⏳ 待迁移 |
| GuangDianTrade | 交易系统 | ⏳ 待迁移 |
| GuangDianCleaner | 清理系统 | ⏳ 待迁移 |
| GuangDianDropControl | 掉落控制 | ⏳ 待迁移 |
| GuangDianItemTrigger | 物品触发 | ⏳ 待迁移 |
| GuangDianWorld | 世界管理 | ⏳ 待迁移 |
| GuangDianLocation | 位置系统 | ⏳ 待迁移 |
| GuangDianDecompose | 分解系统 | ⏳ 待迁移 |
| GuangDianGift | 礼物系统 | ⏳ 待迁移 |
| GuangDianMenu | GUI菜单系统 | ⏳ 待迁移 |
| GuangDianChat | 聊天系统 | ⏳ 待迁移 |
| GuangDianMCP | MCP主控系统 | ⏳ 待迁移 |

---

## 🤝 贡献指南

### Git 分支策略

```
main (生产环境)
  ↑
develop (开发集成)
  ↑
feature/* (功能开发)
  ↑
fix/* (Bug修复)
  ↑
release/* (版本发布)
  ↑
hotfix/* (紧急修复)
```

### 提交规范

```
[类型]: [简要描述]

[详细描述]

[关联Issue]
```

**类型标识:**
- `feat` - 新功能
- `fix` - Bug修复
- `docs` - 文档
- `style` - 格式
- `refactor` - 重构
- `perf` - 性能
- `test` - 测试
- `chore` - 构建

### 示例

```
feat: 添加玩家数据缓存系统

- 实现 TTLCacheManager
- 添加缓存统计功能
- 集成到 PlayerLifecycleManager

Closes #123
```

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源许可证。

---

## 🙏 致谢

- [PaperMC](https://papermc.io/) - 高性能 Minecraft 服务端
- [MythicMobs](https://mythiccraft.io/) - 强大的怪物系统
- [LuckPerms](https://luckperms.net/) - 权限管理
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) - 占位符系统
- [Vault](https://github.com/MilkBowl/Vault) - 经济 API
- [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) - 协议库

---

## 📊 项目统计

| 指标 | 数值 |
|------|------|
| 插件总数 | 26个 |
| 核心插件 | 1个 (RPGCore) |
| 功能插件 | 25个 (GuangDian*) |
| Java 版本 | 21 |
| Minecraft 版本 | 1.21.6 |

---

<p align="center">
  <strong>Astraea RPG - 星辰女神的祝福</strong>
</p>
