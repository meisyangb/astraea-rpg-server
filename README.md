# Astraea RPG 阿斯特瑞亚

> Minecraft Paper 1.21.6 RPG服务器插件体系
> 
> 基于 RPGCore 架构的高性能插件集合

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.6-blue.svg)](https://www.minecraft.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21.6-orange.svg)](https://papermc.io/)
[![JDK](https://img.shields.io/badge/JDK-21-red.svg)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Gradle-9.4.0-green.svg)](https://gradle.org/)

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
│  ├── ArmorStats (装备属性)                                  │
│  ├── Points (点数系统)                                      │
│  ├── Name (命名系统)                                        │
│  ├── Market (市场系统)                                      │
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

- **JDK**: 21 (推荐 Eclipse Temurin)
- **Gradle**: 9.4.0
- **Minecraft Server**: Paper 1.21.6

### 构建项目

```powershell
# 设置环境变量
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"

# 全量构建
gradle build --no-configuration-cache -x test

# 单插件构建
gradle :plugins:RPGCore:build --no-configuration-cache -x test

# 部署到服务器
gradle deployAll --no-configuration-cache -x test
```

### 运行服务器

```powershell
cd server
java -Xms4G -Xmx8G -jar paper-1.21.6.jar nogui
```

---

## 📁 项目结构

```
astraea-rpg-server/
├── .trae/                    # AI助手配置
│   ├── rules/                # 开发规则
│   ├── skills/               # 技能定义
│   └── knowledge/            # 知识库
├── plugins/                  # 插件源码
│   ├── RPGCore/              # 核心插件
│   ├── GuangDianName/        # 命名插件
│   └── ...                   # 其他插件
├── server/                   # 服务端文件
│   └── plugins/              # 编译后的插件
├── tools/                    # 工具链
│   └── jdk-21.0.10+7/        # JDK
├── docs/                     # 文档
├── README.md                 # 本文件
├── PROJECT_NAMING.md         # 命名规范
└── build.gradle              # 构建配置
```

---

## 🛠️ 开发规范

### 插件开发必须遵守

1. **继承 AbstractRPGPlugin** - 禁止直接继承 JavaPlugin
2. **使用 SyncScheduler** - 禁止 Bukkit.getScheduler()
3. **服务注册** - 通过 ServiceRegistry 注册服务
4. **外部服务** - 通过 ExternalServiceIntegration 访问

### 代码审查清单

- [ ] 无 `new BukkitRunnable()` 调用
- [ ] 无 `Bukkit.getPlugin("RPGCore")` 调用
- [ ] 无 `ChatColor` 使用 (改用 Adventure API)
- [ ] 无 `ArmorStand` 用于显示 (改用 TextDisplay)
- [ ] 插件卸载时取消所有调度任务

详细规范见 [`.trae/rules/kaifa.md`](.trae/rules/kaifa.md)

---

## 📚 文档

- [开发规则](.trae/rules/kaifa.md) - 完整的开发规范
- [项目命名](PROJECT_NAMING.md) - 命名规范和Git流程
- [技能文档](.trae/skills/) - AI助手技能定义
- [知识库](.trae/knowledge/) - 问题解决方案

---

## 🤝 贡献指南

### 提交规范

```
[类型]: [简要描述]

[详细描述]

[关联Issue]
```

类型: `feat` | `fix` | `docs` | `style` | `refactor` | `perf` | `test` | `chore`

### 分支策略

- `main` - 稳定版本
- `develop` - 开发分支
- `feature/*` - 功能分支
- `fix/*` - 修复分支

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源许可证。

---

## 🙏 致谢

- [PaperMC](https://papermc.io/) - 高性能 Minecraft 服务端
- [MythicMobs](https://mythiccraft.io/) - 强大的怪物系统
- [LuckPerms](https://luckperms.net/) - 权限管理
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) - 占位符系统

---

<p align="center">
  <strong>Astraea RPG - 星辰女神的祝福</strong>
</p>

