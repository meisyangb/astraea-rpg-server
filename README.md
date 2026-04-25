# 🌟 Astraea RPG 阿斯特瑞亚

<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.6-blue.svg?style=for-the-badge)](https://www.minecraft.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21.6-orange.svg?style=for-the-badge)](https://papermc.io/)
[![JDK](https://img.shields.io/badge/JDK-21-red.svg?style=for-the-badge)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Gradle-9.4.0-green.svg?style=for-the-badge)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)

**一个基于 Minecraft Paper 1.21.6 的高性能 RPG 服务器插件体系**

[🚀 快速开始](#-快速开始) · [📖 文档](#-文档导航) · [🔌 插件列表](#-插件列表) · [🤝 贡献](#-贡献指南) · [❓ 常见问题](#-常见问题faq)

</div>

---

## 📑 目录

- [🎮 项目简介](#-项目简介)
- [✨ 核心特性](#-核心特性)
- [🏛️ 架构概览](#️-架构概览)
- [🚀 快速开始](#-快速开始)
  - [环境要求](#环境要求)
  - [下载安装](#下载安装)
  - [构建项目](#构建项目)
  - [运行服务器](#运行服务器)
  - [部署插件](#部署插件)
- [📁 项目结构](#-项目结构)
- [🔌 插件列表](#-插件列表)
- [⚙️ 配置指南](#️-配置指南)
- [🛠️ 开发规范](#️-开发规范)
- [📚 文档导航](#-文档导航)
- [🤝 贡献指南](#-贡献指南)
- [❓ 常见问题](#-常见问题faq)
- [📄 许可证](#-许可证)
- [🙏 致谢](#-致谢)
- [📞 联系方式](#-联系方式)

---

## 🎮 项目简介

**Astraea RPG** (阿斯特瑞亚) 是一个专为 Minecraft Paper 1.21.6 设计的高性能 RPG 服务器插件体系。项目采用统一的 **RPGCore** 架构，提供完整的玩家数据管理、装备系统、经济系统、副本系统等功能。

### 🎯 设计理念

| 理念 | 说明 |
|------|------|
| 🏗️ **统一架构** | 所有插件基于 RPGCore 核心框架开发，确保一致性和可维护性 |
| ⚡ **高性能** | 采用异步处理、智能缓存、批量操作等优化技术 |
| 🔧 **易扩展** | 服务注册机制支持插件间通信，方便功能扩展 |
| 🎨 **现代化** | 使用 TextDisplay、Adventure API 等 1.21+ 新特性 |
| 📦 **完整生态** | 26个插件覆盖 RPG 服务器各类功能需求 |

### 🌟 适用场景

- 🏰 **RPG 服务器** - 完整的角色扮演体验
- ⚔️ **冒险服务器** - 丰富的战斗和任务系统
- 🏘️ **生存服务器** - 增强的生存体验
- 🎪 **综合服务器** - 多玩法混合服务器

---

## ✨ 核心特性

### 🔥 性能优化

```
┌─────────────────────────────────────────────────────────────┐
│                    性能优化技术栈                            │
├─────────────────────────────────────────────────────────────┤
│  ✅ 异步数据处理 - 数据库操作不阻塞主线程                    │
│  ✅ 智能缓存系统 - TTL 缓存 + LRU 淘汰策略                   │
│  ✅ 批量操作优化 - 减少 I/O 次数                             │
│  ✅ 事件驱动架构 - 解耦模块，提高响应速度                    │
│  ✅ 延迟加载机制 - 按需加载，节省内存                        │
│  ✅ 对象池技术 - 减少 GC 压力                                │
└─────────────────────────────────────────────────────────────┘
```

### 🎮 功能模块

| 模块 | 功能 | 插件 |
|------|------|------|
| 💪 **战斗系统** | RPG属性、伤害计算、技能系统 | GuangDianArmorStats |
| 💰 **经济系统** | 点数、市场、交易 | GuangDianPoints, GuangDianMarket |
| 📜 **任务系统** | 主线、支线、日常任务 | GuangDianQuest |
| 🏠 **公会系统** | 公会管理、权限、升级 | GuangDianGuild |
| 💍 **社交系统** | 婚姻、好友、聊天 | GuangDianMarriage, GuangDianChat |
| 🗡️ **锻造系统** | 装备强化、宝石镶嵌 | GuangDianForge |
| 🗺️ **副本系统** | 洞穴副本、世界管理 | GuangDianCaveFu, GuangDianWorld |
| 🎨 **显示系统** | 记分板、Tab、全息图 | GuangDianBoard, GuangDianTab, GuangDianHolo |
| 🤖 **管理系统** | MCP 远程管理 | GuangDianMCP |

---

## 🏛️ 架构概览

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Astraea RPG 架构                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    RPGCore (核心层)                          │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │   │
│  │  │  Service    │ │   Sync      │ │  External   │           │   │
│  │  │  Registry   │ │  Scheduler  │ │  Services   │           │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘           │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │   │
│  │  │  Player     │ │   Cache     │ │   Event     │           │   │
│  │  │  Lifecycle  │ │   Manager   │ │    Bus      │           │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘           │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                  GuangDian* Plugins (插件层)                 │   │
│  │                                                             │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │   │
│  │  │ Armor   │ │ Points  │ │  Quest  │ │  Guild  │          │   │
│  │  │ Stats   │ │         │ │         │ │         │          │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘          │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │   │
│  │  │  Board  │ │  Forge  │ │ Market  │ │   NPC   │          │   │
│  │  │         │ │         │ │         │ │         │          │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘          │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │   │
│  │  │  MCP    │ │  Holo   │ │  World  │ │  Trade  │  ...     │   │
│  │  │         │ │         │ │         │ │         │          │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                 External Integrations (外部集成)              │   │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐   │   │
│  │  │ MythicMobs│ │ LuckPerms │ │  PAPI     │ │   Vault   │   │   │
│  │  └───────────┘ └───────────┘ └───────────┘ └───────────┘   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 核心组件说明

| 组件 | 职责 | 说明 |
|------|------|------|
| **ServiceRegistry** | 服务注册中心 | 管理所有插件服务的注册和获取 |
| **SyncScheduler** | 同步调度器 | 统一的任务调度，替代 Bukkit 调度器 |
| **ExternalServiceIntegration** | 外部服务集成 | 统一访问 PlaceholderAPI、LuckPerms 等 |
| **PlayerLifecycleManager** | 玩家生命周期 | 管理玩家数据加载、保存、缓存 |
| **TTLCacheManager** | 缓存管理 | 高性能 TTL 缓存，自动过期清理 |
| **EventBus** | 事件总线 | 插件间事件通信 |

---

## 🚀 快速开始

### 环境要求

#### 必需环境

| 软件 | 版本要求 | 下载链接 | 说明 |
|------|----------|----------|------|
| **JDK** | 21+ | [Eclipse Temurin](https://adoptium.net/) | 推荐 Eclipse Temurin 或 Oracle JDK |
| **Gradle** | 9.4.0 | [Gradle 官网](https://gradle.org/) | 构建工具 |
| **Minecraft** | 1.21.6 | [PaperMC](https://papermc.io/) | Paper 服务端 |

#### 可选依赖

| 软件 | 用途 | 说明 |
|------|------|------|
| **PlaceholderAPI** | 占位符支持 | 强烈推荐 |
| **LuckPerms** | 权限管理 | 强烈推荐 |
| **Vault** | 经济 API | 推荐 |
| **MythicMobs** | 怪物系统 | 如需自定义怪物 |
| **ProtocolLib** | 协议库 | 部分功能需要 |

### 下载安装

#### 方式一：下载 Release 版本（推荐）

1. 访问 [Releases 页面](https://github.com/meisyangb/astraea-rpg-server/releases)
2. 下载最新版本的 JAR 文件
3. 将 JAR 文件放入 `server/plugins/` 目录

#### 方式二：从源码构建

```powershell
# 1. 克隆仓库
git clone https://github.com/meisyangb/astraea-rpg-server.git
cd astraea-rpg-server

# 2. 设置 JAVA_HOME 环境变量
# Windows:
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"

# Linux/Mac:
export JAVA_HOME=/path/to/jdk-21

# 3. 构建所有插件
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test
```

### 构建项目

#### 标准构建命令

```powershell
# 进入项目目录
cd e:\原创RPG服务端

# 设置环境变量
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"

# 构建所有插件（唯一标准命令）
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test
```

#### 清理并构建

```powershell
# 清理旧的构建文件并重新构建
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" clean build --no-configuration-cache -x test
```

#### 构建单个插件

```powershell
# 只构建 RPGCore
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" :RPGCore:build --no-configuration-cache -x test

# 只构建 GuangDianArmorStats
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" :GuangDianArmorStats:build --no-configuration-cache -x test
```

### 运行服务器

#### 启动命令

```powershell
# 进入服务器目录
cd server

# 启动服务器（推荐配置）
java -Xms4G -Xmx8G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -jar paper-1.21.6.jar nogui
```

#### 推荐启动参数

```powershell
# 高性能配置（16GB+ 内存）
java -Xms8G -Xmx16G `
  -XX:+UseG1GC `
  -XX:+ParallelRefProcEnabled `
  -XX:MaxGCPauseMillis=200 `
  -XX:+UnlockExperimentalVMOptions `
  -XX:+DisableExplicitGC `
  -XX:+AlwaysPreTouch `
  -XX:G1NewSizePercent=30 `
  -XX:G1MaxNewSizePercent=40 `
  -XX:G1HeapRegionSize=8M `
  -XX:G1ReservePercent=20 `
  -XX:G1HeapWastePercent=5 `
  -XX:G1MixedGCCountTarget=4 `
  -XX:InitiatingHeapOccupancyPercent=15 `
  -XX:G1MixedGCLiveThresholdPercent=90 `
  -XX:G1RSetUpdatingPauseTimePercent=5 `
  -XX:SurvivorRatio=32 `
  -XX:+PerfDisableSharedMem `
  -XX:MaxTenuringThreshold=1 `
  -Dusing.aikars.flags=https://mcflags.emc.gs `
  -Daikars.new.flags=true `
  -jar paper-1.21.6.jar nogui
```

### 部署插件

#### 构建产物位置

```
plugins/
├── RPGCore/build/libs/RPGCore-1.0.0.jar
├── GuangDianArmorStats/build/libs/GuangDianArmorStats-1.0.0.jar
├── GuangDianBoard/build/libs/GuangDianBoard-1.0.0.jar
└── ... (其他插件)
```

#### 部署步骤

```powershell
# 1. 复制所有构建好的 JAR 到服务器插件目录
Copy-Item "plugins/*/build/libs/*.jar" -Destination "server/plugins/"

# 2. 重启服务器
# 或使用 PlugMan 等插件热加载
```

#### 插件加载顺序

服务器会自动按依赖顺序加载插件，但建议确保：

1. **RPGCore** 最先加载（核心框架）
2. **外部依赖** 已安装（PlaceholderAPI、LuckPerms、Vault）
3. **GuangDian*** 插件按需加载

---

## 📁 项目结构

```
astraea-rpg-server/
│
├── .trae/                          # 🤖 AI 助手配置目录
│   ├── rules/                      # 📋 开发规则
│   │   ├── kaifa.md                # 任务分类中心
│   │   ├── FORBIDDEN_PATTERNS.md   # 禁止模式清单
│   │   └── CODE_TEMPLATES.md       # 代码模板库
│   │
│   ├── skills/                     # 🎯 技能定义
│   │   ├── minecraft-rpg-architect/ # RPG 插件开发技能
│   │   ├── code-reviewer/          # 代码审查技能
│   │   ├── performance-tuner/      # 性能优化技能
│   │   └── ui-designer/            # UI 设计技能
│   │
│   ├── knowledge/                  # 📚 知识库
│   │   ├── INDEX.md                # 知识索引
│   │   ├── memory/                 # 记忆中心
│   │   └── logs/                   # 日志中心
│   │
│   └── docs/                       # 📖 文档中心
│       ├── workflow/               # 工作流程文档
│       ├── reference/              # 参考文档
│       ├── FIXES/                  # 修复记录
│       └── CHANGELOG.md            # 更新日志
│
├── plugins/                        # 🔌 插件源码目录
│   │
│   ├── RPGCore/                    # ⭐ 核心框架插件
│   │   ├── src/main/java/          # Java 源码
│   │   ├── src/main/resources/     # 资源文件
│   │   └── build.gradle            # 构建配置
│   │
│   ├── GuangDianArmorStats/        # 💪 装备属性系统
│   ├── GuangDianPoints/            # 💰 点数经济系统
│   ├── GuangDianQuest/             # 📜 任务系统
│   ├── GuangDianGuild/             # 🏠 公会系统
│   ├── GuangDianMarket/            # 🛒 市场系统
│   ├── GuangDianForge/             # ⚒️ 锻造系统
│   ├── GuangDianBoard/             # 📊 记分板系统
│   ├── GuangDianTab/               # 📋 Tab 列表系统
│   ├── GuangDianChat/              # 💬 聊天系统
│   ├── GuangDianMenu/              # 📱 GUI 菜单系统
│   ├── GuangDianHolo/              # ✨ 全息图系统
│   ├── GuangDianNPC/               # 👥 NPC 系统
│   ├── GuangDianWorld/             # 🌍 世界管理系统
│   ├── GuangDianCaveFu/            # 🗻 洞穴副本系统
│   ├── GuangDianMobHealth/         # ❤️ 怪物血量显示
│   ├── GuangDianName/              # 🏷️ 命名系统
│   ├── GuangDianLocation/          # 📍 位置系统
│   ├── GuangDianTrade/             # 🤝 交易系统
│   ├── GuangDianMarriage/          # 💍 婚姻系统
│   ├── GuangDianMCP/               # 🤖 MCP 远程管理
│   ├── GuangDianCleaner/           # 🧹 清理系统
│   ├── GuangDianDropControl/       # 📦 掉落控制
│   ├── GuangDianItemTrigger/       # 🎯 物品触发
│   ├── GuangDianDecompose/         # 💎 分解系统
│   └── GuangDianGift/              # 🎁 礼物系统
│
├── server/                         # 🎮 服务器目录（不提交）
│   ├── plugins/                    # 已部署的插件
│   ├── world/                      # 世界文件
│   └── paper-1.21.6.jar            # 服务端核心
│
# ❌ tools/                          # 🔧 工具链（已排除，不上传）
│
├── .gitignore                      # Git 忽略配置
├── README.md                       # 本文件
├── CONTRIBUTING.md                 # 贡献指南
├── LICENSE                         # MIT 许可证
└── build.gradle                    # 根构建配置
```

---

## 🔌 插件列表

### 核心插件

| 插件 | 功能描述 | 状态 | 依赖 |
|------|----------|------|------|
| **RPGCore** | 核心框架，提供统一的服务注册、调度器、缓存管理等基础设施 | ✅ 已完成 | 无 |

### 战斗与属性

| 插件 | 功能描述 | 状态 | 依赖 |
|------|----------|------|------|
| **GuangDianArmorStats** | RPG 装备属性系统，支持自定义属性、伤害计算、技能系统 | ✅ 已完成 | RPGCore, MythicMobs(可选) |
| **GuangDianMobHealth** | 怪物血量显示，支持 BossBar 和全息图显示 | ✅ 已完成 | RPGCore, MythicMobs(可选) |

### 经济与交易

| 插件 | 功能描述 | 状态 | 依赖 |
|------|----------|------|------|
| **GuangDianPoints** | 点数经济系统，支持多种货币类型、交易记录 | ✅ 已完成 | RPGCore, Vault(可选) |
| **GuangDianMarket** | 玩家市场系统，支持拍卖、一口价 | ✅ 已完成 | RPGCore, GuangDianPoints |
| **GuangDianTrade** | 玩家交易系统，安全可靠的交易机制 | ✅ 已完成 | RPGCore |

### 任务与成就

| 插件 | 功能描述 | 状态 | 依赖 |
|------|----------|------|------|
| **GuangDianQuest** | 任务系统，支持主线、支线、日常任务 | ✅ 已完成 | RPGCore |

### 社交与公会

| 插件 | 功能描述 | 状态 | 依赖 |
|------|----------|------|------|
| **GuangDianGuild** | 公会系统，支持公会管理、权限、升级 | ✅ 已完成 | RPGCore |
| **GuangDianMarriage** | 婚姻系统，支持结婚、离婚、夫妻特权 | ✅ 已完成 | RPGCore |
| **GuangDianChat** | 聊天系统，支持频道、私聊、过滤 | ✅ 已完成 | RPGCore, PlaceholderAPI |

### 装备与锻造

| 插件 | 功能描述 | 状态 | 依赖 |
|------|----------|------|------|
| **GuangDianForge** | 锻造系统，支持装备强化、宝石镶嵌 | ✅ 已完成 | RPGCore, MythicMobs(可选) |
| **GuangDianDecompose** | 分解系统，将装备分解为材料 | ✅ 已完成 | RPGCore |

### 显示与界面

| 插件 | 功能描述 | 状态 | 依赖 |
|------|----------|------|------|
| **GuangDianBoard** | 记分板系统，支持动态更新、占位符 | ✅ 已完成 | RPGCore, PlaceholderAPI |
| **GuangDianTab** | Tab 列表系统，自定义玩家显示 | ✅ 已完成 | RPGCore, PlaceholderAPI |
| **GuangDianHolo** | 全息图系统，基于 TextDisplay | ✅ 已完成 | RPGCore |
| **GuangDianName** | 玩家名称显示，支持称号、血量显示 | ✅ 已完成 | RPGCore |
| **GuangDianMenu** | GUI 菜单系统，支持自定义菜单 | ✅ 已完成 | RPGCore |

### 世界与副本

| 插件 | 功能描述 | 状态 | 依赖 |
|------|----------|------|------|
| **GuangDianWorld** | 世界管理系统，支持世界创建、传送 | ✅ 已完成 | RPGCore |
| **GuangDianCaveFu** | 洞穴副本系统，支持副本创建、进度 | ✅ 已完成 | RPGCore |
| **GuangDianLocation** | 位置系统，支持位置保存、传送 | ✅ 已完成 | RPGCore |

### 功能工具

| 插件 | 功能描述 | 状态 | 依赖 |
|------|----------|------|------|
| **GuangDianNPC** | NPC 系统，支持自定义 NPC、交互 | ✅ 已完成 | RPGCore |
| **GuangDianCleaner** | 清理系统，自动清理掉落物、实体 | ✅ 已完成 | RPGCore |
| **GuangDianDropControl** | 掉落控制，自定义掉落规则 | ✅ 已完成 | RPGCore |
| **GuangDianItemTrigger** | 物品触发，物品使用触发事件 | ✅ 已完成 | RPGCore |
| **GuangDianGift** | 礼物系统，支持礼包、奖励 | ✅ 已完成 | RPGCore |
| **GuangDianMCP** | MCP 远程管理，支持 AI 远程控制服务器 | ✅ 已完成 | RPGCore |

---

## ⚙️ 配置指南

### RPGCore 核心配置

```yaml
# RPGCore/config.yml

# 数据库配置
database:
  type: sqlite  # sqlite 或 mysql
  mysql:
    host: localhost
    port: 3306
    database: astraea_rpg
    username: root
    password: password

# 缓存配置
cache:
  ttl: 300  # 缓存过期时间（秒）
  maxSize: 10000  # 最大缓存数量

# 调试模式
debug: false
```

### GuangDianBoard 记分板配置

```yaml
# GuangDianBoard/config.yml

# 更新间隔（tick，20tick = 1秒）
update-interval: 20

# 世界别名
world-aliases:
  world: "主世界"
  world_nether: "下界"
  world_the_end: "末地"

# 记分板内容
board:
  title: "&6&l Astraea RPG"
  lines:
    - ""
    - "&f玩家: &e%player%"
    - "&f世界: &e%player_world%"
    - "&f称号: %luckperms_prefix%"
    - ""
    - "&f金币: &e%guangdian_points_balance%"
    - ""
```

### GuangDianArmorStats 属性配置

```yaml
# GuangDianArmorStats/attributes.yml

# 基础属性
attributes:
  attack:
    symbol: "⚔"
    name: "攻击力"
    base: 0
  defense:
    symbol: "🛡"
    name: "防御力"
    base: 0
  health:
    symbol: "❤"
    name: "生命值"
    base: 20
  critical:
    symbol: "💥"
    name: "暴击率"
    base: 0
    max: 100

# 伤害公式
damage-formula: |
  base_damage = attacker_attack * (1 - defender_defense / (defender_defense + 100))
  final_damage = base_damage * (1 + critical_bonus)
```

---

## 🛠️ 开发规范

### 必须遵守的规则

#### 1. 插件主类

```java
// ❌ 禁止
public class MyPlugin extends JavaPlugin { }

// ✅ 正确
public class MyPlugin extends AbstractRPGPlugin {
    @Override
    protected void onPluginEnable() { }
    
    @Override
    protected void onPluginDisable() { }
    
    @Override
    protected String getPluginName() {
        return "MyPlugin";
    }
}
```

#### 2. 调度器使用

```java
// ❌ 禁止
new BukkitRunnable() { }.runTaskTimer(plugin, delay, period);
Bukkit.getScheduler().runTaskLater(plugin, task, delay);

// ✅ 正确
RPGCore rpgCore = RPGCore.getInstance();
SyncScheduler scheduler = rpgCore.getScheduler();

scheduler.runSyncLater(() -> { }, 50L);
scheduler.runSyncRepeating(() -> { }, 0L, 20L);
scheduler.runAsync(() -> { });
```

#### 3. 外部服务访问

```java
// ❌ 禁止
LuckPermsProvider.get().getUserManager().getUser(uuid);
PlaceholderAPI.setPlaceholders(player, text);

// ✅ 正确
RPGCore rpgCore = RPGCore.getInstance();
ExternalServiceIntegration external = rpgCore.getExternalServices();

String prefix = external.getPlayerPrefix(player);
String parsed = external.parsePlaceholders(player, text);
```

#### 4. 消息发送

```java
// ❌ 禁止
player.sendMessage(ChatColor.RED + "错误消息");
player.sendMessage("§c错误消息");

// ✅ 正确
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

player.sendMessage(Component.text("错误消息").color(NamedTextColor.RED));
```

### 禁止模式清单

| 禁止项 | 正确替代 | 原因 |
|--------|----------|------|
| `BukkitRunnable` | `SyncScheduler` | 统一调度管理 |
| `Bukkit.getScheduler()` | `RPGCore.getScheduler()` | 统一调度管理 |
| `Bukkit.getPlugin("RPGCore")` | `RPGCore.getInstance()` | 安全获取实例 |
| `ChatColor` | `Adventure API` | 现代化 API |
| `ArmorStand` (显示) | `TextDisplay` | 1.21+ 新特性 |
| `LuckPermsProvider.get()` | `ExternalServiceIntegration` | 统一外部服务 |

详细规范见 [.trae/rules/FORBIDDEN_PATTERNS.md](.trae/rules/FORBIDDEN_PATTERNS.md)

### 代码模板

详细代码模板见 [.trae/rules/CODE_TEMPLATES.md](.trae/rules/CODE_TEMPLATES.md)

---

## 📚 文档导航

### 核心规则

| 文档 | 用途 | 链接 |
|------|------|------|
| 任务分类中心 | 任务类型识别和处理 | [.trae/rules/kaifa.md](.trae/rules/kaifa.md) |
| 禁止模式清单 | 开发禁止事项 | [.trae/rules/FORBIDDEN_PATTERNS.md](.trae/rules/FORBIDDEN_PATTERNS.md) |
| 代码模板库 | 开发模板代码 | [.trae/rules/CODE_TEMPLATES.md](.trae/rules/CODE_TEMPLATES.md) |

### 工作流程

| 文档 | 用途 | 链接 |
|------|------|------|
| 完整工作流程 | 开发工作流程 | [.trae/docs/workflow/WORKFLOW.md](.trae/docs/workflow/WORKFLOW.md) |
| 任务执行流程 | 任务处理流程 | [.trae/docs/workflow/TASK_WORKFLOW.md](.trae/docs/workflow/TASK_WORKFLOW.md) |
| 构建指南 | 构建项目方法 | [.trae/docs/reference/BUILD_GUIDE.md](.trae/docs/reference/BUILD_GUIDE.md) |
| 版本控制规范 | Git 版本管理 | [.trae/docs/reference/VERSION_CONTROL.md](.trae/docs/reference/VERSION_CONTROL.md) |
| 发布检查清单 | 版本发布流程 | [.trae/docs/reference/RELEASE_CHECKLIST.md](.trae/docs/reference/RELEASE_CHECKLIST.md) |

### 技能文档

| 技能 | 用途 | 链接 |
|------|------|------|
| minecraft-rpg-architect | RPG 插件开发 | [.trae/skills/minecraft-rpg-architect/SKILL.md](.trae/skills/minecraft-rpg-architect/SKILL.md) |
| code-reviewer | 代码审查 | [.trae/skills/code-reviewer/SKILL.md](.trae/skills/code-reviewer/SKILL.md) |
| performance-tuner | 性能优化 | [.trae/skills/performance-tuner/SKILL.md](.trae/skills/performance-tuner/SKILL.md) |
| ui-designer | UI 设计 | [.trae/skills/ui-designer/SKILL.md](.trae/skills/ui-designer/SKILL.md) |

### 知识库

| 类型 | 用途 | 链接 |
|------|------|------|
| 知识索引 | 知识库入口 | [.trae/knowledge/INDEX.md](.trae/knowledge/INDEX.md) |
| 修复记录 | Bug 修复记录 | [.trae/docs/FIXES/](.trae/docs/FIXES/) |
| 模式记忆 | 开发模式记录 | [.trae/knowledge/memory/patterns/](.trae/knowledge/memory/patterns/) |

---

## 🤝 贡献指南

### Git 分支策略

```
main (生产环境)
  ↑
develop (开发集成)
  ↑
├── feature/* (功能开发)
├── fix/* (Bug修复)
├── release/* (版本发布)
└── hotfix/* (紧急修复)
```

### 提交规范

```
[类型]: [简要描述]

[详细描述]

[关联Issue]
```

**类型标识:**

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 添加玩家数据缓存系统` |
| `fix` | Bug修复 | `fix: 修复占位符显示问题` |
| `docs` | 文档更新 | `docs: 更新 README.md` |
| `style` | 代码格式 | `style: 格式化代码` |
| `refactor` | 重构 | `refactor: 重构调度器` |
| `perf` | 性能优化 | `perf: 优化缓存性能` |
| `test` | 测试 | `test: 添加单元测试` |
| `chore` | 构建/工具 | `chore: 更新 Gradle 配置` |

### 提交示例

```
feat: 添加玩家数据缓存系统

- 实现 TTLCacheManager
- 添加缓存统计功能
- 集成到 PlayerLifecycleManager

Closes #123
```

### 代码审查清单

- [ ] 新插件主类继承 `AbstractRPGPlugin`
- [ ] 使用 `SyncScheduler` 而非 `Bukkit.getScheduler()`
- [ ] 通过 `ExternalServiceIntegration` 访问外部服务
- [ ] 使用 `Adventure API` 而非 `ChatColor`
- [ ] 插件卸载时取消所有调度任务
- [ ] 添加适当的日志记录
- [ ] 编写单元测试（如适用）

详细贡献指南见 [CONTRIBUTING.md](CONTRIBUTING.md)

---

## ❓ 常见问题（FAQ）

### 构建相关

<details>
<summary><b>Q: 构建时提示找不到 JDK？</b></summary>

**A:** 确保正确设置 `JAVA_HOME` 环境变量：

```powershell
# Windows
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"

# Linux/Mac
export JAVA_HOME=/path/to/jdk-21

# 验证
java -version  # 应显示 21.x.x
```
</details>

<details>
<summary><b>Q: 构建时提示 Gradle 版本不匹配？</b></summary>

**A:** 使用项目指定的 Gradle 版本 9.4.0：

```powershell
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test
```
</details>

<details>
<summary><b>Q: 构建产物在哪里？</b></summary>

**A:** 每个 JAR 文件位于对应插件的 `build/libs/` 目录：

```
plugins/RPGCore/build/libs/RPGCore-1.0.0.jar
plugins/GuangDianArmorStats/build/libs/GuangDianArmorStats-1.0.0.jar
```
</details>

### 运行相关

<details>
<summary><b>Q: 服务器启动时提示找不到 RPGCore？</b></summary>

**A:** 确保按正确顺序安装插件：

1. 先安装 **RPGCore**
2. 再安装 **GuangDian*** 插件
3. 最后安装外部依赖（PlaceholderAPI、LuckPerms、Vault）
</details>

<details>
<summary><b>Q: 占位符显示为原始文本？</b></summary>

**A:** 检查以下几点：

1. PlaceholderAPI 是否已安装
2. 对应的扩展是否已注册
3. 查看 RPGCore 日志确认 PlaceholderAPI 是否被检测到
</details>

<details>
<summary><b>Q: 权限节点是什么？</b></summary>

**A:** 权限节点格式：

```
guangdian.<插件名>.<权限>
```

示例：
- `guangdian.board.view` - 查看记分板
- `guangdian.quest.accept` - 接受任务
- `guangdian.guild.create` - 创建公会
</details>

### 开发相关

<details>
<summary><b>Q: 如何创建新插件？</b></summary>

**A:** 参考 [.trae/rules/CODE_TEMPLATES.md](.trae/rules/CODE_TEMPLATES.md) 中的插件主类模板。

关键步骤：
1. 创建 `build.gradle`
2. 创建主类继承 `AbstractRPGPlugin`
3. 创建服务适配器
4. 注册到 RPGCore
</details>

<details>
<summary><b>Q: 如何注册占位符？</b></summary>

**A:** 参考 [.trae/rules/CODE_TEMPLATES.md](.trae/rules/CODE_TEMPLATES.md) 中的占位符扩展模板。

```java
public class MyPlaceholder extends PlaceholderExpansion {
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.equals("my_value")) {
            return "some value";
        }
        return null;
    }
}
```
</details>

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源许可证。

```
MIT License

Copyright (c) 2026 Astraea RPG Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 🙏 致谢

感谢以下开源项目：

| 项目 | 用途 | 链接 |
|------|------|------|
| PaperMC | 高性能 Minecraft 服务端 | [papermc.io](https://papermc.io/) |
| MythicMobs | 强大的怪物系统 | [mythiccraft.io](https://mythiccraft.io/) |
| LuckPerms | 权限管理 | [luckperms.net](https://luckperms.net/) |
| PlaceholderAPI | 占位符系统 | [GitHub](https://github.com/PlaceholderAPI/PlaceholderAPI) |
| Vault | 经济 API | [GitHub](https://github.com/MilkBowl/Vault) |
| ProtocolLib | 协议库 | [GitHub](https://github.com/dmulloy2/ProtocolLib) |

---

## 📞 联系方式

### 问题反馈

- 🐛 **Bug 报告**: [GitHub Issues](https://github.com/meisyangb/astraea-rpg-server/issues)
- 💡 **功能建议**: [GitHub Discussions](https://github.com/meisyangb/astraea-rpg-server/discussions)

### 社区

- 📖 **Wiki**: [GitHub Wiki](https://github.com/meisyangb/astraea-rpg-server/wiki)
- 💬 **Discord**: 即将开放

---

## 📊 项目统计

| 指标 | 数值 |
|------|------|
| 插件总数 | 26 个 |
| 核心插件 | 1 个 (RPGCore) |
| 功能插件 | 25 个 (GuangDian*) |
| Java 版本 | 21 |
| Minecraft 版本 | 1.21.6 |
| 代码行数 | 50,000+ |
| 开发周期 | 持续更新 |

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个 Star！⭐**

**Astraea RPG - 星辰女神的祝福**

*Made with ❤️ by Astraea RPG Team*

</div>
